package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.project.StatusChangeRequest;
import com.atablood.iWindoor_api.dto.project.StatusHistoryDto;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.ProjectStatusHistoryRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectStatusService {

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED = Map.of(
            ProjectStatus.QUOTE, EnumSet.of(ProjectStatus.APPROVED, ProjectStatus.CANCELLED),
            ProjectStatus.APPROVED, EnumSet.of(ProjectStatus.PRODUCTION, ProjectStatus.QUOTE, ProjectStatus.CANCELLED),
            ProjectStatus.PRODUCTION, EnumSet.of(ProjectStatus.INSTALLATION, ProjectStatus.APPROVED),
            ProjectStatus.INSTALLATION, EnumSet.of(ProjectStatus.DELIVERED, ProjectStatus.PRODUCTION),
            ProjectStatus.CANCELLED, EnumSet.of(ProjectStatus.QUOTE),
            ProjectStatus.DELIVERED, EnumSet.noneOf(ProjectStatus.class)
    );

    private static final Set<String> ADMIN_ONLY = Set.of(
            "APPROVED->QUOTE",
            "PRODUCTION->APPROVED",
            "INSTALLATION->PRODUCTION",
            "QUOTE->CANCELLED",
            "APPROVED->CANCELLED",
            "PRODUCTION->CANCELLED",
            "INSTALLATION->CANCELLED",
            "DELIVERED->CANCELLED",
            "CANCELLED->QUOTE"
    );

    private final ProjectRepository projectRepository;
    private final ProjectStatusHistoryRepository historyRepository;
    private final ProjectQuoteService projectQuoteService;
    private final ProjectFinanceService financeService;
    private final AccessGuard guard;

    @Transactional
    public Project changeStatus(Long projectId, StatusChangeRequest req, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);

        ProjectStatus from = project.getStatus() == null ? ProjectStatus.QUOTE : project.getStatus();
        ProjectStatus to = req.status();
        if (from == to) {
            throw new ConflictException("INVALID_TRANSITION", "Proje zaten bu durumda: " + to);
        }
        Set<ProjectStatus> allowed = ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ConflictException("INVALID_TRANSITION",
                    "Geçersiz durum geçişi: " + from + " → " + to);
        }
        String key = from.name() + "->" + to.name();
        if (ADMIN_ONLY.contains(key) || to == ProjectStatus.CANCELLED) {
            requireAdmin(user);
        }

        if (to == ProjectStatus.APPROVED) {
            project.setApprovedAt(Instant.now());
            project.setApprovedTotal(projectQuoteService.quote(projectId, null, user).grandTotal());
        }
        if (to == ProjectStatus.QUOTE && from != ProjectStatus.CANCELLED) {
            // QUOTE'a dönüş (APPROVED→QUOTE): onay bilgilerini temizle
            project.setApprovedAt(null);
            project.setApprovedTotal(null);
        }
        // CANCELLED→QUOTE: approved fields already null typically; clear anyway
        if (to == ProjectStatus.QUOTE) {
            project.setApprovedAt(null);
            project.setApprovedTotal(null);
        }

        if (req.deliveryDate() != null) {
            project.setDeliveryDate(req.deliveryDate());
        }

        project.setStatus(to);
        project.setStatusChangedAt(Instant.now());

        ProjectStatusHistory hist = new ProjectStatusHistory();
        hist.setProject(project);
        hist.setFromStatus(from);
        hist.setToStatus(to);
        hist.setNote(req.note());
        hist.setChangedBy(user);
        historyRepository.save(hist);

        Project saved = projectRepository.save(project);
        financeService.enrich(saved, user);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryDto> history(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        return historyRepository.findByProject_IdOrderByChangedAtAscIdAsc(projectId).stream()
                .map(StatusHistoryDto::of)
                .toList();
    }

    @Transactional
    public Project setDeliveryDate(Long projectId, java.time.LocalDate deliveryDate, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        project.setDeliveryDate(deliveryDate);
        Project saved = projectRepository.save(project);
        financeService.enrich(saved, user);
        return saved;
    }

    private static void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.COMPANY_ADMIN) {
            throw new ForbiddenException("Bu durum geçişi yalnızca yönetici tarafından yapılabilir.");
        }
    }
}
