package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import com.kfarms.entity.SourceType;
import lombok.Data;

import java.time.LocalDate;

@Data

public class LivestockRequest {
    private String batchName;
    private int quantity;
    private LivestockType type;
    private LocalDate arrivalDate;

    private SourceType sourceType;       // FARM_BIRTH or SUPPLIER
    private Integer startingAgeInWeeks;  // optional; default 0 for FARM_BIRTH
    private Integer mortality;           // optional; default 0

    private String notes;
}
