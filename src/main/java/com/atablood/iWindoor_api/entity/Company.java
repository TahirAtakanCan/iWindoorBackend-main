package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "companies")
public class Company {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Şirket Adı (Örn: ABC Pencere Sistemleri)
    private String taxNumber;   // Vergi No (Opsiyonel)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(name = "tax_office", length = 120)
    private String taxOffice;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("20.00");

    @Column(name = "default_discount_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultDiscountPct = BigDecimal.ZERO.setScale(2);

    @Column(nullable = false, length = 8)
    private String currency = "TRY";

    @Column(name = "quote_note", length = 2000)
    private String quoteNote;

    @Column(name = "quote_validity_days", nullable = false)
    private Integer quoteValidityDays = 15;

    /** Testere kalınlığı (mm), kesim başına. */
    @Column(name = "saw_kerf_mm", nullable = false, precision = 5, scale = 1)
    private BigDecimal sawKerfMm = new BigDecimal("4.0");

    /** Boyun her iki ucundan atılan baş/son payı (mm). */
    @Column(name = "bar_trim_mm", nullable = false, precision = 5, scale = 1)
    private BigDecimal barTrimMm = new BigDecimal("10.0");

    /** Bu uzunluktan (mm) kısa artık fire sayılır. */
    @Column(name = "min_offcut_mm", nullable = false)
    private Integer minOffcutMm = 500;

    @Column(name = "glass_supplier_name", length = 200)
    private String glassSupplierName;

    @Column(name = "glass_supplier_phone", length = 50)
    private String glassSupplierPhone;

    @Column(name = "glass_supplier_email", length = 200)
    private String glassSupplierEmail;

    @Column(name = "glass_order_note", length = 1000)
    private String glassOrderNote;

    /** COST_PLUS | PER_M2 */
    @Column(name = "pricing_mode", nullable = false, length = 16)
    private String pricingMode = "COST_PLUS";

    @Column(name = "profit_margin_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal profitMarginPct = new BigDecimal("30.00");

    @Column(name = "labor_per_m2", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborPerM2 = new BigDecimal("400.00");

    @Column(name = "install_per_m2", nullable = false, precision = 12, scale = 2)
    private BigDecimal installPerM2 = new BigDecimal("250.00");

    @Column(name = "min_billable_area_m2", nullable = false, precision = 6, scale = 2)
    private BigDecimal minBillableAreaM2 = new BigDecimal("1.00");

    // Şirkete bağlı çalışanlar
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<User> users = new ArrayList<>();
}
