package com.kfarms.entity;


import com.kfarms.tenant.entity.Tenant;
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
                @UniqueConstraint(name = "uk_livestock_tenant_batch", columnNames = {"tenant_id", "batch_name"})
        }
)
@Data
@RequiredArgsConstructor
public class Livestock extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "batch_name", nullable = false)
    private String batchName;

    private Integer currentStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LivestockType type;

    private LocalDate arrivalDate = LocalDate.now();

    @Column(nullable = true)
    private String note;

    private int startingAgeInWeeks;

    private Integer mortality;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    private PoultryKeepingMethod keepingMethod;

    @Transient
    public int getAgeInWeeks() {
        int base = (sourceType == SourceType.FARM_BIRTH) ? 0 : Math.max(0, startingAgeInWeeks);
        int sinceArrival = (arrivalDate != null)
                ? (int) ChronoUnit.WEEKS.between(arrivalDate, LocalDate.now())
                : 0;
        return base + Math.max(0, sinceArrival);
    }

    public void adjustStock(int quantity, StockAdjustmentReason reason) {
        if (quantity == 0 || reason == null) return;
        if (this.currentStock == null) this.currentStock = 0;

        switch (reason) {
            case PURCHASE, TRANSFER_IN, OTHER -> this.currentStock += quantity;
            case SALE, CONSUMPTION, TRANSFER_OUT -> this.currentStock = Math.max(this.currentStock - quantity, 0);
            default -> throw new IllegalArgumentException("Unknown stock adjustment reason: " + reason);
        }
    }
}
