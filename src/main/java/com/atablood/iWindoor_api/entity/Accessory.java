package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "accessories")
public class Accessory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Örn: "Beyaz Alüminyum Kol", "EPDM Conta"

    @Column(unique = true)
    private String code; // Stok kodu

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessoryUnit unit; // PIECE veya METER

    @Column(nullable = false)
    private BigDecimal price; // Birim fiyatı

    @Column(name = "currency")
    private String currency = "TRY";
}
