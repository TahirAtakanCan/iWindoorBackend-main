package com.atablood.iWindoor_api.dto.project;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** .iwd proje dosyası (dışa / içe aktarma). */
public record ProjectFileDto(
        String format,
        Integer version,
        Instant exportedAt,
        ProjectPart project,
        List<WindowPart> windows
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
            List<Map<String, Object>> bom
    ) {
    }
}
