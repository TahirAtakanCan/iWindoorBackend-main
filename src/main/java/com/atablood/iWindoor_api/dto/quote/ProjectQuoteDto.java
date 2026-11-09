package com.atablood.iWindoor_api.dto.quote;

import com.atablood.iWindoor_api.dto.company.CompanyDto;
import com.atablood.iWindoor_api.dto.project.ProjectExtraItemDto;
import com.atablood.iWindoor_api.dto.window.QuoteLine;
import com.atablood.iWindoor_api.entity.Project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ProjectQuoteDto(
        Instant generatedAt,
        LocalDate validUntil,
        String currency,
        CompanyDto company,
        ProjectInfo project,
        List<WindowQuoteLine> windows,
        List<ProjectExtraItemDto> extras,
        List<QuoteLine> materials,
        BigDecimal subtotal,
        BigDecimal discountPct,
        BigDecimal discountAmount,
        BigDecimal netTotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal,
        CostSummary costSummary
) {
    public record ProjectInfo(
            Long id,
            String name,
            String description,
            String customerName,
            String customerPhone,
            String customerEmail,
            String customerAddress
    ) {
        public static ProjectInfo of(Project p) {
            return new ProjectInfo(p.getId(), p.getName(), p.getDescription(),
                    p.getCustomerName(), p.getCustomerPhone(), p.getCustomerEmail(), p.getCustomerAddress());
        }
    }

    public record WindowQuoteLine(
            Long windowId,
            String name,
            Integer quantity,
            Double width,
            Double height,
            String seriesName,
            Map<String, Object> design,
            BigDecimal materialUnitPrice,
            BigDecimal saleUnitPrice,
            boolean overridden,
            BigDecimal lineTotal,
            List<QuoteLine> lines,
            List<String> warnings
    ) {
    }

    public record CostSummary(
            BigDecimal materialTotal,
            BigDecimal laborTotal,
            BigDecimal installTotal,
            BigDecimal saleSubtotal,
            BigDecimal grossProfit,
            BigDecimal grossMarginPct
    ) {
    }
}
