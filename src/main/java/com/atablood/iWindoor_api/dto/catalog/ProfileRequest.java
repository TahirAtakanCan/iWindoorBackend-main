package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.ProfileType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProfileRequest(
        @NotNull Long seriesId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull ProfileType type,
        @Positive Double faceWidthMm,
        @Positive Integer barLengthMm,
        @PositiveOrZero Double minGlassMm,
        @PositiveOrZero Double maxGlassMm,
        Boolean isDefault,
        @NotNull @PositiveOrZero BigDecimal pricePerMeter,
        @Size(max = 8) String currency
) {
}
