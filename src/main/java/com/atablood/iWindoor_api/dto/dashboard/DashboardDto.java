package com.atablood.iWindoor_api.dto.dashboard;

import com.atablood.iWindoor_api.entity.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record DashboardDto(
        Map<String, Long> countsByStatus,
        BigDecimal approvedThisMonth,
        BigDecimal receivables,
        java.util.List<DeliveryLine> upcomingDeliveries,
        java.util.List<DeliveryLine> overdueDeliveries
) {
    public record DeliveryLine(
            Long id,
            String name,
            String customerName,
            LocalDate deliveryDate,
            ProjectStatus status
    ) {
    }
}
