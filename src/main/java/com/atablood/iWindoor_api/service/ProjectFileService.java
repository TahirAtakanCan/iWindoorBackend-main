package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.design.DesignDocuments;
import com.atablood.iWindoor_api.design.DesignDocumentValidator;
import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto;
import com.atablood.iWindoor_api.dto.project.ProjectFileDto;
import com.atablood.iWindoor_api.dto.project.ProjectFileDto.ExtraPart;
import com.atablood.iWindoor_api.dto.project.ProjectFileDto.ProjectPart;
import com.atablood.iWindoor_api.dto.project.ProjectFileDto.WindowPart;
import com.atablood.iWindoor_api.dto.project.ProjectRequest;
import com.atablood.iWindoor_api.dto.window.WindowCreateRequest;
import com.atablood.iWindoor_api.dto.window.WindowDto;
import com.atablood.iWindoor_api.entity.Project;
import com.atablood.iWindoor_api.entity.Series;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.entity.WindowUnit;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.pricing.BomPricer;
import com.atablood.iWindoor_api.repository.ProjectExtraItemRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectFileService {

    public static final String FORMAT = "iwindoor-project";
    public static final int VERSION = 1;
    public static final int MAX_WINDOWS = 200;

    private final ProjectRepository projectRepository;
    private final SeriesRepository seriesRepository;
    private final ProjectExtraItemRepository extraItemRepository;
    private final ProjectService projectService;
    private final WindowService windowService;
    private final ProjectExtraItemService extraItemService;
    private final CatalogService catalogService;
    private final CatalogProvisioningService provisioning;
    private final PricingService pricingService;
    private final DesignDocumentValidator validator;
    private final BomPricer bomPricer;
    private final AccessGuard guard;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public ResponseEntity<ProjectFileDto> export(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);

        List<WindowPart> windows = new ArrayList<>();
        for (WindowUnit unit : project.getWindowUnits()) {
            boolean legacy = unit.getDesign() == null;
            Map<String, Object> design = legacy ? DesignDocuments.fromLegacy(unit) : unit.getDesign();
            String seriesName = unit.getSeries() == null ? null : unit.getSeries().getName();
            windows.add(new WindowPart(
                    unit.getName(),
                    unit.getQuantity(),
                    unit.getWidth(),
                    unit.getHeight(),
                    seriesName,
                    design,
                    legacy ? null : unit.getBomSnapshot(),
                    unit.getSalePriceOverride()
            ));
        }

        List<ExtraPart> extras = extraItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(projectId).stream()
                .map(e -> new ExtraPart(e.getName(), e.getQuantity(), e.getUnit(), e.getUnitPrice(), e.getSortOrder()))
                .toList();

        ProjectFileDto body = new ProjectFileDto(
                FORMAT,
                VERSION,
                Instant.now(),
                new ProjectPart(
                        project.getName(),
                        project.getDescription(),
                        project.getCustomerName(),
                        project.getCustomerPhone(),
                        project.getCustomerEmail(),
                        project.getCustomerAddress()),
                windows,
                extras
        );

        String filename = asciiSlug(project.getName()) + ".iwd";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    @Transactional
    public Project importFile(ProjectFileDto file, User user) {
        if (file == null
                || !FORMAT.equals(file.format())
                || file.version() == null
                || file.version() != VERSION
                || file.windows() == null
                || file.windows().isEmpty()) {
            throw new BadRequestException("INVALID_PROJECT_FILE", "Proje dosyası geçersiz veya sürüm uyumsuz.");
        }
        if (file.windows().size() > MAX_WINDOWS) {
            throw new BadRequestException("INVALID_PROJECT_FILE", "En fazla " + MAX_WINDOWS + " pencere içe aktarılabilir.");
        }
        if (file.project() == null || file.project().name() == null || file.project().name().isBlank()) {
            throw new BadRequestException("INVALID_PROJECT_FILE", "Proje adı gerekli.");
        }

        Long companyId = guard.companyId(user);
        provisioning.ensureCompanyCatalog(companyId);

        String baseName = file.project().name().trim();
        String suffix = " (içe aktarıldı)";
        String importName = baseName.length() + suffix.length() > 200
                ? baseName.substring(0, Math.max(0, 200 - suffix.length())) + suffix
                : baseName + suffix;
        ProjectPart src = file.project();
        Project project = projectService.createProject(
                new ProjectRequest(
                        importName,
                        src.description(),
                        src.customerName(),
                        src.customerPhone(),
                        src.customerEmail(),
                        src.customerAddress()),
                user);

        Series defaultSeries = catalogService.defaultSeries(companyId);

        for (WindowPart w : file.windows()) {
            if (w == null || w.name() == null || w.width() == null || w.height() == null) {
                throw new BadRequestException("INVALID_PROJECT_FILE", "Pencere adı ve ölçüleri gerekli.");
            }
            if (w.design() == null) {
                throw new BadRequestException("INVALID_PROJECT_FILE", "Her pencerede tasarım belgesi gerekli.");
            }

            Series series = resolveSeries(companyId, w.seriesName(), defaultSeries);
            Map<String, Object> design = deepCopy(w.design());
            design.put("series", series == null ? null : String.valueOf(series.getId()));
            validator.validateOrThrow(design);

            WindowDto created = windowService.create(project.getId(),
                    new WindowCreateRequest(w.name(), w.width(), w.height(), w.quantity(),
                            series == null ? null : series.getId(), design),
                    user);

            if (w.bom() != null) {
                bomPricer.validate(w.bom());
                windowService.applyBomAndPrice(created.id(), w.bom(), user);
            }
            if (w.salePriceOverride() != null) {
                windowService.applySalePriceOverride(created.id(), w.salePriceOverride(), user);
            }
        }

        if (file.extras() != null) {
            for (ExtraPart e : file.extras()) {
                if (e == null || e.name() == null || e.name().isBlank() || e.unitPrice() == null) continue;
                BigDecimal qty = e.quantity() == null ? BigDecimal.ONE : e.quantity();
                String unit = e.unit() == null || e.unit().isBlank() ? "adet" : e.unit();
                extraItemService.create(project.getId(),
                        new ProjectExtraItemDto.Request(e.name(), qty, unit, e.unitPrice(),
                                e.sortOrder() == null ? 0 : e.sortOrder()),
                        user);
            }
        }

        pricingService.calculateProjectPrice(project.getId());
        return projectService.getProject(project.getId(), user);
    }

    private Series resolveSeries(Long companyId, String seriesName, Series fallback) {
        if (seriesName != null && !seriesName.isBlank()) {
            return seriesRepository.findFirstByCompanyIdAndName(companyId, seriesName.trim())
                    .orElse(fallback);
        }
        return fallback;
    }

    private Map<String, Object> deepCopy(Map<String, Object> src) {
        return objectMapper.convertValue(src, new TypeReference<>() {
        });
    }

    static String asciiSlug(String name) {
        if (name == null || name.isBlank()) return "project";
        String transliterated = transliterateTurkish(name).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (char c : transliterated.toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c == ' ' || c == '-' || c == '_') {
                if (sb.isEmpty() || sb.charAt(sb.length() - 1) != '-') sb.append('-');
            }
        }
        while (!sb.isEmpty() && sb.charAt(0) == '-') sb.deleteCharAt(0);
        while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '-') sb.deleteCharAt(sb.length() - 1);
        return sb.isEmpty() ? "project" : sb.toString();
    }

    private static String transliterateTurkish(String input) {
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            out.append(switch (c) {
                case 'ç', 'Ç' -> 'c';
                case 'ğ', 'Ğ' -> 'g';
                case 'ı', 'İ' -> 'i';
                case 'ö', 'Ö' -> 'o';
                case 'ş', 'Ş' -> 's';
                case 'ü', 'Ü' -> 'u';
                default -> c;
            });
        }
        return out.toString();
    }
}
