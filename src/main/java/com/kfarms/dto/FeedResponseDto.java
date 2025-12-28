package com.kfarms.dto;

import com.kfarms.entity.LivestockType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FeedResponseDto {
    private Long id;
    private String batchType; // LAYER, FISH
    private Long batchId;
    private String feedName;
    private Integer quantityUsed;
    private String note;
    private LocalDate date;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
