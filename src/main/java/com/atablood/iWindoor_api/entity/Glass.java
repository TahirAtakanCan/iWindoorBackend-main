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

    // Tasarım belgesindeki cam/panel düğümü bu koda referans verir (şirket içinde benzersiz)
    @Column(length = 64)
    private String code;

    @Column(nullable = false)
    private String name; // Örn: "4+16+4 Isıcam"

    @Column(name = "thickness_mm")
    private Integer thicknessMm; // Toplam kalınlık. Çıta seçimi buna göre yapılır.

    // Çizimdeki görünüm: clear | lowe | smoke | bronze | mirror | frost | pattern | lam | panel
    @Column(name = "visual_style", nullable = false, length = 16)
    private String visualStyle = "clear";

    @Column(name = "price_per_m2", nullable = false)
    private BigDecimal pricePerSquareMeter;

    @Column(name = "currency")
    private String currency = "TRY";

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "is_active")
    private boolean isActive = true;
}
