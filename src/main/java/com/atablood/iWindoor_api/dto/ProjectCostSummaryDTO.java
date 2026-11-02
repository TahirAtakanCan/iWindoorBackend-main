package com.atablood.iWindoor_api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectCostSummaryDTO {
    private List<CostItemDTO> items = new ArrayList<>();
    private BigDecimal totalCost = BigDecimal.ZERO;

    public void addItem(CostItemDTO item) {
        this.items.add(item);
        if (item.getPrice() != null) {
            this.totalCost = this.totalCost.add(item.getPrice());
        }
    }
}