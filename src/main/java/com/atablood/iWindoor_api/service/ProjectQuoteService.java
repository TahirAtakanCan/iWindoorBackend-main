package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.design.DesignDocuments;
import com.atablood.iWindoor_api.dto.company.CompanyDto;
import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto.CostSummary;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto.ProjectInfo;
import com.atablood.iWindoor_api.dto.quote.ProjectQuoteDto.WindowQuoteLine;
import com.atablood.iWindoor_api.dto.window.PricedBom;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.BadRequestException;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.pricing.BomPricer;
import com.atablood.iWindoor_api.pricing.SalesPricer;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.repository.ProjectExtraItemRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectQuoteService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final ProjectExtraItemRepository extraItemRepository;
    private final BomPricer bomPricer;
    private final SalesPricingService salesPricingService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public ProjectQuoteDto quote(Long projectId, BigDecimal discountPctParam, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        guard.checkProject(project, user);

        Company company = companyRepository.findById(guard.companyId(user))
                .orElseThrow(() -> new NotFoundException("Şirket", guard.companyId(user)));

        BigDecimal discountPct = discountPctParam != null
                ? discountPctParam
                : nullToZero(company.getDefaultDiscountPct());
        if (discountPct.compareTo(BigDecimal.ZERO) < 0 || discountPct.compareTo(new BigDecimal("100")) > 0) {
            throw new BadRequestException("VALIDATION_FAILED", "İskonto oranı 0-100 arasında olmalı.");
        }
        discountPct = money(discountPct);

        List<WindowQuoteLine> windows = new ArrayList<>();
        List<QuoteLine> materialAcc = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal materialTotal = BigDecimal.ZERO;
        BigDecimal laborTotal = BigDecimal.ZERO;
        BigDecimal installTotal = BigDecimal.ZERO;

        for (WindowUnit unit : project.getWindowUnits()) {
            int qty = unit.getQuantity() == null || unit.getQuantity() < 1 ? 1 : unit.getQuantity();
            boolean legacy = unit.getDesign() == null;
            Map<String, Object> design = legacy ? DesignDocuments.fromLegacy(unit) : unit.getDesign();
            String seriesName = unit.getSeries() == null ? null : unit.getSeries().getName();

            List<QuoteLine> lines;
            List<String> warnings = new ArrayList<>();
            BigDecimal materialUnit;

            if (unit.getBomSnapshot() != null) {
                PricedBom priced = bomPricer.price(company.getId(), unit.getSeries(), unit.getBomSnapshot());
                lines = priced.lines();
                materialUnit = priced.total();
                warnings.addAll(priced.warnings());
            } else {
                lines = List.of();
                materialUnit = unit.getUnitPrice() == null
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : money(unit.getUnitPrice());
                warnings.add("Kesim listesi yok; kayıtlı fiyat kullanıldı.");
            }

            // Satış hesabı için geçici material (önbellek güncellemeden)
            WindowUnit probe = new WindowUnit();
            probe.setWidth(unit.getWidth());
            probe.setHeight(unit.getHeight());
            probe.setSeries(unit.getSeries());
            probe.setDesign(unit.getDesign());
            probe.setUnitPrice(materialUnit);
            probe.setSalePriceOverride(unit.getSalePriceOverride());
            SalesPricer.SaleResult sale = salesPricingService.priceWindow(probe, company);
            warnings.addAll(sale.warnings());

            BigDecimal saleUnit = sale.unitPrice();
            BigDecimal lineTotal = money(saleUnit.multiply(BigDecimal.valueOf(qty)));
            subtotal = money(subtotal.add(lineTotal));
            materialTotal = materialTotal.add(materialUnit.multiply(BigDecimal.valueOf(qty)));
            laborTotal = laborTotal.add(sale.breakdown().labor().multiply(BigDecimal.valueOf(qty)));
            installTotal = installTotal.add(sale.breakdown().install().multiply(BigDecimal.valueOf(qty)));

            windows.add(new WindowQuoteLine(
                    unit.getId(), unit.getName(), qty, unit.getWidth(), unit.getHeight(),
                    seriesName, design, money(materialUnit), saleUnit,
                    unit.getSalePriceOverride() != null, lineTotal, lines, warnings));

            for (QuoteLine line : lines) {
                materialAcc.add(scaleLine(line, qty));
            }
        }

        List<ProjectExtraItemDto> extras = new ArrayList<>();
        for (ProjectExtraItem extra : extraItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(projectId)) {
            ProjectExtraItemDto dto = ProjectExtraItemDto.of(extra);
            extras.add(dto);
            subtotal = money(subtotal.add(dto.lineTotal()));
        }

        List<QuoteLine> materials = mergeMaterials(materialAcc);

        BigDecimal discountAmount = money(subtotal.multiply(discountPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        BigDecimal netTotal = money(subtotal.subtract(discountAmount));
        BigDecimal vatRate = money(nullToZero(company.getVatRate()));
        BigDecimal vatAmount = money(netTotal.multiply(vatRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        BigDecimal grandTotal = money(netTotal.add(vatAmount));

        CostSummary costSummary = null;
        if (user.getRole() == Role.COMPANY_ADMIN || user.getRole() == Role.ADMIN) {
            materialTotal = money(materialTotal);
            laborTotal = money(laborTotal);
            installTotal = money(installTotal);
            BigDecimal grossProfit = money(subtotal.subtract(materialTotal).subtract(laborTotal).subtract(installTotal));
            BigDecimal grossMarginPct = subtotal.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                    : money(grossProfit.multiply(new BigDecimal("100")).divide(subtotal, 4, RoundingMode.HALF_UP));
            costSummary = new CostSummary(materialTotal, laborTotal, installTotal, subtotal, grossProfit, grossMarginPct);
        }

        int validityDays = company.getQuoteValidityDays() == null ? 15 : company.getQuoteValidityDays();

        return new ProjectQuoteDto(
                Instant.now(),
                LocalDate.now().plusDays(validityDays),
                company.getCurrency() == null ? "TRY" : company.getCurrency(),
                CompanyDto.of(company),
                ProjectInfo.of(project),
                windows,
                extras,
                materials,
                subtotal,
                discountPct,
                discountAmount,
                netTotal,
                vatRate,
                vatAmount,
                grandTotal,
                costSummary
        );
    }

    private static QuoteLine scaleLine(QuoteLine line, int windowQty) {
        BigDecimal qty = BigDecimal.valueOf(line.quantity()).multiply(BigDecimal.valueOf(windowQty));
        BigDecimal total = money(line.total().multiply(BigDecimal.valueOf(windowQty)));
        return new QuoteLine(line.group(), line.code(), line.name(), qty.doubleValue(),
                line.unit(), line.unitPrice(), total);
    }

    private static List<QuoteLine> mergeMaterials(List<QuoteLine> lines) {
        Map<String, QuoteLine> merged = new LinkedHashMap<>();
        for (QuoteLine line : lines) {
            String key = line.group() + "\0" + (line.code() == null ? "" : line.code());
            QuoteLine existing = merged.get(key);
            if (existing == null) {
                merged.put(key, line);
            } else {
                BigDecimal qty = BigDecimal.valueOf(existing.quantity()).add(BigDecimal.valueOf(line.quantity()));
                BigDecimal total = money(existing.total().add(line.total()));
                merged.put(key, new QuoteLine(existing.group(), existing.code(), existing.name(),
                        qty.doubleValue(), existing.unit(), existing.unitPrice(), total));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : v;
    }
}
