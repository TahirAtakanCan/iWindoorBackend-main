package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProfileUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @PositiveOrZero BigDecimal pricePerMeter,
        @Positive @Max(20000) Integer barLength,
        Boolean isActive,
        Boolean isDefault
) {
}
