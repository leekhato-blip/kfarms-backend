package com.kfarms.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// PURCHASES
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "supplies")
@RequiredArgsConstructor
public class Supplies extends Auditable {
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String itemName; // e.g., "Layer Feed", Vaccine

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplyCategory category; // FEED, LIVESTOCK, FISH, MEDICINE, EQUIPMENT, OTHER

    @Column(nullable = false)
    private Integer quantity;


    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal totalPrice; // auto = quantity + unitPrice

    private String supplierName;
    private String note;

    @Column(nullable = false)
    private LocalDate supplyDate = LocalDate.now(); // default today

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
