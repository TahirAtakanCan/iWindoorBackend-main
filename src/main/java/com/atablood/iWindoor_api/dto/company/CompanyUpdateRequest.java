package com.atablood.iWindoor_api.dto.company;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CompanyUpdateRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 255) String address,
        @Size(max = 255) String taxNumber,
        @Size(max = 120) String taxOffice,
        @Size(max = 50) String phone,
        @Size(max = 200) String email,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal vatRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal defaultDiscountPct,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Size(max = 2000) String quoteNote,
        @NotNull @Min(1) @Max(365) Integer quoteValidityDays,
        @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal sawKerfMm,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal barTrimMm,
        @NotNull @Min(0) @Max(6000) Integer minOffcutMm,
        @Size(max = 200) String glassSupplierName,
        @Size(max = 50) String glassSupplierPhone,
        @Size(max = 200) String glassSupplierEmail,
        @Size(max = 1000) String glassOrderNote,
        @NotBlank @Pattern(regexp = "COST_PLUS|PER_M2") String pricingMode,
        @NotNull @DecimalMin("0") @DecimalMax("500") BigDecimal profitMarginPct,
        @NotNull @DecimalMin("0") BigDecimal laborPerM2,
        @NotNull @DecimalMin("0") BigDecimal installPerM2,
        @NotNull @DecimalMin("0") @DecimalMax("10") BigDecimal minBillableAreaM2,
        @NotNull @DecimalMin("0") @DecimalMax("50") BigDecimal surveyGapMm,
        @NotNull @DecimalMin("0") @DecimalMax("5") BigDecimal screenMinAreaM2,
        @NotNull @DecimalMin("0") @DecimalMax("5") BigDecimal shutterMinAreaM2
) {
}
