package com.kfarms.dto;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FishHatchResponseDto {
    private Long id;
    private LocalDate hatchDate;
    private int maleCount;
    private int femaleCount;
    private int quantityHatched;
    private double hatchRate;
    private String note;

    // pond info
    private Long pondId;
    private String pondName;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
