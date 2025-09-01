package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import com.kfarms.entity.SourceType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LivestockResponse {
    private Long id;
    private String batchName;
    private int quantity;
    private LivestockType type;
    private LocalDate arrivalDate;

    private SourceType sourceType;
    private int startingAgeInWeeks;
    private int ageInWeeks; // derived, not stored
    private Integer mortality;

    private String notes;

    // audit
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
