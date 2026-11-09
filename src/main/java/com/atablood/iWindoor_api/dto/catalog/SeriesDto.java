package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Series;

import java.math.BigDecimal;

public record SeriesDto(Long id, String name, String brand, Integer systemDepthMm, String material, boolean active,
                        SeriesGeometryDto geometry, BigDecimal salePricePerM2) {
    public static SeriesDto of(Series s) {
        return new SeriesDto(s.getId(), s.getName(), s.getBrand(), s.getSystemDepth(), s.getMaterial(), s.isActive(),
                SeriesGeometryDto.of(s), s.getSalePricePerM2());
    }
}
