package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.tenant.service.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;


@Component("csv")
@AllArgsConstructor
public class CsvExporter implements Exporter {

    private final SalesRepository salesRepo;
    private final EggProductionRepo eggRepo;
    private final FeedRepository feedRepo;
    private final SuppliesRepository suppliesRepo;
    private final InventoryRepository inventoryRepo;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishRepo;
    private final FishHatchRepository fishHatchRepo;

    @Override
    public InputStream exportSales(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Sales> salesList = salesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(sale -> start == null || (sale.getSalesDate() != null && !sale.getSalesDate().isBefore(start)))
                .filter(sale -> end == null || (sale.getSalesDate() != null && !sale.getSalesDate().isAfter(end)))
                .sorted((left, right) -> {
                    LocalDate leftDate = left.getSalesDate();
                    LocalDate rightDate = right.getSalesDate();
                    if (leftDate == null && rightDate == null) return 0;
                    if (leftDate == null) return 1;
                    if (rightDate == null) return -1;
                    return rightDate.compareTo(leftDate);
                })
                .toList();
        return CsvReportBuilder.buildReport(salesList, ReportColumnSets.SALES);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<EggProduction> list = eggRepo.findForExport(tenantId, start, end);
        return CsvReportBuilder.buildReport(list, ReportColumnSets.EGGS);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Feed> list = feedRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(feed -> start == null || (feed.getDate() != null && !feed.getDate().isBefore(start)))
                .filter(feed -> end == null || (feed.getDate() != null && !feed.getDate().isAfter(end)))
                .sorted((left, right) -> {
                    LocalDate leftDate = left.getDate();
                    LocalDate rightDate = right.getDate();
                    if (leftDate == null && rightDate == null) return 0;
                    if (leftDate == null) return 1;
                    if (rightDate == null) return -1;
                    return rightDate.compareTo(leftDate);
                })
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.FEEDS);
    }

    @Override
    public InputStream exportSupplies(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Supplies> list = suppliesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSupplyDate() != null && !item.getSupplyDate().isBefore(start)))
                .filter(item -> end == null || (item.getSupplyDate() != null && !item.getSupplyDate().isAfter(end)))
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.SUPPLIES);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> list = livestockRepo.findAllActive(tenantId).stream()
                .filter(item -> start == null || (item.getArrivalDate() != null && !item.getArrivalDate().isBefore(start)))
                .filter(item -> end == null || (item.getArrivalDate() != null && !item.getArrivalDate().isAfter(end)))
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.LIVESTOCK);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishPond> list = fishRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(pond -> start == null || (pond.getDateStocked() != null && !pond.getDateStocked().isBefore(start)))
                .filter(pond -> end == null || (pond.getDateStocked() != null && !pond.getDateStocked().isAfter(end)))
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.FISH_PONDS);
    }

    @Override
    public InputStream exportFishHatches(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishHatch> list = fishHatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId).stream()
                .filter(item -> start == null || (item.getHatchDate() != null && !item.getHatchDate().isBefore(start)))
                .filter(item -> end == null || (item.getHatchDate() != null && !item.getHatchDate().isAfter(end)))
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.FISH_HATCHES);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Inventory> list = inventoryRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getLastUpdated() != null && !item.getLastUpdated().isBefore(start)))
                .filter(item -> end == null || (item.getLastUpdated() != null && !item.getLastUpdated().isAfter(end)))
                .sorted((left, right) -> {
                    LocalDate leftDate = left.getLastUpdated();
                    LocalDate rightDate = right.getLastUpdated();
                    if (leftDate == null && rightDate == null) return 0;
                    if (leftDate == null) return 1;
                    if (rightDate == null) return -1;
                    return rightDate.compareTo(leftDate);
                })
                .toList();
        return CsvReportBuilder.buildReport(list, ReportColumnSets.INVENTORY);
    }
}
