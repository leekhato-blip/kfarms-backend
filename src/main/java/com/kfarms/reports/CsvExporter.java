package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import com.kfarms.tenant.service.TenantContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;


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
        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit Price,Total Price,Buyer,Note\n");
        for (Sales s : salesList) {
            appendCsvRow(sb,
                    s.getSalesDate(),
                    s.getItemName(),
                    s.getCategory(),
                    s.getQuantity(),
                    s.getUnitPrice(),
                    s.getTotalPrice(),
                    s.getBuyer(),
                    s.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<EggProduction> list = eggRepo.findForExport(tenantId, start, end);
        StringBuilder sb = new StringBuilder("Date,Batch,Good Eggs,Damaged Eggs,Crates Produced,Notes\n");
        for (EggProduction e : list) {
            appendCsvRow(sb,
                    e.getCollectionDate(),
                    e.getLivestock() != null ? e.getLivestock().getBatchName() : "",
                    e.getGoodEggs(),
                    e.getDamagedEggs(),
                    e.getCratesProduced(),
                    e.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
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

        StringBuilder sb = new StringBuilder("Date,Batch Type,Feed Name,Quantity,Unit Cost,Note\n");
        for (Feed feed : list) {
            appendCsvRow(sb,
                    feed.getDate(),
                    feed.getBatchType(),
                    feed.getFeedName(),
                    feed.getQuantityUsed(),
                    feed.getUnitCost(),
                    feed.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportSupplies(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Supplies> list = suppliesRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(item -> start == null || (item.getSupplyDate() != null && !item.getSupplyDate().isBefore(start)))
                .filter(item -> end == null || (item.getSupplyDate() != null && !item.getSupplyDate().isAfter(end)))
                .toList();

        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit Price,Total Price,Supplier,Note\n");
        for (Supplies item : list) {
            appendCsvRow(sb,
                    item.getSupplyDate(),
                    item.getItemName(),
                    item.getCategory(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice(),
                    item.getSupplierName(),
                    item.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> list = livestockRepo.findAllActive(tenantId).stream()
                .filter(item -> start == null || (item.getArrivalDate() != null && !item.getArrivalDate().isBefore(start)))
                .filter(item -> end == null || (item.getArrivalDate() != null && !item.getArrivalDate().isAfter(end)))
                .toList();
        StringBuilder sb = new StringBuilder("Batch,Type,Current Stock,ArrivalDate,Source,Starting Age (Weeks),Mortality\n");
        for (Livestock l : list){
            appendCsvRow(sb,
                    l.getBatchName(),
                    l.getType(),
                    l.getCurrentStock(),
                    l.getArrivalDate(),
                    l.getSourceType(),
                    l.getStartingAgeInWeeks(),
                    l.getMortality());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishPond> list = fishRepo.findAllActiveByTenantId(tenantId).stream()
                .filter(pond -> start == null || (pond.getDateStocked() != null && !pond.getDateStocked().isBefore(start)))
                .filter(pond -> end == null || (pond.getDateStocked() != null && !pond.getDateStocked().isAfter(end)))
                .toList();
        StringBuilder sb = new StringBuilder("Pond Name,Type,Current Stock,Capacity,Mortality,Feeding Schedule,Status,Location,Date Stocked,Last Water Change,Next Water Change,Note\n");
        for (FishPond f : list) {
            appendCsvRow(sb,
                    f.getPondName(),
                    f.getPondType(),
                    f.getCurrentStock(),
                    f.getCapacity(),
                    f.getMortalityCount(),
                    f.getFeedingSchedule(),
                    f.getStatus(),
                    f.getPondLocation(),
                    f.getDateStocked(),
                    f.getLastWaterChange(),
                    f.getNextWaterChange(),
                    f.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportFishHatches(LocalDate start, LocalDate end) {
        Long tenantId = TenantContext.getTenantId();
        List<FishHatch> list = fishHatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId).stream()
                .filter(item -> start == null || (item.getHatchDate() != null && !item.getHatchDate().isBefore(start)))
                .filter(item -> end == null || (item.getHatchDate() != null && !item.getHatchDate().isAfter(end)))
                .toList();

        StringBuilder sb = new StringBuilder("Hatch Date,Pond,Quantity Hatched,Hatch Rate,Male Count,Female Count,Note\n");
        for (FishHatch hatch : list) {
            appendCsvRow(sb,
                    hatch.getHatchDate(),
                    hatch.getPond() != null ? hatch.getPond().getPondName() : "",
                    hatch.getQuantityHatched(),
                    hatch.getHatchRate(),
                    hatch.getMaleCount(),
                    hatch.getFemaleCount(),
                    hatch.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
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
        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit,Note\n");
        for (Inventory i : list) {
            appendCsvRow(sb,
                    i.getLastUpdated(),
                    i.getItemName(),
                    i.getCategory(),
                    i.getQuantity(),
                    i.getUnit(),
                    i.getNote());
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void appendCsvRow(StringBuilder sb, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                sb.append(',');
            }
            sb.append(csvValue(values[index]));
        }
        sb.append('\n');
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value).replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return '"' + text + '"';
        }
        return text;
    }
}
