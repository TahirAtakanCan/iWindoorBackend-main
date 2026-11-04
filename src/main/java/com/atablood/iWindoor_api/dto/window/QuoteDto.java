package com.atablood.iWindoor_api.dto.window;

import java.math.BigDecimal;
import java.util.List;

/** Güncel katalog fiyatlarıyla teklif. unitTotal 1 adet, total = unitTotal x quantity. */
public record QuoteDto(Long windowId, List<QuoteLine> lines, BigDecimal unitTotal, int quantity, BigDecimal total,
                       String currency, List<String> warnings) {
}
