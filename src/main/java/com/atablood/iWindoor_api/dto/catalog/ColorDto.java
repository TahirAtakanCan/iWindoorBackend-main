package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Color;

import java.math.BigDecimal;

public record ColorDto(
        Long id,
        String code,
        String name,
        String hexOutside,
        String hexInside,
        BigDecimal surchargePct,
        boolean isActive,
        int sortOrder
) {
    public static ColorDto of(Color c) {
        return new ColorDto(c.getId(), c.getCode(), c.getName(), c.getHexOutside(), c.getHexInside(),
                c.getSurchargePct(), c.isActive(), c.getSortOrder());
    }
}
