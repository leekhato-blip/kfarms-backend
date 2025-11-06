package com.kfarms.reports;

import com.kfarms.repository.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
        long eggs = eggRepo.sumEggsBetween(start, end);
        BigDecimal revenue = salesRepo.sumTotalBetween(start, end);
        BigDecimal expenses = suppliesRepo.sumSupplyCostBetween(start, end);
        long livestock = livestockRepo.countAllActiveLivestock();
        long fishPond = fishRepo.countTotalFishStock();
        return new MonthlySummaryDto(eggs, revenue, expenses, livestock, fishPond);
    }

    @Override
    public MonthlySummaryDto getRangeSummary(LocalDate startDate, LocalDate endDate) {
        // similar, just use startDate/endDate
        long eggs = eggRepo.sumEggsBetween(startDate, endDate);
        BigDecimal revenue = salesRepo.sumTotalBetween(startDate, endDate);
        BigDecimal expenses = computeExpensesBetween(startDate, endDate);
        long livestock = livestockRepo.countAllActiveLivestock();
        long fish = fishRepo.countTotalFishStock();
        return new MonthlySummaryDto(eggs, revenue, expenses, livestock, fish);
    }

    @Override
    public List<TrendPointDto> getTrends(String metric, LocalDate startDate, LocalDate endDate, String interval) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }

        String normalized = (metric == null) ? "" : metric.toLowerCase().trim();

        return switch (normalized) {
            case "eggs", "eggproduction", "egg_production" ->
                    eggRepo.findDailyEggsBetween(startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "sales" ->
                    salesRepo.findDailySalesBetween(startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "feed", "feedusage", "feed_usage" ->
                    feedRepo.findDailyFeedUsageBetween(startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            case "expenses" ->
                    suppliesRepo.findDailySuppliesBetween(startDate, endDate)
                            .stream()
                            .map(r -> new TrendPointDto((LocalDate) r[0],
                                    r[1] == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(r[1]))))
                            .collect(Collectors.toList());

            default -> throw new IllegalArgumentException("Unsupported metric: " + metric);
        };
    }

    @Override
    public ExportResponseMeta validateExportParams(String type, String category, LocalDate start, LocalDate end) {
        // validate type and category, build filename and contentType
        String filename = String.format("%s_%s_%s.%s", category, start, end, type.equals("xlsx") ? "xlsx" : type);
        String contentType = switch (type.toLowerCase()) {
            case "csv" -> "text/csv";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> throw new IllegalArgumentException("Unsupported export type");
        };
        return new ExportResponseMeta(filename, contentType);
    }

    @Override
    public InputStreamSource generateExport(String type, String category, LocalDate start, LocalDate end) {
        // fetch data in streaming/chunks depending on category
        try{
            InputStream in;
            var exporter = exportFactory.getExporter(type);

            switch (category.toLowerCase()) {
                case "sales" -> in = exporter.exportSales(start, end);
                case "eggs", "eggProduction", "egg_production" -> in = exporter.exportEggProduction(start, end);
                case "feed", "feedUsage", "feed_usage" -> in = exporter.exportFeedUsage(start, end);
                case "inventory" -> in = exporter.exportInventory(start, end);
                case "livestock" -> in = exporter.exportLivestock(start, end);
                case "fish", "fishpond", "fish_pond" -> in = exporter.exportFishPond(start, end);
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
    private BigDecimal computeExpensesBetween(LocalDate start, LocalDate end) {
        BigDecimal supplies = suppliesRepo.sumSupplyCostBetween(start, end);

        log.info("SUPPLIES={}", supplies);


        return Stream.of(supplies)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
