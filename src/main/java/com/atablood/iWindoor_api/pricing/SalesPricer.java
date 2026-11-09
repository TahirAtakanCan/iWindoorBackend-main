package com.atablood.iWindoor_api.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Saf satış fiyatı hesabı (malzeme maliyeti BomPricer'dan gelir). */
public final class SalesPricer {

    public static final String MODE_COST_PLUS = "COST_PLUS";
    public static final String MODE_PER_M2 = "PER_M2";

    private SalesPricer() {
    }

    public record Input(
            String mode,
            BigDecimal profitMarginPct,
            BigDecimal laborPerM2,
            BigDecimal installPerM2,
            BigDecimal minBillableAreaM2,
            BigDecimal seriesSalePricePerM2,
            BigDecimal colorSurchargePct,
            BigDecimal materialUnit,
            double widthMm,
            double heightMm,
            BigDecimal salePriceOverride
    ) {
    }

    public record Breakdown(
            BigDecimal material,
            BigDecimal labor,
            BigDecimal install,
            BigDecimal margin,
            BigDecimal billableArea
    ) {
    }

    public record SaleResult(BigDecimal unitPrice, Breakdown breakdown, List<String> warnings) {
    }

    public static SaleResult price(Input in) {
        List<String> warnings = new ArrayList<>();
        BigDecimal material = nz(in.materialUnit());
        BigDecimal area = areaM2(in.widthMm(), in.heightMm());
        BigDecimal minArea = nz(in.minBillableAreaM2());
        BigDecimal billable = area.max(minArea);

        if (in.salePriceOverride() != null) {
            BigDecimal o = money(in.salePriceOverride());
            return new SaleResult(o, new Breakdown(material, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, billable),
                    warnings);
        }

        String mode = in.mode() == null ? MODE_COST_PLUS : in.mode();
        if (MODE_PER_M2.equals(mode)) {
            if (in.seriesSalePricePerM2() == null) {
                warnings.add("Seride m² fiyatı yok");
                return costPlus(in, material, billable, warnings);
            }
            BigDecimal base = money(billable.multiply(in.seriesSalePricePerM2()));
            BigDecimal colorPct = nz(in.colorSurchargePct());
            BigDecimal unit = money(base.multiply(BigDecimal.ONE.add(colorPct.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))));
            return new SaleResult(unit, new Breakdown(material, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, billable),
                    warnings);
        }
        return costPlus(in, material, billable, warnings);
    }

    private static SaleResult costPlus(Input in, BigDecimal material, BigDecimal billable, List<String> warnings) {
        BigDecimal labor = money(billable.multiply(nz(in.laborPerM2())));
        BigDecimal install = money(billable.multiply(nz(in.installPerM2())));
        BigDecimal beforeMargin = material.add(labor).add(install);
        BigDecimal marginPct = nz(in.profitMarginPct());
        BigDecimal unit = money(beforeMargin.multiply(BigDecimal.ONE.add(marginPct.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP))));
        BigDecimal marginAmt = money(unit.subtract(beforeMargin));
        return new SaleResult(unit, new Breakdown(material, labor, install, marginAmt, billable), warnings);
    }

    public static BigDecimal areaM2(double widthMm, double heightMm) {
        if (widthMm <= 0 || heightMm <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(widthMm * heightMm / 1_000_000.0).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
