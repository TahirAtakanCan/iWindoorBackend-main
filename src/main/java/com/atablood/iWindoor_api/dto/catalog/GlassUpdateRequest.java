package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record GlassUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Positive @Max(200) Integer thicknessMm,
        @Pattern(regexp = "clear|lowe|smoke|bronze|mirror|frost|pattern|lam|panel") String visualStyle,
        @NotNull @PositiveOrZero BigDecimal pricePerSquareMeter,
        Boolean isActive
) {
}
