package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;

/** Boş bırakılan alanlar değişmez. Hepsi mm. */
public record SeriesGeometryRequest(
        @PositiveOrZero @DecimalMax("300") Double frameFace,
        @PositiveOrZero @DecimalMax("300") Double sashFace,
        @PositiveOrZero @DecimalMax("300") Double mullionFace,
        @PositiveOrZero @DecimalMax("100") Double beadFace,
        @PositiveOrZero @DecimalMax("100") Double sashLap,
        @PositiveOrZero @DecimalMax("100") Double glassRebateFrame,
        @PositiveOrZero @DecimalMax("100") Double glassRebateSash,
        @PositiveOrZero @DecimalMax("100") Double glassRebateMullion,
        @PositiveOrZero @DecimalMax("50") Double weldAllowance,
        @PositiveOrZero @DecimalMax("50") Double mullionEndAllowance,
        @PositiveOrZero @DecimalMax("200") Double slideInterlock,
        @PositiveOrZero @DecimalMax("100") Double duplexWidth
) {
}
