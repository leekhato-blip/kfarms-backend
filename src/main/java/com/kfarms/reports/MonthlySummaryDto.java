package com.kfarms.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlySummaryDto {
    private long totalEggsProduced;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private long totalLivestockCount;
    private long totalFishStock;
}
