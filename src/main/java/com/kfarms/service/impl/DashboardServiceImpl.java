package com.kfarms.service.impl;


import com.kfarms.dto.DashboardSummaryDto;
import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.service.DashboardService;
import com.kfarms.service.EggProductionService;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
//@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final LivestockRepository livestockRepo;
    private final FeedRepository feedRepo;
    private final EggProductionRepo eggRepo;
    private final SalesRepository salesRepo;
    private final SuppliesRepository suppliesRepo;
    private final FishPondRepository fishPondRepo;
    private final InventoryRepository inventoryRepo;
    private final NotificationService notificationService; // keep in case autowiring triggers something
    private final EggProductionService eggProductionService;

    @Override
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();
        Long tenantId = TenantContext.getTenantId();

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // ================= LIVESTOCK =================
        try {
            List<Livestock> livestockList = livestockRepo.findAllActive(tenantId);

            int totalLivestockCount = livestockList.stream()
                    .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                    .sum();
            summary.setTotalLivestockCount(totalLivestockCount);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading LIVESTOCK section", e);
            summary.setTotalLivestockCount(0);
        }

        // ================= FISH =================
        int totalFishStock = 0;
        int totalPondCount = 0;
        try {
            List<FishPond> ponds = fishPondRepo.findAllActiveByTenantId(tenantId);

            totalPondCount = ponds.size();
            summary.setTotalPondCount(totalPondCount);

            totalFishStock = ponds.stream()
                    .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                    .sum();
            summary.setTotalFishStock(totalFishStock);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading FISH section", e);
            summary.setTotalPondCount(0);
            summary.setTotalFishStock(0);
        }

        // ================= FEED =================
        int totalFeedQuantity = 0;
        try {
            List<Feed> feeds = feedRepo.findAllActiveByTenantId(tenantId);
            summary.setTotalFeeds(feeds.size());

            totalFeedQuantity = inventoryRepo.findAllActiveByTenantId(tenantId).stream()
                    .filter(i -> i.getCategory() == InventoryCategory.FEED)
                    .mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                    .sum();
            summary.setTotalFeedQuantity(totalFeedQuantity);

            // Feed breakdown (safe)
            Map<String, Integer> quantityByBatch = new HashMap<>();
            feeds.forEach(feed -> {
                String type = feed.getBatchType() != null ? feed.getBatchType().name() : "UNKNOWN";
                int qty = feed.getQuantityUsed() != null ? feed.getQuantityUsed() : 0;
                quantityByBatch.merge(type, qty, Integer::sum);
            });

            int grandTotalFeedUsed = quantityByBatch.values().stream().mapToInt(Integer::intValue).sum();

            List<Map<String, Object>> feedBreakdown = quantityByBatch.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("label", e.getKey());
                        m.put("value", e.getValue());
                        double percent = grandTotalFeedUsed == 0 ? 0 :
                                (e.getValue() * 100.0 / grandTotalFeedUsed);
                        m.put("percent", Math.round(percent * 10.0) / 10.0);
                        return m;
                    }).toList();
            summary.setFeedBreakdown(feedBreakdown);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading FEED section", e);
            summary.setTotalFeeds(0);
            summary.setTotalFeedQuantity(0);
            summary.setFeedBreakdown(List.of());
        }

        // ================= EGGS =================
        try {
            List<EggProduction> eggs = eggRepo.findAllActiveVisibleToTenant(tenantId);

            Map<String, Object> eggSummary = null;
            try {
                eggSummary = eggProductionService.getSummary();
            } catch (Exception ex) {
                log.error("[DASHBOARD] Error inside EggProductionService.getSummary()", ex);
            }

            Map<String, Integer> monthlyProduction =
                    eggSummary != null
                            ? (Map<String, Integer>) eggSummary.getOrDefault("MonthlyProduction", new HashMap<>())
                            : new HashMap<>();
            summary.setMonthlyProduction(monthlyProduction);

            int totalGoodEggs = eggs.stream()
                    .mapToInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0)
                    .sum();

            int totalCratesProduced = totalGoodEggs / 30;
            summary.setTotalCratesProduced(totalCratesProduced);

            int totalCratesProducedToday = eggs.stream()
                    .filter(e -> e.getCollectionDate() != null)
                    .filter(e -> e.getCollectionDate().isEqual(now))
                    .mapToInt(e -> e.getCratesProduced() != 0 ? e.getCratesProduced() : 0)
                    .sum();
            summary.setTotalCratesProducedToday(totalCratesProducedToday);

            int totalCratesProducedThisMonth = eggs.stream()
                    .filter(e -> e.getCollectionDate() != null)
                    .filter(e -> e.getCollectionDate().getMonthValue() == month &&
                            e.getCollectionDate().getYear() == year)
                    .mapToInt(e -> e.getCratesProduced() != 0 ? e.getCratesProduced() : 0)
                    .sum();
            summary.setTotalCratesProducedThisMonth(totalCratesProducedThisMonth);

            int totalEggsProducedThisMonth = eggs.stream()
                    .filter(e -> e.getCollectionDate() != null)
                    .filter(e -> e.getCollectionDate().getMonthValue() == month &&
                            e.getCollectionDate().getYear() == year)
                    .mapToInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0)
                    .sum();
            summary.setTotalEggsProducedThisMonth(totalEggsProducedThisMonth);

        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading EGGS section", e);
            summary.setMonthlyProduction(new HashMap<>());
            summary.setTotalCratesProduced(0);
            summary.setTotalCratesProducedToday(0);
            summary.setTotalCratesProducedThisMonth(0);
            summary.setTotalEggsProducedThisMonth(0);
        }

        // ================= SALES =================
        try {
            List<Sales> sales = salesRepo.findAllActiveByTenantId(tenantId);

            BigDecimal totalRevenue = sales.stream()
                    .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalRevenue(totalRevenue);

            BigDecimal totalRevenueThisMonth = sales.stream()
                    .filter(s -> s.getSalesDate() != null)
                    .filter(s -> s.getSalesDate().getMonthValue() == month &&
                            s.getSalesDate().getYear() == year)
                    .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalRevenueThisMonth(totalRevenueThisMonth);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading SALES section", e);
            summary.setTotalRevenue(BigDecimal.ZERO);
            summary.setTotalRevenueThisMonth(BigDecimal.ZERO);
        }

        // ================= EXPENSES =================
        BigDecimal totalExpensesThisMonth = BigDecimal.ZERO;
        try {
            List<Supplies> supplies = suppliesRepo.findAllActiveByTenantId(tenantId);

            BigDecimal totalExpenses = supplies.stream()
                    .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalExpenses(totalExpenses);

            totalExpensesThisMonth = supplies.stream()
                    .filter(s -> s.getSupplyDate() != null)
                    .filter(s -> s.getSupplyDate().getMonthValue() == month &&
                            s.getSupplyDate().getYear() == year)
                    .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalExpensesThisMonth(totalExpensesThisMonth);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while loading EXPENSES section", e);
            summary.setTotalExpenses(BigDecimal.ZERO);
            summary.setTotalExpensesThisMonth(BigDecimal.ZERO);
        }

        // ================= ALERTS =================
        try {
            Map<String, Object> alerts = new HashMap<>();

            if (summary.getTotalFeedQuantity() < 5) {
                alerts.put("feedLow", "Feed stock is running low");
            }

            if (summary.getTotalFishStock() < 100) {
                alerts.put("fishLow", "Fish stock is low");
            }

            if (summary.getTotalExpensesThisMonth().compareTo(summary.getTotalRevenueThisMonth()) > 0) {
                alerts.put("profitWarning", "Expenses exceed revenue this month");
            }

            summary.setAlerts(alerts);
        } catch (Exception e) {
            log.error("[DASHBOARD] Error while creating ALERTS", e);
            summary.setAlerts(new HashMap<>());
        }

        return summary;
    }
}
