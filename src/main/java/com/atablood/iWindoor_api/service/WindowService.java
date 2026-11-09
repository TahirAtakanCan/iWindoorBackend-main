package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.design.DesignDocumentValidator;
import com.atablood.iWindoor_api.design.DesignDocuments;
import com.atablood.iWindoor_api.dto.window.*;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.ConflictException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.pricing.BomPricer;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.repository.WindowUnitRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WindowService {

    private final ProjectRepository projectRepository;
    private final WindowUnitRepository windowUnitRepository;
    private final DesignDocumentValidator validator;
    private final BomPricer bomPricer;
    private final CatalogService catalogService;
    private final CatalogProvisioningService provisioning;
    private final PricingService pricingService;
    private final SalesPricingService salesPricingService;
    private final AccessGuard guard;

    @Transactional
    public WindowDto create(Long projectId, WindowCreateRequest req, User user) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);
        Long companyId = guard.companyId(user);
        provisioning.ensureCompanyCatalog(companyId);

        WindowUnit unit = new WindowUnit();
        unit.setName(req.name());
        unit.setWidth(req.width());
        unit.setHeight(req.height());
        unit.setQuantity(req.quantity() == null ? 1 : req.quantity());
        unit.setSeries(req.seriesId() != null ? catalogService.requireSeries(req.seriesId(), user)
                : catalogService.defaultSeries(companyId));
        unit.setProject(project);

        // Eski uygulama için kök düğüm (düğüm tabanlı editör bunu bekliyor)
        WindowNode root = new WindowNode();
        root.setNodeType(NodeType.FRAME);
        root.setWidth(req.width());
        root.setHeight(req.height());
        root.setItemOrder(0);
        unit.setRootNode(root);

        if (req.design() != null) {
            validator.validateOrThrow(req.design());
            requireSameSize(req.design(), req.width(), req.height());
            unit.setDesign(req.design());
        }
        project.getWindowUnits().add(unit);
        WindowUnit saved = windowUnitRepository.saveAndFlush(unit);
        return WindowDto.of(saved);
    }

    @Transactional(readOnly = true)
    public WindowDto get(Long id, User user) {
        return WindowDto.of(load(id, user));
    }

    @Transactional
    public WindowDto update(Long id, WindowUpdateRequest req, User user) {
        WindowUnit unit = load(id, user);
        if (!Objects.equals(req.rowVersion(), unit.getRowVersion())) {
            throw new ConflictException("STALE_VERSION",
                    "Pencere başka bir cihazda değiştirildi. Güncel hâli yükleyip tekrar deneyin.",
                    Map.of("currentRowVersion", unit.getRowVersion()));
        }
        validator.validateOrThrow(req.design());
        bomPricer.validate(req.bom());

        if (req.seriesId() != null) unit.setSeries(catalogService.requireSeries(req.seriesId(), user));
        unit.setName(req.name());
        if (req.quantity() != null) unit.setQuantity(req.quantity());
        unit.setDesign(req.design());
        unit.setWidth(((Number) req.design().get("width")).doubleValue());
        unit.setHeight(((Number) req.design().get("height")).doubleValue());
        unit.setBomSnapshot(req.bom());
        Long companyId = guard.companyId(user);
        unit.setUnitPrice(req.bom() == null ? null
                : bomPricer.price(companyId, unit.getSeries(), req.bom()).total());
        salesPricingService.applyWindow(unit, companyId);

        WindowUnit saved = windowUnitRepository.saveAndFlush(unit);
        pricingService.calculateProjectPrice(saved.getProject().getId());
        return WindowDto.of(windowUnitRepository.findById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(Long id, User user) {
        WindowUnit unit = load(id, user);
        Project project = unit.getProject();
        project.getWindowUnits().remove(unit);
        windowUnitRepository.delete(unit);
        windowUnitRepository.flush();
        pricingService.calculateProjectPrice(project.getId());
    }

    /** Satış fiyatı geçersiz kılma; rowVersion gerekmez. */
    @Transactional
    public WindowDto updateSalePriceOverride(Long id, BigDecimal override, User user) {
        WindowUnit unit = load(id, user);
        unit.setSalePriceOverride(override == null ? null : override.setScale(2, RoundingMode.HALF_UP));
        windowUnitRepository.saveAndFlush(unit);
        salesPricingService.recalculateProjectSales(unit.getProject().getId());
        return WindowDto.of(windowUnitRepository.findById(id).orElse(unit));
    }

    /** İçe aktarmada: kesim listesini doğrula, kaydet ve güncel katalogdan unitPrice hesapla. */
    @Transactional
    public void applyBomAndPrice(Long windowId, List<Map<String, Object>> bom, User user) {
        WindowUnit unit = load(windowId, user);
        bomPricer.validate(bom);
        unit.setBomSnapshot(bom);
        Long companyId = guard.companyId(user);
        unit.setUnitPrice(bom == null ? null
                : bomPricer.price(companyId, unit.getSeries(), bom).total());
        salesPricingService.applyWindow(unit, companyId);
        windowUnitRepository.saveAndFlush(unit);
    }

    /** İçe aktarmada satış override uygula (sonra proje satış toplamı yeniden hesaplanır). */
    @Transactional
    public void applySalePriceOverride(Long windowId, BigDecimal override, User user) {
        WindowUnit unit = load(windowId, user);
        unit.setSalePriceOverride(override == null ? null : override.setScale(2, RoundingMode.HALF_UP));
        windowUnitRepository.saveAndFlush(unit);
    }

    /** Kayıtlı kesim listesini GÜNCEL katalog fiyatlarıyla fiyatlar (kayıttaki unitPrice değişmez). */
    @Transactional(readOnly = true)
    public QuoteDto quote(Long id, User user) {
        WindowUnit unit = load(id, user);
        int qty = unit.getQuantity() == null ? 1 : unit.getQuantity();
        if (unit.getBomSnapshot() == null) {
            return new QuoteDto(unit.getId(), List.of(), BigDecimal.ZERO.setScale(2), qty, BigDecimal.ZERO.setScale(2),
                    "TRY", List.of("Bu pencere için henüz kesim listesi gönderilmedi."));
        }
        PricedBom p = bomPricer.price(guard.companyId(user), unit.getSeries(), unit.getBomSnapshot());
        return new QuoteDto(unit.getId(), p.lines(), p.total(), qty,
                p.total().multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP), p.currency(), p.warnings());
    }

    private WindowUnit load(Long id, User user) {
        WindowUnit unit = windowUnitRepository.findById(id).orElseThrow(() -> new NotFoundException("Pencere", id));
        guard.checkProject(unit.getProject(), user);
        return unit;
    }

    private static void requireSameSize(Map<String, Object> design, double w, double h) {
        double dw = ((Number) design.get("width")).doubleValue();
        double dh = ((Number) design.get("height")).doubleValue();
        if (Math.abs(dw - w) > 0.5 || Math.abs(dh - h) > 0.5) {
            throw new BadRequestException("SIZE_MISMATCH", "Belgedeki ölçü ile pencere ölçüsü aynı olmalı.");
        }
    }
}
