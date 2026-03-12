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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    private static final String[] SALES_HEADERS = {"Date", "Product", "Quantity", "Unit Price", "Total Price"};
    private static final String[] FEED_HEADERS = {"Date", "Batch Type", "Feed Name", "Quantity Used", "Unit Cost", "Note"};
    private static final String[] EGG_HEADERS = {"Date", "Batch", "Good Eggs", "Cracked Eggs", "Crates", "Note"};
    private static final String[] SUPPLIES_HEADERS = {"Date", "Item", "Category", "Quantity", "Unit Price", "Total Price", "Supplier", "Note"};
    private static final String[] INVENTORY_HEADERS = {"Items", "Type", "Quantity", "Last Updated"};
    private static final String[] LIVESTOCK_HEADERS = {"Batch Type", "Batch Type", "Quantity", "Status"};
    private static final String[] FISHPOND_HEADERS = {"Pond Name", "Fish Count", "Status", "Created Date"};
    private static final String[] HATCH_HEADERS = {"Hatch Date", "Pond", "Quantity Hatched", "Hatch Rate", "Male Count", "Female Count", "Note"};

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
        return build(sales, "Sales Report", SALES_HEADERS);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FeedExportRow> feeds = feedRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(feed -> start == null || (feed.getDate() != null && !feed.getDate().isBefore(start)))
                .filter(feed -> end == null || (feed.getDate() != null && !feed.getDate().isAfter(end)))
                .map(feed -> new FeedExportRow(
                        feed.getDate(),
                        feed.getBatchType() != null ? feed.getBatchType().name() : "UNKNOWN",
                        feed.getFeedName(),
                        feed.getQuantityUsed(),
                        feed.getUnitCost(),
                        feed.getNote()
                ))
                .toList();
        return build(feeds, "Feeds Report", FEED_HEADERS);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<EggExportRow> eggs = eggRepo.findForExport(tenantId, start, end).stream()
                .map(egg -> new EggExportRow(
                        egg.getCollectionDate(),
                        egg.getLivestock() != null ? egg.getLivestock().getBatchName() : null,
                        egg.getGoodEggs(),
                        egg.getDamagedEggs(),
                        egg.getCratesProduced(),
                        egg.getNote()
                ))
                .toList();
        return build(eggs, "Egg Production Report", EGG_HEADERS);
    }

    @Override
    public InputStream exportSupplies(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<SupplyExportRow> supplies = suppliesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSupplyDate() != null && !item.getSupplyDate().isBefore(start)))
                .filter(item -> end == null || (item.getSupplyDate() != null && !item.getSupplyDate().isAfter(end)))
                .map(item -> new SupplyExportRow(
                        item.getSupplyDate(),
                        item.getItemName(),
                        item.getCategory() != null ? item.getCategory().name() : null,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice(),
                        item.getSupplierName(),
                        item.getNote()
                ))
                .toList();
        return build(supplies, "Supplies Report", SUPPLIES_HEADERS);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Inventory> inventories = inventoryRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getLastUpdated() != null && !item.getLastUpdated().isBefore(start)))
                .filter(item -> end == null || (item.getLastUpdated() != null && !item.getLastUpdated().isAfter(end)))
                .toList();
        return build(inventories, "Inventory Report", INVENTORY_HEADERS);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> livestock = livestockRepo.findAllActive(tenantId).stream()
                .filter(item -> start == null || (item.getArrivalDate() != null && !item.getArrivalDate().isBefore(start)))
                .filter(item -> end == null || (item.getArrivalDate() != null && !item.getArrivalDate().isAfter(end)))
                .toList();
        return build(livestock, "Livestock Report", LIVESTOCK_HEADERS);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishPond> fishPonds = fishPondRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(pond -> start == null || (pond.getDateStocked() != null && !pond.getDateStocked().isBefore(start)))
                .filter(pond -> end == null || (pond.getDateStocked() != null && !pond.getDateStocked().isAfter(end)))
                .toList();
        return build(fishPonds, "Fish Pond Report", FISHPOND_HEADERS);
    }

    @Override
    public InputStream exportFishHatches(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishHatchExportRow> hatches = fishHatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId).stream()
                .filter(item -> start == null || (item.getHatchDate() != null && !item.getHatchDate().isBefore(start)))
                .filter(item -> end == null || (item.getHatchDate() != null && !item.getHatchDate().isAfter(end)))
                .map(item -> new FishHatchExportRow(
                        item.getHatchDate(),
                        item.getPond() != null ? item.getPond().getPondName() : null,
                        item.getQuantityHatched(),
                        item.getHatchRate(),
                        item.getMaleCount(),
                        item.getFemaleCount(),
                        item.getNote()
                ))
                .toList();
        return build(hatches, "Fish Hatch Report", HATCH_HEADERS);
    }

    private static final class SupplyExportRow {
        private final LocalDate date;
        private final String item;
        private final String category;
        private final Integer quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal totalPrice;
        private final String supplier;
        private final String note;

        private SupplyExportRow(LocalDate date, String item, String category, Integer quantity, BigDecimal unitPrice,
                                BigDecimal totalPrice, String supplier, String note) {
            this.date = date;
            this.item = item;
            this.category = category;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
            this.supplier = supplier;
            this.note = note;
        }
    }

    // helper method
    private <T> InputStream build(List<T> data, String title, String[] headers) {
        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("No data available for " + title);
            }
            byte[] pdfBytes = PdfReportBuilder.buildReport(
                    data,
                    title,
                    Arrays.asList(headers),
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

    private static final class FeedExportRow {
        private final LocalDate date;
        private final String batchType;
        private final String feedName;
        private final Integer quantityUsed;
        private final BigDecimal unitCost;
        private final String note;

        private FeedExportRow(LocalDate date, String batchType, String feedName, Integer quantityUsed, BigDecimal unitCost, String note) {
            this.date = date;
            this.batchType = batchType;
            this.feedName = feedName;
            this.quantityUsed = quantityUsed;
            this.unitCost = unitCost;
            this.note = note;
        }
    }

    private static final class EggExportRow {
        private final LocalDate date;
        private final String batch;
        private final Integer goodEggs;
        private final Integer crackedEggs;
        private final Integer crates;
        private final String note;

        private EggExportRow(LocalDate date, String batch, Integer goodEggs, Integer crackedEggs, Integer crates, String note) {
            this.date = date;
            this.batch = batch;
            this.goodEggs = goodEggs;
            this.crackedEggs = crackedEggs;
            this.crates = crates;
            this.note = note;
        }
    }

    private static final class FishHatchExportRow {
        private final LocalDate hatchDate;
        private final String pond;
        private final Integer quantityHatched;
        private final Double hatchRate;
        private final Integer maleCount;
        private final Integer femaleCount;
        private final String note;

        private FishHatchExportRow(LocalDate hatchDate, String pond, Integer quantityHatched, Double hatchRate,
                                   Integer maleCount, Integer femaleCount, String note) {
            this.hatchDate = hatchDate;
            this.pond = pond;
            this.quantityHatched = quantityHatched;
            this.hatchRate = hatchRate;
            this.maleCount = maleCount;
            this.femaleCount = femaleCount;
            this.note = note;
        }
    }
}
