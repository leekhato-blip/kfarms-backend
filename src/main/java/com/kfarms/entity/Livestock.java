package com.kfarms.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Entity
@Table(name = "livestock")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Livestock extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchName;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private LivestockType type; // LAYER, DUCK, FOWL, etc.

    private LocalDate arrivalDate;
    private String notes;

    // Initial age when arriving (weeks). 0 for FARM_BIRTH
    private int startingAgeInWeeks;

    // Deaths recorded for this batch
    private int mortality;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType; // FARM_BIRTH or SUPPLIER

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Not stored in DB - derived field
    @Transient
    public int getAgeInWeeks(){
        int base = (sourceType == SourceType.FARM_BIRTH) ? 0 : Math.max(0, startingAgeInWeeks);
        int sinceArrival = (arrivalDate != null)
                ? (int) ChronoUnit.WEEKS.between(arrivalDate, LocalDate.now())
                : 0;
        return base + Math.max(0, sinceArrival);
    }

}
