package com.atablood.iWindoor_api.dto.template;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record DesignTemplateUpdateRequest(
        @Size(max = 120) String name,
        String category,
        Integer sortOrder,
        Map<String, Object> design
) {
}
