package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.design.DesignDocuments;
import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto;
import com.atablood.iWindoor_api.dto.quote.QuoteVersionDto;
import com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.CreateRequest;
import com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.DetailDto;
import com.atablood.iWindoor_api.dto.quote.QuoteVersionDto.RestoreResponse;
import com.atablood.iWindoor_api.dto.window.WindowCreateRequest;
import com.atablood.iWindoor_api.dto.window.WindowDto;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.ForbiddenException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.ProjectExtraItemRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.QuoteVersionRepository;
import com.atablood.iWindoor_api.repository.SeriesRepository;
import com.atablood.iWindoor_api.repository.WindowUnitRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuoteVersionService {

    public static final int MAX_VERSIONS = 50;
    public static final String LABEL_APPROVED = "Onaylanan teklif";
    public static final String LABEL_BEFORE_RESTORE = "Geri yüklemeden önce (otomatik)";

    private final ProjectRepository projectRepository;
    private final QuoteVersionRepository versionRepository;
    private final ProjectExtraItemRepository extraItemRepository;
    private final WindowUnitRepository windowUnitRepository;
    private final SeriesRepository seriesRepository;
    private final ProjectQuoteService projectQuoteService;
    private final ProjectExtraItemService extraItemService;
    private final WindowService windowService;
    private final PricingService pricingService;
    private final ProjectFinanceService financeService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public List<QuoteVersionDto> list(Long projectId, User user) {
        Project project = loadProject(projectId, user);
        return versionRepository.findByProject_IdOrderByVersionNoAsc(projectId).stream()
                .map(v -> QuoteVersionDto.of(v, project))
                .toList();
    }

    @Transactional(readOnly = true)
    public DetailDto get(Long projectId, Long versionId, User user) {
        Project project = loadProject(projectId, user);
        QuoteVersion v = versionRepository.findByIdAndProject_Id(versionId, projectId)
                .orElseThrow(() -> new NotFoundException("Sürüm", versionId));
        return DetailDto.of(v, project);
    }

    @Transactional
    public QuoteVersionDto create(Long projectId, CreateRequest req, User user) {
        Project project = loadProject(projectId, user);
        return QuoteVersionDto.of(saveSnapshot(project, req == null ? null : req.label(), user), project);
    }

    /** APPROVED geçişinde otomatik sürüm; kaydedilen entity döner. */
    @Transactional
    public QuoteVersion createApprovedVersion(Project project, User user) {
        return saveSnapshot(project, LABEL_APPROVED, user);
    }

    @Transactional
    public RestoreResponse restore(Long projectId, Long versionId, User user) {
        Project project = loadProject(projectId, user);
        if (project.getStatus() != null && project.getStatus() != ProjectStatus.QUOTE) {
            throw new ConflictException("PROJECT_LOCKED",
                    "Yalnızca teklif durumundaki projelerde geri yükleme yapılabilir.");
        }
        QuoteVersion target = versionRepository.findByIdAndProject_Id(versionId, projectId)
                .orElseThrow(() -> new NotFoundException("Sürüm", versionId));

        saveSnapshot(project, LABEL_BEFORE_RESTORE, user);

        // Mevcut pencereleri ve ek kalemleri sil
        List<WindowUnit> windows = new ArrayList<>(project.getWindowUnits());
        for (WindowUnit u : windows) {
            project.getWindowUnits().remove(u);
            windowUnitRepository.delete(u);
        }
        windowUnitRepository.flush();
        extraItemRepository.deleteByProject_Id(projectId);
        extraItemRepository.flush();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snapWindows = (List<Map<String, Object>>) target.getSnapshot().get("windows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snapExtras = (List<Map<String, Object>>) target.getSnapshot().get("extras");

        List<String> warnings = new ArrayList<>();
        if (snapWindows != null) {
            List<Map<String, Object>> ordered = new ArrayList<>(snapWindows);
            ordered.sort(Comparator.comparingInt(m -> intVal(m.get("sortOrder"), 0)));
            for (Map<String, Object> w : ordered) {
                Long seriesId = longVal(w.get("seriesId"));
                if (seriesId != null && seriesRepository.findById(seriesId).isEmpty()) {
                    warnings.add("Seri bulunamadı, pencere atlandı: " + str(w.get("name")));
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> design = (Map<String, Object>) w.get("design");
                if (design == null) {
                    warnings.add("Tasarım yok, pencere atlandı: " + str(w.get("name")));
                    continue;
                }
                double width = dbl(w.get("width"), 0);
                double height = dbl(w.get("height"), 0);
                Integer qty = w.get("quantity") instanceof Number n ? n.intValue() : 1;
                WindowDto created = windowService.create(projectId,
                        new WindowCreateRequest(str(w.get("name")), width, height, qty, seriesId, design), user);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bom = (List<Map<String, Object>>) w.get("bomSnapshot");
                if (bom != null) {
                    windowService.applyBomAndPrice(created.id(), bom, user);
                }
                if (w.get("salePriceOverride") != null) {
                    windowService.applySalePriceOverride(created.id(),
                            new BigDecimal(w.get("salePriceOverride").toString()), user);
                }
            }
        }
        if (snapExtras != null) {
            for (Map<String, Object> e : snapExtras) {
                if (e.get("name") == null || e.get("unitPrice") == null) continue;
                BigDecimal qty = e.get("quantity") == null ? BigDecimal.ONE : new BigDecimal(e.get("quantity").toString());
                String unit = e.get("unit") == null ? "adet" : e.get("unit").toString();
                Integer sort = e.get("sortOrder") instanceof Number n ? n.intValue() : 0;
                extraItemService.create(projectId, new ProjectExtraItemDto.Request(
                        e.get("name").toString(), qty, unit,
                        new BigDecimal(e.get("unitPrice").toString()), sort), user);
            }
        }

        pricingService.syncPrices(projectId);
        Project refreshed = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        financeService.enrich(refreshed, user);
        return new RestoreResponse(refreshed, warnings);
    }

    @Transactional
    public void delete(Long projectId, Long versionId, User user) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.COMPANY_ADMIN) {
            throw new ForbiddenException("Sürüm silme yalnızca yönetici tarafından yapılabilir.");
        }
        Project project = loadProject(projectId, user);
        QuoteVersion v = versionRepository.findByIdAndProject_Id(versionId, projectId)
                .orElseThrow(() -> new NotFoundException("Sürüm", versionId));
        if (project.getApprovedVersion() != null && project.getApprovedVersion().getId().equals(versionId)) {
            throw new ConflictException("VERSION_APPROVED", "Onaylanan sürüm silinemez.");
        }
        versionRepository.delete(v);
    }

    private QuoteVersion saveSnapshot(Project project, String label, User user) {
        if (versionRepository.countByProject_Id(project.getId()) >= MAX_VERSIONS) {
            throw new BadRequestException("VERSION_LIMIT", "Proje başına en fazla " + MAX_VERSIONS + " sürüm kaydedilebilir.");
        }
        ProjectQuoteDto quote = projectQuoteService.quote(project.getId(), null, user);
        Map<String, Object> snapshot = buildSnapshot(project, quote);

        QuoteVersion v = new QuoteVersion();
        v.setProject(project);
        v.setVersionNo(versionRepository.maxVersionNo(project.getId()) + 1);
        v.setLabel(label);
        v.setSnapshot(snapshot);
        v.setGrandTotal(quote.grandTotal());
        v.setSaleSubtotal(quote.subtotal());
        v.setWindowCount(project.getWindowUnits() == null ? 0 : project.getWindowUnits().size());
        v.setCreatedBy(user);
        return versionRepository.save(v);
    }

    private Map<String, Object> buildSnapshot(Project project, ProjectQuoteDto quote) {
        List<Map<String, Object>> windows = new ArrayList<>();
        Map<String, String> seriesNames = new LinkedHashMap<>();
        int sort = 0;
        for (WindowUnit unit : project.getWindowUnits()) {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("name", unit.getName());
            w.put("width", unit.getWidth());
            w.put("height", unit.getHeight());
            w.put("quantity", unit.getQuantity());
            Long seriesId = unit.getSeriesId();
            w.put("seriesId", seriesId);
            if (seriesId != null && unit.getSeries() != null) {
                seriesNames.put(String.valueOf(seriesId), unit.getSeries().getName());
            }
            Map<String, Object> design = unit.getDesign() == null ? DesignDocuments.fromLegacy(unit) : unit.getDesign();
            w.put("design", design);
            w.put("bomSnapshot", unit.getBomSnapshot());
            w.put("unitPrice", unit.getUnitPrice());
            w.put("saleUnitPrice", unit.getSaleUnitPrice());
            w.put("salePriceOverride", unit.getSalePriceOverride());
            w.put("sortOrder", sort++);
            windows.add(w);
        }

        List<Map<String, Object>> extras = new ArrayList<>();
        for (ProjectExtraItem e : extraItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(project.getId())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getName());
            m.put("quantity", e.getQuantity());
            m.put("unit", e.getUnit());
            m.put("unitPrice", e.getUnitPrice());
            m.put("sortOrder", e.getSortOrder());
            extras.add(m);
        }

        Map<String, Object> quoteMap = new LinkedHashMap<>();
        quoteMap.put("subtotal", quote.subtotal());
        quoteMap.put("discountPct", quote.discountPct());
        quoteMap.put("discount", quote.discountAmount());
        quoteMap.put("net", quote.netTotal());
        quoteMap.put("vatRate", quote.vatRate());
        quoteMap.put("vat", quote.vatAmount());
        quoteMap.put("grandTotal", quote.grandTotal());

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("name", project.getCustomerName());
        customer.put("phone", project.getCustomerPhone());
        customer.put("email", project.getCustomerEmail());
        customer.put("address", project.getCustomerAddress());

        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("windows", windows);
        snap.put("extras", extras);
        snap.put("quote", quoteMap);
        snap.put("customer", customer);
        snap.put("seriesNames", seriesNames);
        return snap;
    }

    private Project loadProject(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        return project;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long longVal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double dbl(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        return def;
    }

    private static int intVal(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        return def;
    }
}
