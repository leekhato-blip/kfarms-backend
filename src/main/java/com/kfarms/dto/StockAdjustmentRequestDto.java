package com.kfarms.dto;

import com.kfarms.entity.StockAdjustmentReason;
import lombok.Data;

@Data
public class StockAdjustmentRequestDto {
    private int quantity;
    private StockAdjustmentReason reason;
    private String note;
}
