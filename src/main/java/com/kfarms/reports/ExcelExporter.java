package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component("xlsx")
@RequiredArgsConstructor
public class ExcelExporter implements Exporter {

    private final SalesRepository salesRepo;
    private final FeedRepository feedRepo;
    private final EggProductionRepo eggRepo;
    private final InventoryRepository inventoryRepo;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishPondRepo;

    private static final String[] SALES_HEADERS = {"Date", "Product", "Quantity", "Unit Price", "Total Price"};
    private static final String[] FEED_HEADERS = {"Feed Name", "Quantity Used", "Date"};
    private static final String[] EGG_HEADERS = {"Date", "Good Eggs", "Cracked Eggs", "Crates"};
    private static final String[] INVENTORY_HEADERS = {"Items", "Type", "Quantity", "Last Updated"};
    private static final String[] LIVESTOCK_HEADERS = {"Batch Type", "Quantity", "Status", "Created At"};
    private static final String[] FISHPOND_HEADERS = {"Pond Name", "Fish Count", "Status", "Created Date"};

    @Override
    public InputStream exportSales(LocalDate start, LocalDate end) {
        List<Sales> sales = salesRepo.findBySalesDateBetween(start, end);

        List<String> headers = Arrays.asList(SALES_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(sales, "Sales Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        List<Feed> feeds = feedRepo.findAllByDateBetween(start, end);

        List<String> headers = Arrays.asList(FEED_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(feeds, "Feeds Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        List<EggProduction> eggs = eggRepo.findAllByCollectionDateBetween(start, end);

        List<String> headers = Arrays.asList(EGG_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(eggs, "Egg Production Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        List<Inventory> inventories = inventoryRepo.findAll();

        List<String> headers = Arrays.asList(INVENTORY_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(inventories, "Inventory Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        List<Livestock> livestock = livestockRepo.findAllByArrivalDateBetween(start, end);

        List<String> headers = Arrays.asList(LIVESTOCK_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(livestock, "Livestock Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        List<FishPond> fishPonds = fishPondRepo.findAllByDateStockedBetween(start, end);

        List<String> headers = Arrays.asList(FISHPOND_HEADERS);
            byte[] excelBytes = ExcelReportBuilder.buildWorkbook(fishPonds, "Fish Pond Report", headers);
            return new ByteArrayInputStream(excelBytes);
    }
}
