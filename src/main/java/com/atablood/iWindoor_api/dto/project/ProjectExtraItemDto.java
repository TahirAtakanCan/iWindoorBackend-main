package com.atablood.iWindoor_api.dto.project;

import com.atablood.iWindoor_api.entity.ProjectExtraItem;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProjectExtraItemDto(
        Long id,
        String name,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        Integer sortOrder,
        BigDecimal lineTotal
) {
    public static ProjectExtraItemDto of(ProjectExtraItem e) {
        BigDecimal qty = e.getQuantity() == null ? BigDecimal.ONE : e.getQuantity();
        BigDecimal price = e.getUnitPrice() == null ? BigDecimal.ZERO : e.getUnitPrice();
        return new ProjectExtraItemDto(
                e.getId(),
                e.getName(),
                qty,
                e.getUnit(),
                price,
                e.getSortOrder(),
                qty.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP)
        );
    }

    public record Request(
            @NotBlank @Size(max = 200) String name,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotBlank @Size(max = 16) String unit,
            @NotNull @DecimalMin("0") BigDecimal unitPrice,
            Integer sortOrder
    ) {
    }
}
