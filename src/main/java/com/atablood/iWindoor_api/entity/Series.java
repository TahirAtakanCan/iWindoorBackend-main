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
}
