package com.kfarms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FeedResponseDto {
    private Long id;
    private String batchType; // LAYER, FISH
    private String type;
    private Long batchId;
    private String feedName;
    private String name;
    private String itemName;
    private Integer quantityUsed;
    private Integer quantity;
    private BigDecimal unitCost;
    private String note;
    private LocalDate date;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
