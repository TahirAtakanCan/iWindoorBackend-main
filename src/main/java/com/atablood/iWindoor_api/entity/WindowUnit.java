package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    /** Eski (sunucu tarafı) düğüm ağacı. Yeni istemciler {@link #design} kullanır. */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "root_node_id")
    private WindowNode rootNode;

    /** Tasarım belgesi (JSON). null ise tasarım eski düğüm ağacından türetilir. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "design", columnDefinition = "jsonb")
    private Map<String, Object> design;

    @Column(name = "design_version", nullable = false)
    private Integer designVersion = 1;

    /** İstemcinin çizim motorunda hesapladığı kesim listesi (fiyatlandırma girdisi). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bom_snapshot", columnDefinition = "jsonb")
    private List<Map<String, Object>> bomSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Series series;

    /**
     * İyimser kilit: mobil ve masaüstü aynı pencereyi aynı anda düzenlerse ikincisi 409 alır.
     * Başlangıç değeri VERİLMEZ: Spring Data null sürümü "yeni kayıt" sayar; 0L verilirse merge ile çift kayıt oluşur.
     */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("seriesId")
    public Long getSeriesId() {
        return series == null ? null : series.getId();
    }
}
