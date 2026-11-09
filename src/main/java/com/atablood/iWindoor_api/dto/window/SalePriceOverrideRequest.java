package com.atablood.iWindoor_api.dto.window;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record SalePriceOverrideRequest(
        @DecimalMin("0") BigDecimal override
) {
}
