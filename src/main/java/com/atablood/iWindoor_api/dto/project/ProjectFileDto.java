package com.atablood.iWindoor_api.dto.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** .iwd proje dosyası (dışa / içe aktarma). */
public record ProjectFileDto(
        String format,
        Integer version,
        Instant exportedAt,
        ProjectPart project,
        List<WindowPart> windows,
        List<ExtraPart> extras,
        List<SurveyPart> survey
) {
    public record ProjectPart(
            String name,
            String description,
            String customerName,
            String customerPhone,
            String customerEmail,
            String customerAddress
    ) {
    }

    public record WindowPart(
            String name,
            Integer quantity,
            Double width,
            Double height,
            String seriesName,
            Map<String, Object> design,
            List<Map<String, Object>> bom,
            BigDecimal salePriceOverride
    ) {
    }

    public record ExtraPart(
            String name,
            BigDecimal quantity,
            String unit,
            BigDecimal unitPrice,
            Integer sortOrder
    ) {
    }

    public record SurveyPart(
            String room,
            String productType,
            BigDecimal holeWidth,
            BigDecimal holeHeight,
            Integer quantity,
            String note,
            Integer sortOrder
    ) {
    }
}
