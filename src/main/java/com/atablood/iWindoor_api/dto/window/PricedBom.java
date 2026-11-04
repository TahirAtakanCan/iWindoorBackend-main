package com.atablood.iWindoor_api.dto.window;

import java.math.BigDecimal;
import java.util.List;

/** Bir pencerenin (1 adet) fiyatlanmış kesim listesi. */
public record PricedBom(List<QuoteLine> lines, BigDecimal total, String currency, List<String> warnings) {
}
