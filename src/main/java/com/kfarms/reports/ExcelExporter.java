package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Component("xlsx")
@RequiredArgsConstructor
public class ExcelExporter implements Exporter {

    private final SalesRepository salesRepo;
    private final FeedRepository feedRepo;
    private final EggProductionRepo eggRepo;
    private final SuppliesRepository suppliesRepo;
    private final InventoryRepository inventoryRepo;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishPondRepo;
    private final FishHatchRepository fishHatchRepo;

    @Override
    public InputStream exportSales(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Sales> sales = salesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSalesDate() != null && !item.getSalesDate().isBefore(start)))
                .filter(item -> end == null || (item.getSalesDate() != null && !item.getSalesDate().isAfter(end)))
                .sorted((left, right) -> {
                    LocalDate leftDate = left.getSalesDate();
                    LocalDate rightDate = right.getSalesDate();
                    if (leftDate == null && rightDate == null) return 0;
                    if (leftDate == null) return 1;
                    if (rightDate == null) return -1;
                    return rightDate.compareTo(leftDate);
                })
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(sales, "Sales Report", ReportColumnSets.SALES);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Feed> feeds = feedRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(feed -> start == null || (feed.getDate() != null && !feed.getDate().isBefore(start)))
                .filter(feed -> end == null || (feed.getDate() != null && !feed.getDate().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(feeds, "Feeds Report", ReportColumnSets.FEEDS);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<EggProduction> eggs = eggRepo.findForExport(tenantId, start, end);
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(eggs, "Egg Production Report", ReportColumnSets.EGGS);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportSupplies(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Supplies> supplies = suppliesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSupplyDate() != null && !item.getSupplyDate().isBefore(start)))
                .filter(item -> end == null || (item.getSupplyDate() != null && !item.getSupplyDate().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(supplies, "Supplies Report", ReportColumnSets.SUPPLIES);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Inventory> inventories = inventoryRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getLastUpdated() != null && !item.getLastUpdated().isBefore(start)))
                .filter(item -> end == null || (item.getLastUpdated() != null && !item.getLastUpdated().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(inventories, "Inventory Report", ReportColumnSets.INVENTORY);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> livestock = livestockRepo.findAllActive(tenantId).stream()
                .filter(item -> start == null || (item.getArrivalDate() != null && !item.getArrivalDate().isBefore(start)))
                .filter(item -> end == null || (item.getArrivalDate() != null && !item.getArrivalDate().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(livestock, "Livestock Report", ReportColumnSets.LIVESTOCK);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishPond> fishPonds = fishPondRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(pond -> start == null || (pond.getDateStocked() != null && !pond.getDateStocked().isBefore(start)))
                .filter(pond -> end == null || (pond.getDateStocked() != null && !pond.getDateStocked().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(fishPonds, "Fish Pond Report", ReportColumnSets.FISH_PONDS);
        return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportFishHatches(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishHatch> hatches = fishHatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId).stream()
                .filter(item -> start == null || (item.getHatchDate() != null && !item.getHatchDate().isBefore(start)))
                .filter(item -> end == null || (item.getHatchDate() != null && !item.getHatchDate().isAfter(end)))
                .toList();
        byte[] excelBytes = ExcelReportBuilder.buildWorkbook(hatches, "Fish Hatch Report", ReportColumnSets.FISH_HATCHES);
        return new ByteArrayInputStream(excelBytes);
    }
}
