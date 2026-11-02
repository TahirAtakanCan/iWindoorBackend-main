package com.atablood.iWindoor_api.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data // Getter, Setter, toString, equals hepsini otomatik yapar
@Entity
@Table(name = "series")
public class Series {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Örn: "60'lık Eko Seri"

    private String brand; // Örn: "Fırat", "Ege" vb.

    @Column(name = "system_depth_mm")
    private Integer systemDepth; // Sistem derinliği (Örn: 60mm, 70mm)

    // Bu seriye ait profilleri listelemek istersek (Opsiyonel ama kullanışlı)
    // @OneToMany(mappedBy = "series")
    // private List<Profile> profiles;
}
