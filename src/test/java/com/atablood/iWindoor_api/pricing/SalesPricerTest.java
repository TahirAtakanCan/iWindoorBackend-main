package com.atablood.iWindoor_api.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SalesPricerTest {

    private static SalesPricer.Input base(String mode, BigDecimal seriesM2, BigDecimal colorPct,
                                         BigDecimal material, double w, double h, BigDecimal override) {
        return new SalesPricer.Input(
                mode,
                new BigDecimal("30"),
                new BigDecimal("400"),
                new BigDecimal("250"),
                new BigDecimal("1.00"),
                seriesM2,
                colorPct,
                material,
                w,
                h,
                override
        );
    }

    @Test
    void costPlus_example() {
        SalesPricer.SaleResult r = SalesPricer.price(base(
                SalesPricer.MODE_COST_PLUS, null, BigDecimal.ZERO,
                new BigDecimal("5758.31"), 1800, 1700, null));
        assertThat(r.unitPrice()).isEqualByComparingTo("10071.50");
        assertThat(r.breakdown().labor()).isEqualByComparingTo("1224.00");
        assertThat(r.breakdown().install()).isEqualByComparingTo("765.00");
        assertThat(r.warnings()).isEmpty();
    }

    @Test
    void perM2_noColorAndWithColor() {
        SalesPricer.SaleResult plain = SalesPricer.price(base(
                SalesPricer.MODE_PER_M2, new BigDecimal("3500"), BigDecimal.ZERO,
                new BigDecimal("5758.31"), 1800, 1700, null));
        assertThat(plain.unitPrice()).isEqualByComparingTo("10710.00");

        SalesPricer.SaleResult colored = SalesPricer.price(base(
                SalesPricer.MODE_PER_M2, new BigDecimal("3500"), new BigDecimal("25"),
                new BigDecimal("5758.31"), 1800, 1700, null));
        assertThat(colored.unitPrice()).isEqualByComparingTo("13387.50");
    }

    @Test
    void perM2_minBillableArea() {
        SalesPricer.SaleResult r = SalesPricer.price(base(
                SalesPricer.MODE_PER_M2, new BigDecimal("3500"), BigDecimal.ZERO,
                new BigDecimal("100"), 600, 600, null));
        assertThat(r.breakdown().billableArea()).isEqualByComparingTo("1.00");
        assertThat(r.unitPrice()).isEqualByComparingTo("3500.00");
    }

    @Test
    void perM2_missingSeriesPrice_fallsBackToCostPlus() {
        SalesPricer.SaleResult r = SalesPricer.price(base(
                SalesPricer.MODE_PER_M2, null, BigDecimal.ZERO,
                new BigDecimal("5758.31"), 1800, 1700, null));
        assertThat(r.unitPrice()).isEqualByComparingTo("10071.50");
        assertThat(r.warnings()).containsExactly("Seride m² fiyatı yok");
    }

    @Test
    void override_wins() {
        SalesPricer.SaleResult r = SalesPricer.price(base(
                SalesPricer.MODE_COST_PLUS, null, BigDecimal.ZERO,
                new BigDecimal("5758.31"), 1800, 1700, new BigDecimal("12500.00")));
        assertThat(r.unitPrice()).isEqualByComparingTo("12500.00");
    }
}
