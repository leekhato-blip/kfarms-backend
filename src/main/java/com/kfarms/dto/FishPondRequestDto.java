package com.kfarms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FishPondRequestDto {

    @NotBlank(message = "Pond name is required")
    @Size(max = 50, message = "Pond name cannot exceed 50 characters")
    private String pondName;

    @NotBlank(message = "Pond type is required")
    private String pondType;

    // Nullable for partial updates
    @Min(value = 0, message = "Current stock cannot be negative")
    private Integer currentStock;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Min(value = 0, message = "Mortality count cannot be negative")
    private Integer mortalityCount;

    @Size(max = 255, message = "Feeding schedule cannot exceed 255 characters")
    private String feedingSchedule;

    @Size(max = 50, message = "Status cannot exceed 50 characters")
    private String status;

    @Size(max = 100, message = "Pond location cannot exceed 100 characters")
    private String pondLocation;

    @PastOrPresent(message = "Date stocked cannot be in the future")
    private LocalDate dateStocked;

    @PastOrPresent(message = "Last water change cannot be in the future")
    private LocalDate lastWaterChange;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
