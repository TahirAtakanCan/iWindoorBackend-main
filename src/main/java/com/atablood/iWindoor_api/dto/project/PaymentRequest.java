package com.atablood.iWindoor_api.dto.project;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotBlank @Pattern(regexp = "PAYMENT|REFUND") String kind,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @NotNull LocalDate paidAt,
        @NotBlank @Pattern(regexp = "CASH|CARD|TRANSFER|CHECK|OTHER") String method,
        @Size(max = 500) String note
) {
}
