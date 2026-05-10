package com.kfarms.reports;

import com.kfarms.repository.*;
import com.kfarms.tenant.service.TenantContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService{

    private final SalesRepository salesRepo;
    private final EggProductionRepo eggRepo;
    private final FeedRepository feedRepo;
    private final InventoryRepository inventoryRepo;
    private final ExportFactory exportFactory;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishRepo;
    private final SuppliesRepository suppliesRepo;

    @Override
    public MonthlySummaryDto getMonthlySummary(LocalDate month) {
        LocalDate m = month == null ? LocalDate.now().withDayOfMonth(1) : month.withDayOfMonth(1);
        LocalDate start = m;
        LocalDate end = m.plusMonths(1).minusDays(1);

        // Use repository aggregate queries to avoid loading all rows
        Long tenantId = requireTenantId();
        long eggs = eggRepo.sumEggsBetween(tenantId, start, end);
        BigDecimal revenue = salesRepo.sumTotalBetween(tenantId, start, end);
        BigDecimal expenses = suppliesRepo.sumSupplyCostBetween(tenantId, start, end);
        long livestock = livestockRepo.countAllActiveLivestock(tenantId);
        long fishPond = fishRepo.countTotalFishStock(tenantId);
        return new MonthlySummaryDto(eggs, revenue, expenses, livestock, fishPond);
    }

    @Override
    public MonthlySummaryDto getRangeSummary(LocalDate startDate, LocalDate endDate) {
        Long tenantId = requireTenantId();
        // similar, just use startDate/endDate
        long eggs = eggRepo.sumEggsBetween(tenantId, startDate, endDate);
        BigDecimal revenue = salesRepo.sumTotalBetween(tenantId, startDate, endDate);
        BigDecimal expenses = computeExpensesBetween(tenantId, startDate, endDate);
        long livestock = livestockRepo.countAllActiveLivestock(tenantId);
        long fish = fishRepo.countTotalFishStock(tenantId);
        return new MonthlySummaryDto(eggs, revenue, expenses, livestock, fish);
    }

    @Override
    public List<TrendPointDto> getTrends(String metric, LocalDate startDate, LocalDate endDate, String interval) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }

        String normalized = (metric == null) ? "" : metric.toLowerCase().trim();
        Long tenantId = requireTenantId();

        return switch (normalized) {
            case "eggs", "eggproduction", "egg_production" ->
                    eggRepo.findDailyEggsBetween(tenantId, startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "sales" ->
                    salesRepo.findDailySalesBetween(tenantId, startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "feed", "feedusage", "feed_usage" ->
                    feedRepo.findDailyFeedUsageBetween(tenantId, startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "expenses" ->
                    suppliesRepo.findDailySuppliesBetween(tenantId, startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
        };
    }

    @Override
    public ExportResponseMeta validateExportParams(String type, String category, LocalDate start, LocalDate end) {
        String normalizedType = normalizeType(type);
        String normalizedCategory = normalizeCategory(category);

        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date");
        }

        String filename = normalizedCategory + "_" + resolveRangeLabel(start, end) + "." + normalizedType;
        String contentType = switch (normalizedType) {
            case "csv" -> "text/csv";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> throw new IllegalArgumentException("Unsupported export type");
        };
        return new ExportResponseMeta(filename, contentType);
    }

    @Override
    public InputStreamSource generateExport(String type, String category, LocalDate start, LocalDate end) {
        try{
            String normalizedType = normalizeType(type);
            String normalizedCategory = normalizeCategory(category);
            InputStream in;
            var exporter = exportFactory.getExporter(normalizedType);

            switch (normalizedCategory) {
                case "sales" -> in = exporter.exportSales(start, end);
                case "eggs" -> in = exporter.exportEggProduction(start, end);
                case "feeds" -> in = exporter.exportFeedUsage(start, end);
                case "supplies" -> in = exporter.exportSupplies(start, end);
                case "inventory" -> in = exporter.exportInventory(start, end);
                case "livestock" -> in = exporter.exportLivestock(start, end);
                case "fish" -> in = exporter.exportFishPond(start, end);
                case "hatches" -> in = exporter.exportFishHatches(start, end);
                default -> throw new IllegalArgumentException("Unsupported export category: " + category);
            }
            byte[] bytes = in.readAllBytes();
            return new ByteArrayResource(bytes);
        } catch (Exception e) {
            log.error("Export failed for {} [{} - {}]: {}", category, start, end, e.getMessage(), e);
            throw new RuntimeException("Export failed. Please check your parameters or data availability.");
        }
    }

    // helper method
    private BigDecimal computeExpensesBetween(Long tenantId, LocalDate start, LocalDate end) {
        BigDecimal supplies = suppliesRepo.sumSupplyCostBetween(tenantId, start, end);

        log.info("SUPPLIES={}", supplies);


        return Stream.of(supplies)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "csv" -> "csv";
            case "xlsx", "excel" -> "xlsx";
            case "pdf" -> "pdf";
            default -> throw new IllegalArgumentException("Unsupported export type: " + type);
        };
    }

    private String normalizeCategory(String category) {
        String normalized = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "sales", "sale" -> "sales";
            case "eggs", "eggproduction", "egg_production", "production", "productions" -> "eggs";
            case "feed", "feeds", "feedusage", "feed_usage" -> "feeds";
            case "supplies", "supply", "purchase", "purchases" -> "supplies";
            case "inventory", "stock" -> "inventory";
            case "livestock", "poultry" -> "livestock";
            case "fish", "pond", "ponds", "fishpond", "fish_pond", "fish-ponds" -> "fish";
            case "hatch", "hatches", "fishhatches", "fish_hatches", "fish-hatches" -> "hatches";
            default -> throw new IllegalArgumentException("Unsupported export category: " + category);
        };
    }

    private String resolveRangeLabel(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "all-time";
        }

        String from = start == null ? "start" : start.toString();
        String to = end == null ? "today" : end.toString();
        return from + "_to_" + to;
    }
}
