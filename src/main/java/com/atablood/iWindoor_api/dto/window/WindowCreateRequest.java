package com.atablood.iWindoor_api.dto.window;

import jakarta.validation.constraints.*;

import java.util.Map;

/** Eski mobil uygulama sadece {name,width,height} gönderir; yeni istemci design da gönderebilir. */
public record WindowCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("200") @DecimalMax("12000") Double width,
        @NotNull @DecimalMin("200") @DecimalMax("6000") Double height,
        @Positive @Max(10000) Integer quantity,
        Long seriesId,
        Map<String, Object> design
) {
}
