package com.kfarms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EggProductionResponseDto {
    private Long id;
    private Long livestockId;
    private String batchName;
    private LocalDate collectionDate;
    private Integer goodEggs;
    private Integer damagedEggs;
    private double cratesProduced;
    private String note;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
