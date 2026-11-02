package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

// @Data kullanırken recursive yapılarda StackOverflow hatası alabilirsin (toString döngüsü).
// O yüzden Getter/Setter ayrı kullanmak daha güvenlidir.
@Getter
@Setter
@Entity
@Table(name = "window_nodes")
public class WindowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NodeType nodeType;

    // --- RECURSIVE RELATIONSHIP (AĞAÇ YAPISI) ---

    // --- RECURSIVE DÜZELTME ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore // <--- JSON çıktısında üst düğüme gitme (Döngü kırma 2)
    @ToString.Exclude // <--- Lombok ToString döngüsünü engelle
    private WindowNode parent;

    // Çocuklar (Alt parçalar)
    // Örn: Bir Kasa ikiye bölünürse, children listesinde 2 eleman olur (Sol boşluk, Sağ boşluk)
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC") // Parçaların sırası önemli (Sol->Sağ veya Üst->Alt)
    private List<WindowNode> children = new ArrayList<>();

    private Integer itemOrder; // 0, 1, 2... (Sıralama indisi)

    // --- GEOMETRİ ---
    // Bu parçanın genişliği ve yüksekliği (Hesaplamalarla bulunur)
    private Double width;
    private Double height;

    // --- MATERYAL BAĞLANTILARI ---

    // Eğer bu düğüm bir Kasa, Kanat veya Kayıt ise hangi PROFIL kullanıldı?
    @ManyToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    // Eğer bu düğüm bir Cam ise hangi CAM kullanıldı?
    @ManyToOne
    @JoinColumn(name = "glass_id")
    private Glass glass;

    // Helper method: Çocuk eklemeyi kolaylaştırır
    public void addChild(WindowNode child) {
        children.add(child);
        child.setParent(this);
    }

    private java.math.BigDecimal storedPrice;

    // WindowNode sınıfının içine, en alta ekledim. Lombok hatasını giderdik:
    public void setParent(WindowNode parent) {
        this.parent = parent;
    }
}
