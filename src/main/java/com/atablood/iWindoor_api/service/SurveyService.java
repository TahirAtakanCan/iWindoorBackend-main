package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.survey.SurveyItemDto;
import com.atablood.iWindoor_api.dto.survey.SurveyItemDto.LinkRequest;
import com.atablood.iWindoor_api.dto.survey.SurveyItemDto.Request;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.SurveyItemRepository;
import com.atablood.iWindoor_api.repository.SurveyPhotoRepository;
import com.atablood.iWindoor_api.repository.WindowUnitRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final int MAX_PHOTOS = 5;

    private final ProjectRepository projectRepository;
    private final SurveyItemRepository surveyItemRepository;
    private final SurveyPhotoRepository surveyPhotoRepository;
    private final WindowUnitRepository windowUnitRepository;
    private final CompanyRepository companyRepository;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public List<SurveyItemDto> list(Long projectId, User user) {
        Project project = loadProject(projectId, user);
        BigDecimal gap = surveyGap(project);
        return surveyItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .peek(i -> i.getPhotos().size())
                .map(i -> toDto(i, gap))
                .toList();
    }

    @Transactional
    public SurveyItemDto create(Long projectId, Request req, User user) {
        Project project = loadProject(projectId, user);
        SurveyItem item = new SurveyItem();
        item.setProject(project);
        apply(item, req);
        SurveyItem saved = surveyItemRepository.save(item);
        return toDto(saved, surveyGap(project));
    }

    @Transactional
    public SurveyItemDto update(Long projectId, Long itemId, Request req, User user) {
        Project project = loadProject(projectId, user);
        SurveyItem item = surveyItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Keşif kalemi", itemId));
        apply(item, req);
        return toDto(surveyItemRepository.save(item), surveyGap(project));
    }

    @Transactional
    public void delete(Long projectId, Long itemId, User user) {
        loadProject(projectId, user);
        SurveyItem item = surveyItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Keşif kalemi", itemId));
        surveyItemRepository.delete(item);
    }

    @Transactional
    public SurveyItemDto link(Long projectId, Long itemId, LinkRequest req, User user) {
        Project project = loadProject(projectId, user);
        SurveyItem item = surveyItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Keşif kalemi", itemId));
        if (req == null || req.windowUnitId() == null) {
            item.setWindowUnit(null);
        } else {
            WindowUnit unit = windowUnitRepository.findById(req.windowUnitId())
                    .orElseThrow(() -> new NotFoundException("Pencere", req.windowUnitId()));
            if (unit.getProject() == null || !projectId.equals(unit.getProject().getId())) {
                throw new BadRequestException("VALIDATION_FAILED", "Pencere bu projeye ait değil.");
            }
            item.setWindowUnit(unit);
        }
        return toDto(surveyItemRepository.save(item), surveyGap(project));
    }

    @Transactional
    public Map<String, Long> uploadPhoto(Long projectId, Long itemId, MultipartFile file, User user) {
        loadProject(projectId, user);
        SurveyItem item = surveyItemRepository.findByIdAndProject_Id(itemId, projectId)
                .orElseThrow(() -> new NotFoundException("Keşif kalemi", itemId));
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("VALIDATION_FAILED", "Dosya gerekli.");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_TYPES.contains(ct)) {
            throw new BadRequestException("VALIDATION_FAILED", "Yalnızca image/jpeg, image/png veya image/webp.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("VALIDATION_FAILED", "Dosya en fazla 3 MB olabilir.");
        }
        if (surveyPhotoRepository.countBySurveyItem_Id(itemId) >= MAX_PHOTOS) {
            throw new BadRequestException("VALIDATION_FAILED", "Kalem başına en fazla " + MAX_PHOTOS + " fotoğraf.");
        }
        try {
            SurveyPhoto photo = new SurveyPhoto();
            photo.setSurveyItem(item);
            photo.setContentType(ct);
            photo.setData(file.getBytes());
            photo.setSizeBytes((int) file.getSize());
            SurveyPhoto saved = surveyPhotoRepository.save(photo);
            return Map.of("id", saved.getId());
        } catch (IOException e) {
            throw new BadRequestException("VALIDATION_FAILED", "Dosya okunamadı.");
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getPhoto(Long photoId, User user) {
        SurveyPhoto photo = surveyPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Fotoğraf", photoId));
        guard.checkProject(photo.getSurveyItem().getProject(), user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, photo.getContentType())
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(photo.getData());
    }

    @Transactional
    public void deletePhoto(Long photoId, User user) {
        SurveyPhoto photo = surveyPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Fotoğraf", photoId));
        guard.checkProject(photo.getSurveyItem().getProject(), user);
        surveyPhotoRepository.delete(photo);
    }

    private SurveyItemDto toDto(SurveyItem item, BigDecimal gap) {
        BigDecimal suggestedW = item.getHoleWidth().subtract(gap.multiply(BigDecimal.valueOf(2)))
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal suggestedH = item.getHoleHeight().subtract(gap.multiply(BigDecimal.valueOf(2)))
                .setScale(1, RoundingMode.HALF_UP);
        List<Long> photoIds = item.getPhotos() == null ? List.of()
                : item.getPhotos().stream().map(SurveyPhoto::getId).sorted().toList();
        Long windowId = item.getWindowUnit() == null ? null : item.getWindowUnit().getId();
        return new SurveyItemDto(item.getId(), item.getRoom(), item.getProductType(),
                item.getHoleWidth(), item.getHoleHeight(), item.getQuantity(), item.getNote(),
                item.getSortOrder(), windowId, photoIds, suggestedW, suggestedH, item.getCreatedAt());
    }

    private static void apply(SurveyItem item, Request req) {
        item.setRoom(req.room().trim());
        item.setProductType(req.productType());
        item.setHoleWidth(req.holeWidth().setScale(1, RoundingMode.HALF_UP));
        item.setHoleHeight(req.holeHeight().setScale(1, RoundingMode.HALF_UP));
        item.setQuantity(req.quantity());
        item.setNote(req.note());
        item.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }

    private Project loadProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        return project;
    }

    private BigDecimal surveyGap(Project project) {
        Long companyId = project.getCreatedBy() == null || project.getCreatedBy().getCompany() == null
                ? null : project.getCreatedBy().getCompany().getId();
        if (companyId == null) return new BigDecimal("10.0");
        return companyRepository.findById(companyId)
                .map(c -> c.getSurveyGapMm() == null ? new BigDecimal("10.0") : c.getSurveyGapMm())
                .orElse(new BigDecimal("10.0"));
    }
}
