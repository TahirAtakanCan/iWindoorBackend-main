package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PriceAdjustmentRequest(
        @NotBlank String target,
        @NotNull @DecimalMin("-50") @DecimalMax("100") BigDecimal percent
) {
}
