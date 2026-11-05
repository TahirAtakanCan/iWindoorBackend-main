package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record HardwareRuleRequest(
        @NotBlank String kind,
        Long seriesId,
        @NotBlank @Size(max = 64) String accessoryCode,
        @NotBlank String qtyType,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal qty,
        Integer sortOrder
) {
}
