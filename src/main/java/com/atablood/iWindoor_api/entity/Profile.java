package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stok kodu (şirket içinde benzersiz)
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfileType type;

    // Görünen yüz genişliği (mm). Seri geometrisi varsayılanları bundan bağımsız tutulur.
    @Column(name = "width_mm")
    private Double widthMm;

    @Column(name = "weight_per_meter")
    private Double weightPerMeter;

    @Column(name = "price_per_meter", nullable = false)
    private BigDecimal pricePerMeter;

    @Column(name = "currency")
    private String currency = "TRY";

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "bar_length_mm")
    private Integer barLengthMm = 6500; // boy uzunluğu (kesim optimizasyonu için)

    // Sadece çıta (BEAD) için: hangi cam kalınlık aralığında kullanılır
    @Column(name = "min_glass_mm")
    private Double minGlassMm;

    @Column(name = "max_glass_mm")
    private Double maxGlassMm;

    // Aynı seri + tip içinde varsayılan profil (kesim listesinde rol -> profil eşlemesi)
    @Column(name = "is_default", nullable = false)
    private boolean defaultForType = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @JsonProperty("seriesId")
    public Long getSeriesId() {
        return series == null ? null : series.getId();
    }
}
