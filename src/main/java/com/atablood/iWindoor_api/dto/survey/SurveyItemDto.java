package com.atablood.iWindoor_api.dto.survey;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SurveyItemDto(
        Long id,
        String room,
        String productType,
        BigDecimal holeWidth,
        BigDecimal holeHeight,
        Integer quantity,
        String note,
        Integer sortOrder,
        Long windowUnitId,
        List<Long> photoIds,
        BigDecimal suggestedWidth,
        BigDecimal suggestedHeight,
        Instant createdAt,
        String clientId
) {
    public record Request(
            @NotBlank @Size(max = 120) String room,
            @NotBlank @Pattern(regexp = "WINDOW|DOOR|BALCONY|SLIDING|OTHER") String productType,
            @NotNull @DecimalMin("200") @DecimalMax("12000") BigDecimal holeWidth,
            @NotNull @DecimalMin("200") @DecimalMax("12000") BigDecimal holeHeight,
            @NotNull @Min(1) @Max(10000) Integer quantity,
            @Size(max = 1000) String note,
            Integer sortOrder,
            @Size(max = 64) String clientId
    ) {
    }

    public record LinkRequest(Long windowUnitId) {
    }

    /** created=true → 201, false → 200 (idempotent tekrar). */
    public record CreateResult(SurveyItemDto body, boolean created) {
    }

    public record PhotoResult(Long id, boolean created) {
    }
}
