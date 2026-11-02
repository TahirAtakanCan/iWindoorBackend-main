package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stok kodu (Benzersiz olmalı)
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING) // Enum'ı veritabanına String olarak yazar (0,1 yerine "FRAME" yazar)
    @Column(nullable = false)
    private ProfileType type;

    // Çizim motoru için görsel genişlik/kalınlık (mm cinsinden)
    // Örn: Kasa profili çizimde 64mm yer kaplar.
    @Column(name = "width_mm")
    private Double widthMm;

    // Ağırlık hesabı için (Alüminyum ise kg/m, PVC ise g/m)
    @Column(name = "weight_per_meter")
    private Double weightPerMeter;

    // Fiyatlandırma
    @Column(name = "price_per_meter", nullable = false)
    private BigDecimal pricePerMeter;

    @Column(name = "currency")
    private String currency = "TRY"; // Varsayılan TL

    // İLİŞKİ: Her profil bir seriye aittir.
    @ManyToOne(fetch = FetchType.LAZY) // İhtiyaç duyulmadıkça Seriyi veritabanından çekme (Performans)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;
}
