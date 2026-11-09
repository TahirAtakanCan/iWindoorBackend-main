package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "colors")
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "hex_outside", nullable = false, length = 9)
    private String hexOutside;

    @Column(name = "hex_inside", nullable = false, length = 9)
    private String hexInside;

    @Column(name = "surcharge_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal surchargePct = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
