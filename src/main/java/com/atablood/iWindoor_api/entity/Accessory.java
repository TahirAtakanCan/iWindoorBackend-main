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
    private String name; // Örn: "Pencere kolu", "EPDM conta"

    private String code; // Stok kodu (şirket içinde benzersiz)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessoryUnit unit; // PIECE, METER, SET

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "currency")
    private String currency = "TRY";

    @Column(name = "company_id")
    private Long companyId;
}
