package com.kfarms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InventoryAdjustmentRequestDto {

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    private String note;
}
