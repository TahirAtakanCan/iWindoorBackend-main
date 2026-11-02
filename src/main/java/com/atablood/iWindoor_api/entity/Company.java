package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    // Şirkete bağlı çalışanlar
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<User> users = new ArrayList<>();
}