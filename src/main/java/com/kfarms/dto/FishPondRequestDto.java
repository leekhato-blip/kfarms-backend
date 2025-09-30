package com.kfarms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FishPondRequestDto {

    private String pondName;
    private String pondType;
    // STOCK INFO
    private Integer currentStock;
    private Integer capacity;
    private Integer mortalityCount;
    // GROWTH/FEEDING
    private String feedingSchedule;
    private String status;
    // LOCATION
    private String pondLocation;
    // DATES
    private LocalDate dateStocked;
    private LocalDate lastWaterChange;
    // NOTES
    private String note;
}
