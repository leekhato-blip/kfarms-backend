package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inventory_tenant_item_category",
                        columnNames = {"tenant_id", "item_name", "category"}
                )
        }
)
@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)

public class Inventory extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String itemName;

    @Column(length = 80)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryCategory category; // e.g. FEED, MEDICINE, TOOL

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private int minThreshold = 0;

    @Column(nullable = false)
    private String unit;  // e.g.  kg, bags, litres

    @Column(precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(length = 120)
    private String supplierName;

    @Column(length = 120)
    private String storageLocation;

    private String note; // e.g. reserved for pond 3

    @Column(nullable = false)
    private LocalDate lastUpdated = LocalDate.now(); // default today

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
