package com.kfarms.dto;


import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class DashboardSummaryDto {
    private int totalLivestockCount;
    private int totalFishStock;
    private int totalFeeds; // total feed types
    private double totalFeedQuantity; // sum of quantities used
    private int totalEggsProduced;
    private int totalEggsProducedThisMonth;
    private BigDecimal totalRevenue;
    private BigDecimal totalRevenueThisMonth;
    private BigDecimal totalExpenses;
    private BigDecimal totalExpensesThisMonth;
    private Map<String, Object> alerts;
}
