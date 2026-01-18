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

@Component("pdf")
@RequiredArgsConstructor
public class PdfExporter implements Exporter{

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
    private static final String[] LIVESTOCK_HEADERS = {"Batch Type", "Batch Type", "Quantity", "Status"};
    private static final String[] FISHPOND_HEADERS = {"Pond Name", "Fish Count", "Status", "Created Date"};

    @Override
    public InputStream exportSales(LocalDate start, LocalDate end) {
        List<Sales> sales = salesRepo.findBySalesDateBetween(start, end);
        return build(sales, "Sales Report", SALES_HEADERS);
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        List<Feed> feeds = feedRepo.findAllByDateBetween(start, end);
        return build(feeds, "Feeds Report", FEED_HEADERS);
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        List<EggProduction> eggs = eggRepo.findAllByCollectionDateBetween(start, end);
        return build(eggs, "Egg Production Report", EGG_HEADERS);
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        List<Inventory> inventories = inventoryRepo.findAll();
        return build(inventories, "Inventory Report", INVENTORY_HEADERS);
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        List<Livestock> livestock = livestockRepo.findAllByArrivalDateBetween(start, end);
        return build(livestock, "Livestock Report", LIVESTOCK_HEADERS);
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        List<FishPond> fishPonds = fishPondRepo.findAllByDateStockedBetween(start, end);
        return build(fishPonds, "Fish Pond Report", FISHPOND_HEADERS);
    }

    // helper method
    private <T> InputStream build(List<T> data, String title, String[] headers) {
        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("No data available for " + title);
            }
            byte[] pdfBytes = PdfReportBuilder.buildReport(data, title, Arrays.asList(headers));
            return new ByteArrayInputStream(pdfBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error exporting " + title, e);
        }
    }
}
