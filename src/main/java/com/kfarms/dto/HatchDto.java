package com.kfarms.dto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HatchDto {
    private Long id;
    private LocalDate hatchDate;
    private int maleCount;
    private int femaleCount;
    private double hatchRate;
    private int quantityHatched;
    private String note;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
