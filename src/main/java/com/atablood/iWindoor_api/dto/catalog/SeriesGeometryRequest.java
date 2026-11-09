package com.atablood.iWindoor_api.dto.catalog;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Boş bırakılan alanlar değişmez. Hepsi mm (maxFixedGlassArea m2). */
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
        @PositiveOrZero @DecimalMax("100") Double duplexWidth,
        @Positive @DecimalMax("5000") Double minSashWidth,
        @Positive @DecimalMax("5000") Double minSashHeight,
        @Positive @DecimalMax("10000") Double maxSashWidth,
        @Positive @DecimalMax("10000") Double maxSashHeight,
        @Positive @DecimalMax("10000") Double maxSlideSashWidth,
        @Positive @DecimalMax("100") Double maxFixedGlassArea,
        @PositiveOrZero @DecimalMax("300") Double adapterFace,
        @PositiveOrZero @DecimalMax("300") Double doorSashFace,
        @PositiveOrZero @DecimalMax("300") Double thresholdFace,
        @PositiveOrZero @DecimalMax("50") Double thresholdEndAllowance,
        @Positive @DecimalMax("5000") Double minDoorSashWidth,
        @Positive @DecimalMax("10000") Double maxDoorSashWidth,
        @Positive @DecimalMax("5000") Double minDoorSashHeight,
        @Positive @DecimalMax("10000") Double maxDoorSashHeight,
        @PositiveOrZero @DecimalMax("300") Double couplingFace,
        @PositiveOrZero @DecimalMax("100") Double couplingGap
) {
}
