package com.kfarms.reports;

import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.Sales;
import com.kfarms.entity.Supplies;

import java.util.List;

public final class ReportColumnSets {

    public static final List<ReportColumn<Sales>> SALES = List.of(
            ReportColumn.of("Date", Sales::getSalesDate),
            ReportColumn.of("Item", Sales::getItemName),
            ReportColumn.of("Category", sale -> text(sale.getCategory())),
            ReportColumn.of("Quantity", Sales::getQuantity),
            ReportColumn.of("Unit Price", Sales::getUnitPrice),
            ReportColumn.of("Total Price", Sales::getTotalPrice),
            ReportColumn.of("Buyer", Sales::getBuyer),
            ReportColumn.of("Note", Sales::getNote)
    );

    public static final List<ReportColumn<Feed>> FEEDS = List.of(
            ReportColumn.of("Date", Feed::getDate),
            ReportColumn.of("Batch Type", feed -> text(feed.getBatchType())),
            ReportColumn.of("Feed Name", Feed::getFeedName),
            ReportColumn.of("Quantity Used", Feed::getQuantityUsed),
            ReportColumn.of("Unit Cost", Feed::getUnitCost),
            ReportColumn.of("Note", Feed::getNote)
    );

    public static final List<ReportColumn<EggProduction>> EGGS = List.of(
            ReportColumn.of("Date", EggProduction::getCollectionDate),
            ReportColumn.of("Batch", egg -> egg.getLivestock() != null ? egg.getLivestock().getBatchName() : ""),
            ReportColumn.of("Good Eggs", EggProduction::getGoodEggs),
            ReportColumn.of("Cracked Eggs", EggProduction::getDamagedEggs),
            ReportColumn.of("Crates", EggProduction::getCratesProduced),
            ReportColumn.of("Note", EggProduction::getNote)
    );

    public static final List<ReportColumn<Supplies>> SUPPLIES = List.of(
            ReportColumn.of("Date", Supplies::getSupplyDate),
            ReportColumn.of("Item", Supplies::getItemName),
            ReportColumn.of("Category", item -> text(item.getCategory())),
            ReportColumn.of("Quantity", Supplies::getQuantity),
            ReportColumn.of("Unit Price", Supplies::getUnitPrice),
            ReportColumn.of("Total Price", Supplies::getTotalPrice),
            ReportColumn.of("Supplier", Supplies::getSupplierName),
            ReportColumn.of("Note", Supplies::getNote)
    );

    public static final List<ReportColumn<Inventory>> INVENTORY = List.of(
            ReportColumn.of("Item", Inventory::getItemName),
            ReportColumn.of("Category", item -> text(item.getCategory())),
            ReportColumn.of("Quantity", Inventory::getQuantity),
            ReportColumn.of("Unit", item -> text(item.getUnit())),
            ReportColumn.of("Updated", Inventory::getLastUpdated),
            ReportColumn.of("Note", Inventory::getNote)
    );

    public static final List<ReportColumn<Livestock>> LIVESTOCK = List.of(
            ReportColumn.of("Flock", Livestock::getBatchName),
            ReportColumn.of("Type", item -> text(item.getType())),
            ReportColumn.of("Alive", Livestock::getCurrentStock),
            ReportColumn.of("Start Date", Livestock::getArrivalDate),
            ReportColumn.of("Source", item -> text(item.getSourceType())),
            ReportColumn.of("Starting Age (Weeks)", Livestock::getStartingAgeInWeeks),
            ReportColumn.of("Mortality", Livestock::getMortality)
    );

    public static final List<ReportColumn<FishPond>> FISH_PONDS = List.of(
            ReportColumn.of("Pond", FishPond::getPondName),
            ReportColumn.of("Type", item -> text(item.getPondType())),
            ReportColumn.of("Current Stock", FishPond::getCurrentStock),
            ReportColumn.of("Capacity", FishPond::getCapacity),
            ReportColumn.of("Mortality", FishPond::getMortalityCount),
            ReportColumn.of("Feeding Schedule", FishPond::getFeedingSchedule),
            ReportColumn.of("Status", item -> text(item.getStatus())),
            ReportColumn.of("Location", FishPond::getPondLocation),
            ReportColumn.of("Date Stocked", FishPond::getDateStocked),
            ReportColumn.of("Last Water Change", FishPond::getLastWaterChange),
            ReportColumn.of("Next Water Change", FishPond::getNextWaterChange),
            ReportColumn.of("Note", FishPond::getNote)
    );

    public static final List<ReportColumn<FishHatch>> FISH_HATCHES = List.of(
            ReportColumn.of("Hatch Date", FishHatch::getHatchDate),
            ReportColumn.of("Pond", hatch -> hatch.getPond() != null ? hatch.getPond().getPondName() : ""),
            ReportColumn.of("Quantity Hatched", FishHatch::getQuantityHatched),
            ReportColumn.of("Hatch Rate", FishHatch::getHatchRate),
            ReportColumn.of("Male Count", FishHatch::getMaleCount),
            ReportColumn.of("Female Count", FishHatch::getFemaleCount),
            ReportColumn.of("Note", FishHatch::getNote)
    );

    private ReportColumnSets() {
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
