package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Series;

/** Alan adları Dart tarafındaki SeriesGeometry.fromJson ile birebir aynıdır. Hepsi mm (alan m2 hariç). */
public record SeriesGeometryDto(
        String id,
        double frameFace,
        double sashFace,
        double mullionFace,
        double beadFace,
        double sashLap,
        double glassRebateFrame,
        double glassRebateSash,
        double glassRebateMullion,
        double weldAllowance,
        double mullionEndAllowance,
        double slideInterlock,
        double duplexWidth,
        double minSashWidth,
        double minSashHeight,
        double maxSashWidth,
        double maxSashHeight,
        double maxSlideSashWidth,
        double maxLiftSlideSashWidth,
        double maxFixedGlassArea,
        double adapterFace,
        double doorSashFace,
        double thresholdFace,
        double thresholdEndAllowance,
        double minDoorSashWidth,
        double maxDoorSashWidth,
        double minDoorSashHeight,
        double maxDoorSashHeight,
        double couplingFace,
        double couplingGap,
        double shutterBoxHeight,
        double sillOverhang,
        double trimWidth
) {
    public static SeriesGeometryDto of(Series s) {
        return new SeriesGeometryDto(String.valueOf(s.getId()), s.getFrameFaceMm(), s.getSashFaceMm(), s.getMullionFaceMm(),
                s.getBeadFaceMm(), s.getSashLapMm(), s.getGlassRebateFrameMm(), s.getGlassRebateSashMm(),
                s.getGlassRebateMullionMm(), s.getWeldAllowanceMm(), s.getMullionEndAllowanceMm(),
                s.getSlideInterlockMm(), s.getDuplexWidthMm(),
                s.getMinSashWidthMm(), s.getMinSashHeightMm(), s.getMaxSashWidthMm(), s.getMaxSashHeightMm(),
                s.getMaxSlideSashWidthMm(), s.getMaxLiftSlideSashWidthMm(), s.getMaxFixedGlassAreaM2(),
                s.getAdapterFaceMm(), s.getDoorSashFaceMm(), s.getThresholdFaceMm(), s.getThresholdEndAllowanceMm(),
                s.getMinDoorSashWidthMm(), s.getMaxDoorSashWidthMm(),
                s.getMinDoorSashHeightMm(), s.getMaxDoorSashHeightMm(),
                s.getCouplingFaceMm(), s.getCouplingGapMm(),
                s.getShutterBoxHeightMm(), s.getSillOverhangMm(), s.getTrimWidthMm());
    }
}
