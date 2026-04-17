package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Component("pdf")
@RequiredArgsConstructor
public class PdfExporter implements Exporter{

    private final SalesRepository salesRepo;
    private final FeedRepository feedRepo;
    private final EggProductionRepo eggRepo;
    private final SuppliesRepository suppliesRepo;
    private final InventoryRepository inventoryRepo;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishPondRepo;
    private final FishHatchRepository fishHatchRepo;
    private final TenantRepository tenantRepository;

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
        return build(sales, "Sales Report", ReportColumnSets.SALES);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Feed> feeds = feedRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(feed -> start == null || (feed.getDate() != null && !feed.getDate().isBefore(start)))
                .filter(feed -> end == null || (feed.getDate() != null && !feed.getDate().isAfter(end)))
                .toList();
        return build(feeds, "Feeds Report", ReportColumnSets.FEEDS);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<EggProduction> eggs = eggRepo.findForExport(tenantId, start, end);
        return build(eggs, "Egg Production Report", ReportColumnSets.EGGS);
    }

    @Override
    public InputStream exportSupplies(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Supplies> supplies = suppliesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSupplyDate() != null && !item.getSupplyDate().isBefore(start)))
                .filter(item -> end == null || (item.getSupplyDate() != null && !item.getSupplyDate().isAfter(end)))
                .toList();
        return build(supplies, "Supplies Report", ReportColumnSets.SUPPLIES);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Inventory> inventories = inventoryRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getLastUpdated() != null && !item.getLastUpdated().isBefore(start)))
                .filter(item -> end == null || (item.getLastUpdated() != null && !item.getLastUpdated().isAfter(end)))
                .toList();
        return build(inventories, "Inventory Report", ReportColumnSets.INVENTORY);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> livestock = livestockRepo.findAllActive(tenantId).stream()
                .filter(item -> start == null || (item.getArrivalDate() != null && !item.getArrivalDate().isBefore(start)))
                .filter(item -> end == null || (item.getArrivalDate() != null && !item.getArrivalDate().isAfter(end)))
                .toList();
        return build(livestock, "Livestock Report", ReportColumnSets.LIVESTOCK);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishPond> fishPonds = fishPondRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(pond -> start == null || (pond.getDateStocked() != null && !pond.getDateStocked().isBefore(start)))
                .filter(pond -> end == null || (pond.getDateStocked() != null && !pond.getDateStocked().isAfter(end)))
                .toList();
        return build(fishPonds, "Fish Pond Report", ReportColumnSets.FISH_PONDS);
    }

    @Override
    public InputStream exportFishHatches(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishHatch> hatches = fishHatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId).stream()
                .filter(item -> start == null || (item.getHatchDate() != null && !item.getHatchDate().isBefore(start)))
                .filter(item -> end == null || (item.getHatchDate() != null && !item.getHatchDate().isAfter(end)))
                .toList();
        return build(hatches, "Fish Hatch Report", ReportColumnSets.FISH_HATCHES);
    }

    // helper method
    private <T> InputStream build(List<T> data, String title, List<ReportColumn<T>> columns) {
        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("No data available for " + title);
            }
            byte[] pdfBytes = PdfReportBuilder.buildReport(
                    data,
                    title,
                    columns,
                    resolveBranding()
            );
            return new ByteArrayInputStream(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error exporting " + title, e);
        }
    }

    private PdfReportBuilder.ReportBranding resolveBranding() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return null;
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return null;
        }

        String footer = tenant.getReportFooter();
        if (footer == null || footer.isBlank()) {
            footer = "Generated for " + tenant.getName() + " via KFarms";
        }

        return new PdfReportBuilder.ReportBranding(
                tenant.getName(),
                tenant.getBrandPrimaryColor(),
                tenant.getBrandAccentColor(),
                footer
        );
    }
}
