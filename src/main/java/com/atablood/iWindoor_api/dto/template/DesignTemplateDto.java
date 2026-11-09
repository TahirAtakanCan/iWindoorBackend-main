package com.atablood.iWindoor_api.dto.template;

import java.time.Instant;
import java.util.Map;

public record DesignTemplateDto(
        Long id,
        String name,
        String category,
        int sortOrder,
        Map<String, Object> design,
        Instant updatedAt
) {
}
