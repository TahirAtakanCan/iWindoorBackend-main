package com.atablood.iWindoor_api.dto.catalog;

import java.math.BigDecimal;

public record HardwareRuleDto(
        Long id,
        String kind,
        Long seriesId,
        String accessoryCode,
        String accessoryName,
        String qtyType,
        BigDecimal qty
) {
}
