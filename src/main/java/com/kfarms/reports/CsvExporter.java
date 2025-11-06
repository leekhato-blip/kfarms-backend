package com.kfarms.reports;

import com.kfarms.entity.*;
import com.kfarms.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;


@Component("csv")
@AllArgsConstructor
public class CsvExporter implements Exporter {

    private final SalesRepository salesRepo;
    private final EggProductionRepo eggRepo;
    private final FeedRepository feedRepo;
    private final InventoryRepository inventoryRepo;
    private final LivestockRepository livestockRepo;
    private final FishPondRepository fishRepo;

    @Override
    public InputStream exportSales(LocalDate start, LocalDate end) {
        List<Sales> salesList = salesRepo.findBySalesDateBetween(start, end);
        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit Price,Total Price,Buyer,Note\n");
        for (Sales s : salesList) {
            sb.append(s.getSalesDate()).append(",");
            sb.append(s.getItemName()).append(",");
            sb.append(s.getCategory()).append(",");
            sb.append(s.getQuantity()).append(",");
            sb.append(s.getUnitPrice()).append(",");
            sb.append(s.getTotalPrice()).append(",");
            sb.append(s.getBuyer()).append(",");
            sb.append(s.getNote()).append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportEggProduction(LocalDate start, LocalDate end) {
        List<EggProduction> list = eggRepo.findByCollectionDateBetween(start, end);
        StringBuilder sb = new StringBuilder("Date,Batch,Good Eggs,Damaged Eggs,Crates Produced,Notes\n");
        for (EggProduction e : list) {
            sb.append(e.getCollectionDate()).append(",");
            sb.append(e.getLivestock()).append(",");
            sb.append(e.getGoodEggs()).append(",");
            sb.append(e.getDamagedEggs()).append(",");
            sb.append(e.getCratesProduced()).append(",");
            sb.append(e.getNote()).append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportFeedUsage(LocalDate start, LocalDate end) {
        List<Inventory> list = inventoryRepo.findByLastUpdatedBetween(start, end);
        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit,Note\n");
        for (Inventory i : list) {
            if ("FEED".equalsIgnoreCase(i.getCategory().name())) {
                sb.append(i.getLastUpdated()).append(",");
                sb.append(i.getItemName()).append(",");
                sb.append(i.getCategory()).append(",");
                sb.append(i.getQuantity()).append(",");
                sb.append(i.getUnit()).append(",");
                sb.append(i.getNote()).append("\n");
            }
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportLivestock(LocalDate start, LocalDate end) {
        List<Livestock> list = livestockRepo.findAllActive();
        StringBuilder sb = new StringBuilder("Batch,Type,Current Stock,ArrivalDate,Source,Starting Age (Weeks),Mortality\n");
        for (Livestock l : list){
            sb.append(l.getBatchName()).append(",");
            sb.append(l.getType()).append(",");
            sb.append(l.getCurrentStock()).append(",");
            sb.append(l.getArrivalDate()).append(",");
            sb.append(l.getSourceType()).append(",");
            sb.append(l.getStartingAgeInWeeks()).append(",");
            sb.append(l.getMortality()).append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportFishPond(LocalDate start, LocalDate end) {
        List<FishPond> list = fishRepo.findByDateStockedBetween(start, end);
        StringBuilder sb = new StringBuilder("Pond Name,Type,Current Stock,Capacity,Mortality,Feeding Schedule,Status,Location,Date Stocked,Last Water Change,Next Water Change,Note\n");
        for (FishPond f : list) {
            sb.append(f.getPondName()).append(",");
            sb.append(f.getPondType()).append(",");
            sb.append(f.getCurrentStock()).append(",");
            sb.append(f.getCapacity()).append(",");
            sb.append(f.getMortalityCount()).append(",");
            sb.append(f.getFeedingSchedule()).append(",");
            sb.append(f.getStatus()).append(",");
            sb.append(f.getPondLocation()).append(",");
            sb.append(f.getDateStocked()).append(",");
            sb.append(f.getLastWaterChange()).append(",");
            sb.append(f.getNextWaterChange()).append(",");
            sb.append(f.getNote()).append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream exportInventory(LocalDate start, LocalDate end) {
        List<Inventory> list = inventoryRepo.findByLastUpdatedBetween(start, end);
        StringBuilder sb = new StringBuilder("Date,Item,Category,Quantity,Unit,Note\n");
        for (Inventory i : list) {
            sb.append(i.getLastUpdated()).append(",");
            sb.append(i.getItemName()).append(",");
            sb.append(i.getCategory()).append(",");
            sb.append(i.getQuantity()).append(",");
            sb.append(i.getUnit()).append(",");
            sb.append(i.getNote()).append("\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
