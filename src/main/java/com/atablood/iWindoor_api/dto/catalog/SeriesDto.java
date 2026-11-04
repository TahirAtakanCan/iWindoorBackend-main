package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Series;

public record SeriesDto(Long id, String name, String brand, Integer systemDepthMm, String material, boolean active,
                        SeriesGeometryDto geometry) {
    public static SeriesDto of(Series s) {
        return new SeriesDto(s.getId(), s.getName(), s.getBrand(), s.getSystemDepth(), s.getMaterial(), s.isActive(),
                SeriesGeometryDto.of(s));
    }
}
