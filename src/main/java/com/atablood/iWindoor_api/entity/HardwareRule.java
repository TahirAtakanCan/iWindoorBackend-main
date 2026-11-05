package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "hardware_rules")
public class HardwareRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "series_id")
    private Long seriesId;

    @Column(nullable = false, length = 16)
    private String kind;

    @Column(name = "accessory_code", nullable = false, length = 64)
    private String accessoryCode;

    @Column(name = "qty_type", nullable = false, length = 24)
    private String qtyType;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal qty = BigDecimal.ONE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
