package com.atablood.iWindoor_api.dto.project;

import com.atablood.iWindoor_api.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PaymentDto(
        Long id,
        String kind,
        BigDecimal amount,
        LocalDate paidAt,
        String method,
        String note,
        Long createdById,
        Instant createdAt
) {
    public static PaymentDto of(Payment p) {
        return new PaymentDto(p.getId(), p.getKind(), p.getAmount(), p.getPaidAt(), p.getMethod(),
                p.getNote(), p.getCreatedById(), p.getCreatedAt());
    }

    public record ListResponse(
            List<PaymentDto> payments,
            BigDecimal paid,
            BigDecimal target,
            BigDecimal balance
    ) {
    }
}
