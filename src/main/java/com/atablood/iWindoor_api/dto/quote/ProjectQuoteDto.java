package com.atablood.iWindoor_api.dto.quote;

import com.atablood.iWindoor_api.dto.company.CompanyDto;
import com.atablood.iWindoor_api.dto.window.QuoteLine;

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
        List<QuoteLine> materials,
        BigDecimal subtotal,
        BigDecimal discountPct,
        BigDecimal discountAmount,
        BigDecimal netTotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal grandTotal
) {
    public record ProjectInfo(Long id, String name, String description) {
    }

    public record WindowQuoteLine(
            Long windowId,
            String name,
            Integer quantity,
            Double width,
            Double height,
            String seriesName,
            Map<String, Object> design,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            List<QuoteLine> lines,
            List<String> warnings
    ) {
    }
}
