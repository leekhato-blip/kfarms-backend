package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "fish_pond",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fish_pond_tenant_name",
                        columnNames = {"tenant_id", "pond_name"}
                )
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class FishPond extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false)
    private String pondName;  // e.g. "Pond 1"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FishPondType pondType; // HATCHING, GROW_OUT, BROODSTOCK, HOLDING

    // STOCK INFO
    private Integer currentStock = 0; // number of fish currently in pond
    private Integer capacity = 0; // maximum number of fish
    private Integer mortalityCount = 0; // number of deaths recorded (total)

    // GROWTH/FEEDING
    @Enumerated(EnumType.STRING)
    private FishFeedingSchedule feedingSchedule = FishFeedingSchedule.BOTH; // e.g., MORNING, EVENING OR BOTH - defaults to both

    // STATUS
    @Enumerated(EnumType.STRING)
    private FishPondStatus status = FishPondStatus.ACTIVE; // ACTIVE, EMPTY, MAINTENANCE - defaults to ACTIVE

    // LOCATION
    private FishPondLocation pondLocation = FishPondLocation.FARM;

    // DATES
    private LocalDate dateStocked = LocalDate.now(); // when fish were added
    private LocalDate lastWaterChange; // last date water was changed (maintenance tracking)

    // NOTES
    private String note; // free notes for observation

    @ManyToOne
    @JoinColumn(name = "hatch_id")
    private FishHatch hatchBatch;

    // 🔹 Not persisted: calculate dynamically in service
    @Transient
    private LocalDate nextWaterChange;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

}
