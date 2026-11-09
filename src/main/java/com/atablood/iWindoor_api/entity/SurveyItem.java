package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "survey_items")
public class SurveyItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Project project;

    @Column(nullable = false, length = 120)
    private String room;

    @Column(name = "product_type", nullable = false, length = 16)
    private String productType;

    @Column(name = "hole_width", nullable = false, precision = 8, scale = 1)
    private BigDecimal holeWidth;

    @Column(name = "hole_height", nullable = false, precision = 8, scale = 1)
    private BigDecimal holeHeight;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "window_unit_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private WindowUnit windowUnit;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /** Çevrimdışı istemci kimliği; (project_id, client_id) kısmi unique. */
    @Column(name = "client_id", length = 64)
    private String clientId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "surveyItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<SurveyPhoto> photos = new ArrayList<>();
}
