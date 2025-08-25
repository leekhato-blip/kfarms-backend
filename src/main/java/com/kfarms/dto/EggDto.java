package com.kfarms.dto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
public class EggDto {
    private Long id;
    private Long layerId;
    private LocalDate collectionDate;
    private int quantity;
    private String notes;

    // FROM AUDITABLE
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
