package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SeriesRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String brand,
        @Positive Integer systemDepthMm,
        @Pattern(regexp = "PVC|ALU") String material,
        @Valid SeriesGeometryRequest geometry,
        @DecimalMin("0") BigDecimal salePricePerM2
) {
}
