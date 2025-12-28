package com.kfarms.dto;

import com.kfarms.entity.StockAdjustmentReason;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockAdjustmentRequestDto {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Reason is required")
    private StockAdjustmentReason reason;

    @Size(max = 255, message = "Note cannot exceed 255 characters")
    private String note;
}
