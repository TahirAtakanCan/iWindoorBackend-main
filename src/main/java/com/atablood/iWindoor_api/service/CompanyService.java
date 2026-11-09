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

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AccessGuard guard;

    @Transactional(readOnly = true)
    public CompanyDto getMine(User user) {
        return CompanyDto.of(load(user));
    }

    @Transactional
    public CompanyDto updateMine(CompanyUpdateRequest req, User user) {
        Company c = load(user);
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
        return CompanyDto.of(companyRepository.save(c));
    }

    private Company load(User user) {
        Long id = guard.companyId(user);
        return companyRepository.findById(id).orElseThrow(() -> new NotFoundException("Şirket", id));
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal mm1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }
}
