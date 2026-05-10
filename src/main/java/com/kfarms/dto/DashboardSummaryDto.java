package com.kfarms.dto;


import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class DashboardSummaryDto {
    private int totalLivestockCount;
    private int totalFishStock;
    private int totalFeeds; // total feed types
    private int totalFeedQuantity; // sum of quantities used
    private List<Map<String, Object>> feedBreakdown;
    private int totalCratesProduced;
    private int totalPondCount;
    private int totalCratesProducedToday;
    private int totalCratesProducedThisMonth;
    private int totalEggsProducedThisMonth;
    private Map<String, Integer> monthlyProduction; // for chart
    private BigDecimal totalRevenue;
    private BigDecimal totalRevenueThisMonth;
    private BigDecimal totalExpenses;
    private BigDecimal totalExpensesThisMonth;
    private Map<String, Object> alerts;
}
