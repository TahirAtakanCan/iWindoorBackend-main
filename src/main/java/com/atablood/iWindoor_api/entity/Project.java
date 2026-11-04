package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Örn: "Ahmet Bey - Yayla Evi"

    private String description;

    // Proje toplam tutarı (Hesaplama sonucu güncellenir)
    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "currency")
    private String currency = "TRY";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Bir projenin birden fazla penceresi/kapısı olur.
    // CascadeType.ALL: Projeyi silersem içindeki tüm pencereler de silinsin.
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WindowUnit> windowUnits = new java.util.ArrayList<>();

    // Kullanıcı (şifre özeti dahil) JSON'a asla yazılmaz
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User createdBy;
}
