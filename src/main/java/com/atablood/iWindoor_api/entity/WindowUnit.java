package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString; // Lombok kullanıyorsan bunu ekle
import java.math.BigDecimal;

@Data // Lombok geri döndü
@Entity
@Table(name = "window_units")
public class WindowUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double width;
    private Double height;

    private Integer quantity = 1;

    private BigDecimal unitPrice;

    // --- KRİTİK DÜZELTME BURADA ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore // <--- JSON oluştururken bu alanı YOK SAY (Döngüyü kırar)
    @ToString.Exclude // <--- LOMBOK İÇİN: Log basarken sonsuz döngüye girmesin
    private Project project;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "root_node_id")
    private WindowNode rootNode;
}