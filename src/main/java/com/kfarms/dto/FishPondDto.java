package com.kfarms.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FishPondDto {
    private Long id;
    private String name;  // e.g. "Pond 1"
    private int quantity;
    private int capacity;
    private LocalDate lastWaterChangeDate;
    private String status; // ACTIVE, EMPTY, MAINTENANCE

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
