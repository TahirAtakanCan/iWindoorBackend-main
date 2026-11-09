package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.entity.*;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.pricing.SalesPricer;
import com.atablood.iWindoor_api.repository.ColorRepository;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.repository.ProjectExtraItemRepository;
import com.atablood.iWindoor_api.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/** Satış birim fiyatı ve proje sale_total önbelleğini günceller (malzeme maliyeti dokunulmaz). */
@Service
@RequiredArgsConstructor
public class SalesPricingService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final ColorRepository colorRepository;
    private final ProjectExtraItemRepository extraItemRepository;

    @Transactional
    public void recalculateProjectSales(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Proje", projectId));
        Company company = companyOf(project);
        if (company == null) {
            project.setSaleTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            projectRepository.save(project);
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (WindowUnit unit : project.getWindowUnits()) {
            applyWindow(unit, company);
            BigDecimal sale = unit.getSaleUnitPrice() == null ? BigDecimal.ZERO : unit.getSaleUnitPrice();
            total = total.add(sale.multiply(BigDecimal.valueOf(qty(unit))));
        }
        for (ProjectExtraItem extra : extraItemRepository.findByProject_IdOrderBySortOrderAscIdAsc(projectId)) {
            BigDecimal q = extra.getQuantity() == null ? BigDecimal.ONE : extra.getQuantity();
            BigDecimal p = extra.getUnitPrice() == null ? BigDecimal.ZERO : extra.getUnitPrice();
            total = total.add(q.multiply(p));
        }
        project.setSaleTotal(total.setScale(2, RoundingMode.HALF_UP));
        projectRepository.save(project);
    }

    @Transactional
    public void recalculateCompanySales(Long companyId) {
        for (Project project : projectRepository.findAllByCreatedBy_Company_Id(companyId)) {
            recalculateProjectSales(project.getId());
        }
    }

    @Transactional
    public void recalculateSeriesSales(Long companyId, Long seriesId) {
        for (Project project : projectRepository.findAllByCreatedBy_Company_Id(companyId)) {
            boolean uses = project.getWindowUnits().stream()
                    .anyMatch(u -> u.getSeries() != null && seriesId.equals(u.getSeries().getId()));
            if (uses) {
                recalculateProjectSales(project.getId());
            }
        }
    }

    public SalesPricer.SaleResult priceWindow(WindowUnit unit, Company company) {
        BigDecimal material = unit.getUnitPrice() == null ? BigDecimal.ZERO : unit.getUnitPrice();
        double w = unit.getWidth() == null ? 0 : unit.getWidth();
        double h = unit.getHeight() == null ? 0 : unit.getHeight();
        BigDecimal seriesM2 = unit.getSeries() == null ? null : unit.getSeries().getSalePricePerM2();
        BigDecimal colorPct = colorSurchargePct(company.getId(), unit.getDesign());
        return SalesPricer.price(new SalesPricer.Input(
                company.getPricingMode(),
                company.getProfitMarginPct(),
                company.getLaborPerM2(),
                company.getInstallPerM2(),
                company.getMinBillableAreaM2(),
                seriesM2,
                colorPct,
                material,
                w,
                h,
                unit.getSalePriceOverride()
        ));
    }

    public void applyWindow(WindowUnit unit, Company company) {
        BigDecimal next = priceWindow(unit, company).unitPrice();
        if (unit.getSaleUnitPrice() == null || unit.getSaleUnitPrice().compareTo(next) != 0) {
            unit.setSaleUnitPrice(next);
        }
    }

    public void applyWindow(WindowUnit unit, Long companyId) {
        if (companyId == null) return;
        companyRepository.findById(companyId).ifPresent(c -> applyWindow(unit, c));
    }

    private BigDecimal colorSurchargePct(Long companyId, Map<String, Object> design) {
        if (design == null || companyId == null) return BigDecimal.ZERO;
        Object raw = design.get("color");
        if (!(raw instanceof String code) || code.isBlank()) return BigDecimal.ZERO;
        return colorRepository.findFirstByCompanyIdAndCode(companyId, code.trim())
                .map(Color::getSurchargePct)
                .orElse(BigDecimal.ZERO);
    }

    private Company companyOf(Project project) {
        if (project.getCreatedBy() == null || project.getCreatedBy().getCompany() == null) return null;
        Long id = project.getCreatedBy().getCompany().getId();
        return companyRepository.findById(id).orElse(null);
    }

    private static int qty(WindowUnit u) {
        return u.getQuantity() == null || u.getQuantity() < 1 ? 1 : u.getQuantity();
    }

    /** Proje listesi/ayrıntısı için: önbellek yoksa hesapla. */
    @Transactional
    public BigDecimal ensureSaleTotal(Project project) {
        if (project.getSaleTotal() != null) return project.getSaleTotal();
        recalculateProjectSales(project.getId());
        return projectRepository.findById(project.getId()).map(Project::getSaleTotal).orElse(BigDecimal.ZERO);
    }
}
