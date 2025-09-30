package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "FishPond")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FishPond extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @Column(nullable = false, unique = true)
    private String pondName;  // e.g. "Pond 1"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FishPondType pondType; // HATCHING, GROW_OUT, BROODSTOCK, HOLDING

    // STOCK INFO
    private Integer currentStock; // number of fish currently in pond
    private Integer capacity; // maximum number of fish
    private Integer mortalityCount = 0; // number of deaths recorded (total)

    // GROWTH/FEEDING
    @Enumerated(EnumType.STRING)
    private FishFeedingSchedule feedingSchedule = FishFeedingSchedule.BOTH; // e.g., MORNING, EVENING OR BOTH - defaults to both

    // STATUS
    @Enumerated(EnumType.STRING)
    private FishPondStatus status = FishPondStatus.ACTIVE; // ACTIVE, EMPTY, MAINTENANCE - defaults to ACTIVE

    // LOCATION
    private FishPondLocation pondLocation;

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

}
