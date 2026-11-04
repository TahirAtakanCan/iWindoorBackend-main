package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Series;

/** Alan adları Dart tarafındaki SeriesGeometry.fromJson ile birebir aynıdır. Hepsi mm. */
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
        double duplexWidth
) {
    public static SeriesGeometryDto of(Series s) {
        return new SeriesGeometryDto(String.valueOf(s.getId()), s.getFrameFaceMm(), s.getSashFaceMm(), s.getMullionFaceMm(),
                s.getBeadFaceMm(), s.getSashLapMm(), s.getGlassRebateFrameMm(), s.getGlassRebateSashMm(),
                s.getGlassRebateMullionMm(), s.getWeldAllowanceMm(), s.getMullionEndAllowanceMm(),
                s.getSlideInterlockMm(), s.getDuplexWidthMm());
    }
}
