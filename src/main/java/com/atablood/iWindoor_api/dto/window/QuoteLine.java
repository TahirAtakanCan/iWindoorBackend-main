package com.atablood.iWindoor_api.dto.window;

import java.math.BigDecimal;

/** Fiyatlanmış kesim listesi satırı. quantity birimi: m (çubuk), m2 (cam/panel). */
public record QuoteLine(
        String group,
        String code,
        String name,
        double quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal total
) {
}
