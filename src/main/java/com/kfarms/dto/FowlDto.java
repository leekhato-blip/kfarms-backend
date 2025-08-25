package com.kfarms.dto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FowlDto {
    private Long id;
    private String batchName;
    private int quantity;
    private LocalDate arrivalDate;
    private int ageInWeeks;
    private String notes;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
