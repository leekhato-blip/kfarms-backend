package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import com.kfarms.entity.PoultryKeepingMethod;
import com.kfarms.entity.SourceType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LivestockResponseDto {
    private Long id;
    private String batchName;
    private Integer currentStock;
    private LivestockType type;
    private LocalDate arrivalDate;

    private SourceType sourceType;
    private int startingAgeInWeeks;
    private int ageInWeeks; // derived, not stored
    private Integer mortality;
    private PoultryKeepingMethod keepingMethod;
    private Integer mortalityThisWeek;
    private Integer mortalityThisMonth;
    private LocalDate lastMortalityDate;

    private String note;

    // audit
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
