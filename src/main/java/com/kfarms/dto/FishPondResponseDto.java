package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FishPondResponseDto {
    private Long id;
    private String pondName;
    private String pondType;
    // STOCK INFO
    private Integer currentStock;
    private Integer capacity;
    private Integer mortalityCount;
    // GROWTH/FEEDING
    private String feedingSchedule;
    // STATUS
    private String status;
    // LOCATION
    private String pondLocation;
    // DATES
    private LocalDate dateStocked;
    private LocalDate lastWaterChange;
    private LocalDate nextWaterChange; // derived - not stored
    // NOTES
    private String note;

    // AUDITABLE
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
