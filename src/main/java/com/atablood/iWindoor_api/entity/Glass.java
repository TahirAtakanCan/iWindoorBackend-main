package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "glasses")
public class Glass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Örn: "4+16+4 Isıcam", "Lamine Cam"

    @Column(name = "thickness_mm")
    private Integer thicknessMm; // Camın toplam kalınlığı (Örn: 24mm). Çıta seçimi için kritik!

    @Column(name = "price_per_m2", nullable = false)
    private BigDecimal pricePerSquareMeter; // m² fiyatı

    @Column(name = "currency")
    private String currency = "TRY"; // Para birimi

    // Stok veya aktiflik durumu
    @Column(name = "is_active")
    private boolean isActive = true;
}
