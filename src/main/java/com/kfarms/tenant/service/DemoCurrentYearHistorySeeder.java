package com.kfarms.tenant.service;

import com.kfarms.demo.DemoAccountSupport;
import com.kfarms.entity.Auditable;
import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;
import com.kfarms.entity.Supplies;
import com.kfarms.entity.SupplyCategory;
import com.kfarms.repository.EggProductionRepo;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.FishHatchRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(220)
@RequiredArgsConstructor
public class DemoCurrentYearHistorySeeder implements ApplicationRunner {

    private static final String ACTOR = "SYSTEM:DEMO_HISTORY";

    private final TenantRepository tenantRepository;
    private final LivestockRepository livestockRepository;
    private final EggProductionRepo eggProductionRepo;
    private final FeedRepository feedRepository;
    private final SalesRepository salesRepository;
    private final SuppliesRepository suppliesRepository;
    private final FishPondRepository fishPondRepository;
    private final FishHatchRepository fishHatchRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant tenant = tenantRepository
                .findBySlugIgnoreCase(DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG)
                .orElse(null);

        if (tenant == null) {
            log.info("Demo history backfill skipped because tenant '{}' was not found.", DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG);
            return;
        }

        int year = LocalDate.now().getYear();
        List<Livestock> flocks = livestockRepository.findAllActive(tenant.getId());
        List<FishPond> ponds = fishPondRepository.findAllActiveByTenantId(tenant.getId());

        List<Livestock> layerFlocks = flocks.stream()
                .filter(flock -> flock.getType() == LivestockType.LAYER)
                .sorted(Comparator.comparing(Livestock::getId))
                .toList();
        List<Livestock> broilerFlocks = flocks.stream()
                .filter(flock -> flock.getType() == LivestockType.BROILER)
                .sorted(Comparator.comparing(Livestock::getId))
                .toList();
        List<FishPond> activePonds = ponds.stream()
                .sorted(Comparator.comparing(FishPond::getId))
                .toList();

        if (layerFlocks.size() < 2 || broilerFlocks.isEmpty() || activePonds.isEmpty()) {
            log.warn("Demo history backfill skipped because the demo tenant is missing required flocks or ponds.");
            return;
        }

        Set<Integer> eggMonths = eggProductionRepo.findAllActiveVisibleToTenant(tenant.getId()).stream()
                .map(EggProduction::getCollectionDate)
                .filter(date -> date != null && date.getYear() == year)
                .map(LocalDate::getMonthValue)
                .collect(Collectors.toSet());
        Set<Integer> feedMonths = feedRepository.findAllActiveByTenantId(tenant.getId()).stream()
                .map(Feed::getDate)
                .filter(date -> date != null && date.getYear() == year)
                .map(LocalDate::getMonthValue)
                .collect(Collectors.toSet());
        Set<Integer> salesMonths = salesRepository.findAllActiveByTenantId(tenant.getId()).stream()
                .map(Sales::getSalesDate)
                .filter(date -> date != null && date.getYear() == year)
                .map(LocalDate::getMonthValue)
                .collect(Collectors.toSet());
        Set<Integer> supplyMonths = suppliesRepository.findAllActiveByTenantId(tenant.getId()).stream()
                .map(Supplies::getSupplyDate)
                .filter(date -> date != null && date.getYear() == year)
                .map(LocalDate::getMonthValue)
                .collect(Collectors.toSet());
        Set<Integer> hatchMonths = fishHatchRepository.findAllByTenant_IdAndDeletedFalse(tenant.getId()).stream()
                .map(FishHatch::getHatchDate)
                .filter(date -> date != null && date.getYear() == year)
                .map(LocalDate::getMonthValue)
                .collect(Collectors.toSet());

        List<Integer> seededMonths = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            boolean seeded = false;
            Month monthName = Month.of(month);
            int monthOffset = month - 1;

            if (!eggMonths.contains(month)) {
                seedEggMonth(
                        tenant,
                        layerFlocks.get(0),
                        layerFlocks.get(1),
                        year,
                        month,
                        monthName,
                        monthOffset
                );
                seeded = true;
            }

            if (!feedMonths.contains(month)) {
                seedFeedMonth(
                        tenant,
                        layerFlocks.get(0),
                        broilerFlocks.get(0),
                        activePonds.get(0),
                        year,
                        month,
                        monthName,
                        monthOffset
                );
                seeded = true;
            }

            if (!salesMonths.contains(month)) {
                seedSalesMonth(tenant, year, month, monthName, monthOffset);
                seeded = true;
            }

            if (!supplyMonths.contains(month)) {
                seedSupplyMonth(tenant, year, month, monthName, monthOffset);
                seeded = true;
            }

            if (!hatchMonths.contains(month)) {
                FishPond hatcheryPond = activePonds.get(monthOffset % activePonds.size());
                seedHatchMonth(tenant, hatcheryPond, year, month, monthName, monthOffset);
                seeded = true;
            }

            if (seeded) {
                seededMonths.add(month);
            }
        }

        if (seededMonths.isEmpty()) {
            log.info("Demo history backfill found complete current-year coverage for tenant '{}'.", tenant.getSlug());
            return;
        }

        log.info("Demo history backfill seeded missing current-year months {} for tenant '{}'.", seededMonths, tenant.getSlug());
    }

    private void seedEggMonth(
            Tenant tenant,
            Livestock leadLayer,
            Livestock supportLayer,
            int year,
            int month,
            Month monthName,
            int monthOffset
    ) {
        createEggProduction(
                tenant,
                leadLayer,
                LocalDate.of(year, month, 6),
                760 + (monthOffset * 11),
                14 + (monthOffset % 4),
                monthName + " primary house collection with stable tray output."
        );
        createEggProduction(
                tenant,
                supportLayer,
                LocalDate.of(year, month, 19),
                708 + (monthOffset * 9),
                11 + ((monthOffset + 1) % 5),
                monthName + " support house collection after flock balancing."
        );
    }

    private void seedFeedMonth(
            Tenant tenant,
            Livestock leadLayer,
            Livestock broilerFlock,
            FishPond fishPond,
            int year,
            int month,
            Month monthName,
            int monthOffset
    ) {
        createFeed(
                tenant,
                FeedBatchType.LAYER,
                leadLayer.getId(),
                "Enterprise Layer Mash",
                24 + (monthOffset % 4),
                "11800",
                LocalDate.of(year, month, 5),
                monthName + " layer ration for the flagship laying house."
        );
        createFeed(
                tenant,
                FeedBatchType.BROILER,
                broilerFlock.getId(),
                "Enterprise Finisher Feed",
                14 + (monthOffset % 3),
                "10750",
                LocalDate.of(year, month, 13),
                monthName + " broiler finishing run for scheduled buyers."
        );
        createFeed(
                tenant,
                FeedBatchType.FISH,
                fishPond.getId(),
                "Floating Feed 4mm",
                15 + (monthOffset % 5),
                "17000",
                LocalDate.of(year, month, 21),
                monthName + " aqua feed cycle for the main pond line."
        );
    }

    private void seedSalesMonth(Tenant tenant, int year, int month, Month monthName, int monthOffset) {
        createSale(
                tenant,
                "Premium Table Eggs - " + (320 + (monthOffset * 8)) + " crates",
                SalesCategory.LAYER,
                320 + (monthOffset * 8),
                "5800",
                LocalDate.of(year, month, 11),
                "Regional distributor",
                monthName + " egg consignment for supermarket shelves."
        );
        createSale(
                tenant,
                "Fresh Catfish " + (640 + (monthOffset * 18)) + "kg",
                SalesCategory.FISH,
                640 + (monthOffset * 18),
                "2550",
                LocalDate.of(year, month, 24),
                "Cold-chain buyer",
                monthName + " fish harvest routed through the processing desk."
        );
    }

    private void seedSupplyMonth(Tenant tenant, int year, int month, Month monthName, int monthOffset) {
        createSupply(
                tenant,
                "Enterprise Layer Mash",
                SupplyCategory.FEED,
                120 + (monthOffset * 3),
                "11800",
                LocalDate.of(year, month, 4),
                "Premier Feed",
                monthName + " warehouse restock for laying houses."
        );
        createSupply(
                tenant,
                "Floating Feed 4mm",
                SupplyCategory.FEED,
                72 + (monthOffset * 2),
                "17000",
                LocalDate.of(year, month, 17),
                "Blue Crown Aqua",
                monthName + " fish feed restock for ponds and holding units."
        );
    }

    private void seedHatchMonth(
            Tenant tenant,
            FishPond pond,
            int year,
            int month,
            Month monthName,
            int monthOffset
    ) {
        createFishHatch(
                tenant,
                pond,
                34 + (monthOffset % 5),
                49 + (monthOffset % 6),
                LocalDate.of(year, month, 9),
                79.5 + (monthOffset % 4),
                18200 + (monthOffset * 420),
                monthName + " hatch cycle for continuous pond stocking."
        );
    }

    private EggProduction createEggProduction(
            Tenant tenant,
            Livestock livestock,
            LocalDate collectionDate,
            int goodEggs,
            int damagedEggs,
            String note
    ) {
        EggProduction egg = new EggProduction();
        egg.setTenant(tenant);
        egg.setLivestock(livestock);
        egg.setCollectionDate(collectionDate);
        egg.setGoodEggs(goodEggs);
        egg.setDamagedEggs(damagedEggs);
        egg.setNote(note);
        egg.calculateCrates();
        stamp(egg);
        return eggProductionRepo.save(egg);
    }

    private Feed createFeed(
            Tenant tenant,
            FeedBatchType batchType,
            Long batchId,
            String feedName,
            int quantityUsed,
            String unitCost,
            LocalDate date,
            String note
    ) {
        Feed feed = new Feed();
        feed.setTenant(tenant);
        feed.setBatchType(batchType);
        feed.setBatchId(batchId);
        feed.setFeedName(feedName);
        feed.setQuantityUsed(quantityUsed);
        feed.setUnitCost(money(unitCost));
        feed.setInventoryTracked(true);
        feed.setDate(date);
        feed.setNote(note);
        stamp(feed);
        return feedRepository.save(feed);
    }

    private Supplies createSupply(
            Tenant tenant,
            String itemName,
            SupplyCategory category,
            int quantity,
            String unitPrice,
            LocalDate supplyDate,
            String supplierName,
            String note
    ) {
        Supplies supply = new Supplies();
        supply.setTenant(tenant);
        supply.setItemName(itemName);
        supply.setCategory(category);
        supply.setQuantity(quantity);
        supply.setUnitPrice(money(unitPrice));
        supply.setTotalPrice(money(unitPrice).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
        supply.setSupplyDate(supplyDate);
        supply.setSupplierName(supplierName);
        supply.setNote(note);
        stamp(supply);
        return suppliesRepository.save(supply);
    }

    private Sales createSale(
            Tenant tenant,
            String itemName,
            SalesCategory category,
            int quantity,
            String unitPrice,
            LocalDate salesDate,
            String buyer,
            String note
    ) {
        Sales sale = new Sales();
        sale.setTenant(tenant);
        sale.setItemName(itemName);
        sale.setCategory(category);
        sale.setQuantity(quantity);
        sale.setUnitPrice(money(unitPrice));
        sale.setTotalPrice(money(unitPrice).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
        sale.setSalesDate(salesDate);
        sale.setBuyer(buyer);
        sale.setNote(note);
        stamp(sale);
        return salesRepository.save(sale);
    }

    private FishHatch createFishHatch(
            Tenant tenant,
            FishPond pond,
            int maleCount,
            int femaleCount,
            LocalDate hatchDate,
            double hatchRate,
            int quantityHatched,
            String note
    ) {
        FishHatch hatch = new FishHatch();
        hatch.setTenant(tenant);
        hatch.setPond(pond);
        hatch.setMaleCount(maleCount);
        hatch.setFemaleCount(femaleCount);
        hatch.setHatchDate(hatchDate);
        hatch.setHatchRate(hatchRate);
        hatch.setQuantityHatched(quantityHatched);
        hatch.setNote(note);
        stamp(hatch);
        return fishHatchRepository.save(hatch);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void stamp(Auditable auditable) {
        auditable.setCreatedBy(ACTOR);
        auditable.setUpdatedBy(ACTOR);
    }
}
