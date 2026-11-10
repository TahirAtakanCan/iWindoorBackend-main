package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Profil sistemi (seri). Çizim motoru ve kesim hesabı bu kayıttaki geometri değerlerini kullanır.
 * companyId == null olan kayıtlar şablondur; her şirket kayıt olurken şablonun kopyasını alır.
 */
@Data
@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Örn: "70 Seri"

    private String brand;

    @Column(name = "system_depth_mm")
    private Integer systemDepth; // Sistem derinliği (60, 70, 76...)

    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false, length = 16)
    private String material = "PVC"; // PVC | ALU

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // --- Geometri (mm) : Dart tarafındaki SeriesGeometry ile birebir ---
    @Column(name = "frame_face_mm", nullable = false)
    private Double frameFaceMm = 62.0;          // kasa görünen yüzü

    @Column(name = "sash_face_mm", nullable = false)
    private Double sashFaceMm = 78.0;           // kanat görünen yüzü

    @Column(name = "mullion_face_mm", nullable = false)
    private Double mullionFaceMm = 80.0;        // orta kayıt görünen yüzü

    @Column(name = "bead_face_mm", nullable = false)
    private Double beadFaceMm = 20.0;           // cam çıtası görünen yüzü

    @Column(name = "sash_lap_mm", nullable = false)
    private Double sashLapMm = 8.0;             // kanadın kasaya bindirmesi

    @Column(name = "glass_rebate_frame_mm", nullable = false)
    private Double glassRebateFrameMm = 14.0;   // cam payı (kasa kenarı)

    @Column(name = "glass_rebate_sash_mm", nullable = false)
    private Double glassRebateSashMm = 14.0;    // cam payı (kanat kenarı)

    @Column(name = "glass_rebate_mullion_mm", nullable = false)
    private Double glassRebateMullionMm = 14.0; // cam payı (kayıt kenarı)

    @Column(name = "weld_allowance_mm", nullable = false)
    private Double weldAllowanceMm = 3.0;       // kaynak payı (uç başına, ALU için 0)

    @Column(name = "mullion_end_allowance_mm", nullable = false)
    private Double mullionEndAllowanceMm = 3.0; // kayıt uç payı

    @Column(name = "slide_interlock_mm", nullable = false)
    private Double slideInterlockMm = 40.0;     // sürme kanat bindirmesi

    @Column(name = "duplex_width_mm", nullable = false)
    private Double duplexWidthMm = 18.0;        // dubleks çıta genişliği

    // --- Kanat / cam sınırları (mobil editör uyarıları) ---
    @Column(name = "min_sash_width_mm", nullable = false)
    private Double minSashWidthMm = 350.0;

    @Column(name = "min_sash_height_mm", nullable = false)
    private Double minSashHeightMm = 350.0;

    @Column(name = "max_sash_width_mm", nullable = false)
    private Double maxSashWidthMm = 1400.0;

    @Column(name = "max_sash_height_mm", nullable = false)
    private Double maxSashHeightMm = 2400.0;

    @Column(name = "max_slide_sash_width_mm", nullable = false)
    private Double maxSlideSashWidthMm = 2000.0;

    @Column(name = "max_lift_slide_sash_width_mm", nullable = false)
    private Double maxLiftSlideSashWidthMm = 3000.0;

    @Column(name = "max_fixed_glass_area_m2", nullable = false)
    private Double maxFixedGlassAreaM2 = 6.0;

    // --- Adaptör / kapı / eşik ---
    @Column(name = "adapter_face_mm", nullable = false)
    private Double adapterFaceMm = 40.0;

    @Column(name = "door_sash_face_mm", nullable = false)
    private Double doorSashFaceMm = 110.0;

    @Column(name = "threshold_face_mm", nullable = false)
    private Double thresholdFaceMm = 20.0;

    @Column(name = "threshold_end_allowance_mm", nullable = false)
    private Double thresholdEndAllowanceMm = 0.0;

    @Column(name = "min_door_sash_width_mm", nullable = false)
    private Double minDoorSashWidthMm = 600.0;

    @Column(name = "max_door_sash_width_mm", nullable = false)
    private Double maxDoorSashWidthMm = 1100.0;

    @Column(name = "min_door_sash_height_mm", nullable = false)
    private Double minDoorSashHeightMm = 1800.0;

    @Column(name = "max_door_sash_height_mm", nullable = false)
    private Double maxDoorSashHeightMm = 2500.0;

    @Column(name = "coupling_face_mm", nullable = false)
    private Double couplingFaceMm = 20.0;

    @Column(name = "coupling_gap_mm", nullable = false)
    private Double couplingGapMm = 0.0;

    /** PER_M2 modunda kullanılan satış birim fiyatı (₺/m²); null ise COST_PLUS'a düşülür. */
    @Column(name = "sale_price_per_m2", precision = 12, scale = 2)
    private java.math.BigDecimal salePricePerM2;
}
