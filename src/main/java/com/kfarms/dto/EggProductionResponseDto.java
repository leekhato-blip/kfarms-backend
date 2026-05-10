package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EggProductionResponseDto {
    private Long id;
    private Long batchId;
    private String batchName;
    private LocalDate collectionDate;
    private int goodEggs;
    private int damagedEggs;
    private int cratesProduced;
    private String note;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
