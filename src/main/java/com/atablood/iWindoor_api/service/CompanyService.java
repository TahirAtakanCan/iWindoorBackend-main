package com.atablood.iWindoor_api.service;

import com.atablood.iWindoor_api.dto.company.CompanyDto;
import com.atablood.iWindoor_api.dto.company.CompanyUpdateRequest;
import com.atablood.iWindoor_api.entity.Company;
import com.atablood.iWindoor_api.entity.User;
import com.atablood.iWindoor_api.exception.NotFoundException;
import com.atablood.iWindoor_api.repository.CompanyRepository;
import com.atablood.iWindoor_api.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SalesPricingService salesPricingService;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public CompanyDto getMine(User user) {
        return CompanyDto.of(load(user));
    }

    @Transactional
    public CompanyDto updateMine(CompanyUpdateRequest req, User user) {
        Company c = load(user);
        boolean pricingChanged = pricingChanged(c, req);
        c.setName(req.name().trim());
        c.setAddress(req.address());
        c.setTaxNumber(req.taxNumber());
        c.setTaxOffice(req.taxOffice());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setVatRate(money(req.vatRate()));
        c.setDefaultDiscountPct(money(req.defaultDiscountPct()));
        c.setCurrency(req.currency());
        c.setQuoteNote(req.quoteNote());
        c.setQuoteValidityDays(req.quoteValidityDays());
        c.setSawKerfMm(mm1(req.sawKerfMm()));
        c.setBarTrimMm(mm1(req.barTrimMm()));
        c.setMinOffcutMm(req.minOffcutMm());
        c.setGlassSupplierName(req.glassSupplierName());
        c.setGlassSupplierPhone(req.glassSupplierPhone());
        c.setGlassSupplierEmail(req.glassSupplierEmail());
        c.setGlassOrderNote(req.glassOrderNote());
        c.setPricingMode(req.pricingMode());
        c.setProfitMarginPct(money(req.profitMarginPct()));
        c.setLaborPerM2(money(req.laborPerM2()));
        c.setInstallPerM2(money(req.installPerM2()));
        c.setMinBillableAreaM2(money(req.minBillableAreaM2()));
        Company saved = companyRepository.save(c);
        if (pricingChanged) {
            salesPricingService.recalculateCompanySales(saved.getId());
        }
        return CompanyDto.of(saved);
    }

    private static boolean pricingChanged(Company c, CompanyUpdateRequest req) {
        return !Objects.equals(c.getPricingMode(), req.pricingMode())
                || money(c.getProfitMarginPct()).compareTo(money(req.profitMarginPct())) != 0
                || money(c.getLaborPerM2()).compareTo(money(req.laborPerM2())) != 0
                || money(c.getInstallPerM2()).compareTo(money(req.installPerM2())) != 0
                || money(c.getMinBillableAreaM2()).compareTo(money(req.minBillableAreaM2())) != 0;
    }

    private Company load(User user) {
        Long id = guard.companyId(user);
        return companyRepository.findById(id).orElseThrow(() -> new NotFoundException("Şirket", id));
    }

    private static BigDecimal money(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal mm1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }
}
