package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Accessory;
import com.atablood.iWindoor_api.entity.AccessoryUnit;

import java.math.BigDecimal;

public record AccessoryDto(
        Long id,
        String code,
        String name,
        AccessoryUnit unit,
        BigDecimal price,
        String currency,
        boolean isActive
) {
    public static AccessoryDto of(Accessory a) {
        return new AccessoryDto(a.getId(), a.getCode(), a.getName(), a.getUnit(), a.getPrice(), a.getCurrency(),
                a.isActive());
    }
}
