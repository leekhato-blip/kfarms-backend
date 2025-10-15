package com.kfarms.service.impl;

import com.kfarms.dto.DashboardSummaryDto;
import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.service.DashboardService;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final LivestockRepository livestockRepo;
    private final FeedRepository feedRepo;
    private final EggProductionRepo eggRepo;
    private final SalesRepository salesRepo;
    private final SuppliesRepository suppliesRepo;
    private final FishPondRepository fishPondRepo;
    private final InventoryRepository inventoryRepo;

    // Notification service
    private final NotificationService notificationService;

    @Override
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        // ==== LIVESTOCK ====
        List<Livestock> livestockList = livestockRepo.findAll()
                .stream()
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .toList();

        int totalLivestockCount = livestockList.stream()
                .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                .sum();
        summary.setTotalLivestockCount(totalLivestockCount);

        // ==== FISH STOCK ====
        List<FishPond> ponds = fishPondRepo.findAll()
                .stream()
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .toList();

        int totalFishStock = ponds.stream()
                .mapToInt(p -> p.getCurrentStock() != null ? p.getCurrentStock() : 0)
                .sum();
        summary.setTotalFishStock(totalFishStock);

        // ==== FEED ====
        List<Feed> feeds = feedRepo.findAll()
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .toList();
        summary.setTotalFeeds(feeds.size());

        double totalFeedQuantity = inventoryRepo.findAll().stream()
                .filter(i -> !Boolean.TRUE.equals(i.getDeleted()) &&
                        i.getCategory() != null &&
                        i.getCategory() == InventoryCategory.FEED)
                .mapToDouble(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                .sum();
        summary.setTotalFeedQuantity(totalFeedQuantity);

        // ==== EGG PRODUCTION ====
        List<EggProduction> eggs = eggRepo.findAll()
                .stream()
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .toList();

        double totalCratesProduced = eggs.stream()
                .mapToDouble(EggProduction::getCratesProduced)
                .sum();

        double totalCratesProducedThisMonth = eggs.stream()
                .filter(e -> e.getCollectionDate().getMonthValue() == month &&
                        e.getCollectionDate().getYear() == year)
                .mapToDouble(e -> e.getCratesProduced() != 0.0 ? e.getCratesProduced() : 0.0)
                .sum();
        summary.setTotalCratesProduced(totalCratesProduced);
        summary.setTotalCratesProducedThisMonth(totalCratesProducedThisMonth);

        // ==== SALES / REVENUE ====
        List<Sales> sales = salesRepo.findAll()
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .toList();
        BigDecimal totalRevenue = sales.stream()
                .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenueThisMonth = sales.stream()
                .filter(s -> s.getSalesDate() != null &&
                        s.getSalesDate().getMonthValue() == month &&
                        s.getSalesDate().getYear() == year)
                .map(s -> s.getTotalPrice() != null ? s.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalRevenue(totalRevenue);
        summary.setTotalRevenueThisMonth(totalRevenueThisMonth);

        // ==== EXPENSES ====
        List<Supplies> supplies = suppliesRepo.findAll()
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .toList();
        BigDecimal totalExpenses = supplies.stream()
                .map(s -> s.getUnitPrice() != null ? s.getUnitPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpensesThisMonth = supplies.stream()
                .filter(s -> s.getSupplyDate().getMonthValue() == month &&
                        s.getSupplyDate().getYear() == year)
                .map(s -> s.getUnitPrice() != null ? s.getUnitPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalExpenses(totalExpenses);
        summary.setTotalExpensesThisMonth(totalExpensesThisMonth);

        // ==== ALERT ====
        Map<String, Object> alerts = new HashMap<>();
        if (totalFeedQuantity < 5) {
            alerts.put("feedLow", "Feed stock is running low");
            notificationService.createNotification("FEED", "Feed Low", "Feed stock is running low");
        }
        if (totalFishStock < 100) {
            alerts.put("fishLow", "Fish stock is low");
        }
        if (totalExpensesThisMonth.compareTo(totalRevenueThisMonth) > 0) {
            alerts.put("profitWarning", "Expenses exceed revenue this month");
        }
        summary.setAlerts(alerts);
        return summary;
    }
}
