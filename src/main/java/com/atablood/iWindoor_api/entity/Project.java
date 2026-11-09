package com.atablood.iWindoor_api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    @Column(name = "customer_address", length = 500)
    private String customerAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectStatus status = ProjectStatus.QUOTE;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_total", precision = 14, scale = 2)
    private BigDecimal approvedTotal;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_version_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private QuoteVersion approvedVersion;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "sale_total", precision = 14, scale = 2)
    private BigDecimal saleTotal;

    @Column(name = "currency")
    private String currency = "TRY";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WindowUnit> windowUnits = new java.util.ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User createdBy;

    /** Yanıt için doldurulur (kalıcı değil). */
    @Transient
    private BigDecimal paidTotal;

    @Transient
    private BigDecimal balance;

    @Transient
    private Boolean priceChangedAfterApproval;

    @Transient
    private BigDecimal priceDelta;

    @JsonProperty("customerId")
    public Long getCustomerId() {
        return customer == null ? null : customer.getId();
    }

    @JsonProperty("approvedVersionId")
    public Long getApprovedVersionId() {
        return approvedVersion == null ? null : approvedVersion.getId();
    }
}
