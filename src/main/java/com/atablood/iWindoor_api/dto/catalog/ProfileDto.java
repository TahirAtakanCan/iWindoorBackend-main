package com.atablood.iWindoor_api.dto.catalog;

import com.atablood.iWindoor_api.entity.Profile;
import com.atablood.iWindoor_api.entity.ProfileType;

import java.math.BigDecimal;

public record ProfileDto(Long id, Long seriesId, String code, String name, ProfileType type, Double faceWidthMm,
                         Integer barLengthMm, Double minGlassMm, Double maxGlassMm, boolean isDefault, boolean active,
                         BigDecimal pricePerMeter, String currency) {
    public static ProfileDto of(Profile p) {
        Integer bar = p.getBarLengthMm() == null ? 6500 : p.getBarLengthMm();
        return new ProfileDto(p.getId(), p.getSeriesId(), p.getCode(), p.getName(), p.getType(), p.getWidthMm(),
                bar, p.getMinGlassMm(), p.getMaxGlassMm(), p.isDefaultForType(), p.isActive(),
                p.getPricePerMeter(), p.getCurrency());
    }
}
