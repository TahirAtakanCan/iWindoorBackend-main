package com.atablood.iWindoor_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CostItemDTO {
    private String name;        // Örn: "60'lık Kasa Profili"
    private String category;    // Örn: "Profil", "Cam", "Aksesuar"
    private double quantity;    // Örn: 12.5
    private String unit;        // Örn: "m", "m2", "adet"
    private BigDecimal price;   // Örn: 1250.00 TL
}