package com.atablood.iWindoor_api.dto.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record DesignTemplateCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank String category,
        @NotNull Map<String, Object> design
) {
}
