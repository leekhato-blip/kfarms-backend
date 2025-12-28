package com.kfarms.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


@Entity
@Table(
        name = "livestock",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"batchName"})
        }
)
@Data
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Livestock extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String batchName;
    private Integer currentStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LivestockType type; // LAYER, DUCK, FOWL, etc.

    private LocalDate arrivalDate = LocalDate.now();

    @Column(nullable = true) // explicitly nullable
    private String note;

    // Initial age when arriving (weeks). 0 for FARM_BIRTH
    private int startingAgeInWeeks;

    // Deaths recorded for this batch
    private Integer mortality;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType; // FARM_BIRTH or SUPPLIER

    // Not stored in DB - derived field
    @Transient
    public int getAgeInWeeks(){
        int base = (sourceType == SourceType.FARM_BIRTH) ? 0 : Math.max(0, startingAgeInWeeks);
        int sinceArrival = (arrivalDate != null)
                ? (int) ChronoUnit.WEEKS.between(arrivalDate, LocalDate.now())
                : 0;
        return base + Math.max(0, sinceArrival);
    }

    // adjust stock logic
    public void adjustStock(int quantity, StockAdjustmentReason reason) {
        if (quantity == 0 || reason == null) return;

        if (this.currentStock == null) this.currentStock = 0;

        switch (reason) {
            case PURCHASE, TRANSFER_IN, OTHER -> this.currentStock += quantity;
            case SALE, CONSUMPTION, TRANSFER_OUT -> {
                int newStock = this.currentStock - quantity;
                this.currentStock = Math.max(newStock, 0);
            }
            default -> throw new IllegalArgumentException("Unknown stock adjustment reason: " + reason);
        }
    }
}
