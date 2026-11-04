package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Glass;

import java.math.BigDecimal;

public record GlassDto(Long id, String code, String name, Integer thicknessMm, String visualStyle,
                       BigDecimal pricePerSquareMeter, String currency) {
    public static GlassDto of(Glass g) {
        return new GlassDto(g.getId(), g.getCode(), g.getName(), g.getThicknessMm(), g.getVisualStyle(),
                g.getPricePerSquareMeter(), g.getCurrency());
    }
}
