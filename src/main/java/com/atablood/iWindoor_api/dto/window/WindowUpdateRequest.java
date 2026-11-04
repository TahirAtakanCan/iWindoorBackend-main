package com.atablood.iWindoor_api.dto.window;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

/**
 * Tüm pencere belgesi tek seferde gönderilir (PUT). rowVersion son okunan değer olmalı; değilse 409 STALE_VERSION.
 * bom: istemcinin çizim motorunda hesapladığı kesim listesi (1 adet pencere için).
 */
public record WindowUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Positive @Max(10000) Integer quantity,
        Long seriesId,
        @NotNull Map<String, Object> design,
        List<Map<String, Object>> bom,
        @NotNull Long rowVersion
) {
}
