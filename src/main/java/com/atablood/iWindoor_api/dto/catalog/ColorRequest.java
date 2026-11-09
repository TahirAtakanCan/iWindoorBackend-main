package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ColorRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 9) String hexOutside,
        @NotBlank @Size(max = 9) String hexInside,
        @NotNull @DecimalMin("0") @DecimalMax("200") BigDecimal surchargePct,
        Boolean isActive,
        Integer sortOrder
) {
}
