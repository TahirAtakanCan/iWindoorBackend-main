package com.atablood.iWindoor_api.dto.company;

import com.atablood.iWindoor_api.entity.Company;

import java.math.BigDecimal;

public record CompanyDto(
        Long id,
        String name,
        String address,
        String taxNumber,
        String taxOffice,
        String phone,
        String email,
        BigDecimal vatRate,
        BigDecimal defaultDiscountPct,
        String currency,
        String quoteNote,
        Integer quoteValidityDays,
        BigDecimal sawKerfMm,
        BigDecimal barTrimMm,
        Integer minOffcutMm,
        String glassSupplierName,
        String glassSupplierPhone,
        String glassSupplierEmail,
        String glassOrderNote,
        String pricingMode,
        BigDecimal profitMarginPct,
        BigDecimal laborPerM2,
        BigDecimal installPerM2,
        BigDecimal minBillableAreaM2,
        BigDecimal surveyGapMm
) {
    public static CompanyDto of(Company c) {
        return new CompanyDto(
                c.getId(),
                c.getName(),
                c.getAddress(),
                c.getTaxNumber(),
                c.getTaxOffice(),
                c.getPhone(),
                c.getEmail(),
                c.getVatRate(),
                c.getDefaultDiscountPct(),
                c.getCurrency(),
                c.getQuoteNote(),
                c.getQuoteValidityDays(),
                c.getSawKerfMm(),
                c.getBarTrimMm(),
                c.getMinOffcutMm(),
                c.getGlassSupplierName(),
                c.getGlassSupplierPhone(),
                c.getGlassSupplierEmail(),
                c.getGlassOrderNote(),
                c.getPricingMode(),
                c.getProfitMarginPct(),
                c.getLaborPerM2(),
                c.getInstallPerM2(),
                c.getMinBillableAreaM2(),
                c.getSurveyGapMm()
        );
    }
}
