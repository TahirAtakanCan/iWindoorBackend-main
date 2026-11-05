package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.AccessoryUnit;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record AccessoryRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull AccessoryUnit unit,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean isActive
) {
}
