package com.kfarms.tenant.service;

import com.kfarms.demo.DemoAccountSupport;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Auditable;
import com.kfarms.entity.BillingSubscription;
import com.kfarms.entity.BillingSubscriptionStatus;
import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;
import com.kfarms.entity.FishFeedingSchedule;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.FishPondLocation;
import com.kfarms.entity.FishPondStatus;
import com.kfarms.entity.FishPondType;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import com.kfarms.entity.PoultryKeepingMethod;
import com.kfarms.entity.Role;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;
import com.kfarms.entity.SourceType;
import com.kfarms.entity.Supplies;
import com.kfarms.entity.SupplyCategory;
import com.kfarms.entity.Task;
import com.kfarms.entity.TaskSource;
import com.kfarms.entity.TaskStatus;
import com.kfarms.entity.TaskType;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.repository.BillingSubscriptionRepository;
import com.kfarms.repository.EggProductionRepo;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.FishHatchRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.InventoryRepository;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.repository.TaskRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(205)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kfarms.demo.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class DemoWorkspaceBootstrapRunner implements ApplicationRunner {

    private static final String ACTOR = "SYSTEM:DEMO_BOOTSTRAP";

    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final InventoryRepository inventoryRepository;
    private final SuppliesRepository suppliesRepository;
    private final SalesRepository salesRepository;
    private final LivestockRepository livestockRepository;
    private final EggProductionRepo eggProductionRepo;
    private final FishPondRepository fishPondRepository;
    private final FishHatchRepository fishHatchRepository;
    private final FeedRepository feedRepository;
    private final TaskRepository taskRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant tenant = upsertTenant();
        upsertSubscription(tenant);

        AppUser demoUser = upsertDemoUser();
        upsertMembership(tenant, demoUser);

        List<Livestock> flocks = ensureLivestock(tenant);
        List<FishPond> ponds = ensureFishPonds(tenant);

        ensureInventory(tenant);
        ensureSupplies(tenant);
        ensureEggProduction(tenant, flocks);
        ensureFishHatches(tenant, ponds);
        ensureFeeds(tenant, flocks, ponds);
        ensureSales(tenant);
        ensureTasks(tenant);

        log.info(
                "Demo workspace '{}' is ready for {}.",
                tenant.getSlug(),
                DemoAccountSupport.DEMO_VIEWER_EMAIL
        );
    }

    private Tenant upsertTenant() {
        Tenant tenant = tenantRepository.findBySlugIgnoreCase(DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG)
                .orElseGet(Tenant::new);

        tenant.setName(DemoAccountSupport.DEMO_VIEWER_TENANT_NAME);
        tenant.setSlug(DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG);
        tenant.setPlan(TenantPlan.ENTERPRISE);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPoultryEnabled(true);
        tenant.setFishEnabled(true);
        tenant.setContactEmail("demo@kfarms.africa");
        tenant.setContactPhone("+2348000000000");
        tenant.setAddress("Konnect Demo Yard, Abuja");
        tenant.setTimezone("Africa/Lagos");
        tenant.setCurrency("NGN");
        tenant.setBrandPrimaryColor("#2563EB");
        tenant.setBrandAccentColor("#10B981");
        tenant.setLoginHeadline("Explore the KFarms demo");
        tenant.setLoginMessage(DemoAccountSupport.DEMO_VIEWER_TENANT_NOTE);
        tenant.setReportFooter("KFarms Demo Workspace");
        tenant.setWatermarkEnabled(true);
        tenant.setCriticalSmsAlertsEnabled(false);
        stamp(tenant);
        return tenantRepository.save(tenant);
    }

    private void upsertSubscription(Tenant tenant) {
        BillingSubscription subscription = billingSubscriptionRepository.findByTenant_Id(tenant.getId())
                .orElseGet(BillingSubscription::new);

        subscription.setTenant(tenant);
        subscription.setPlan(TenantPlan.ENTERPRISE);
        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setProvider("DEMO");
        subscription.setAmount(money("0"));
        subscription.setCurrency("NGN");
        subscription.setBillingInterval("MONTHLY");
        subscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        subscription.setCancelAtPeriodEnd(false);
        subscription.setSubscriptionReference("DEMO-WORKSPACE");
        stamp(subscription);
        billingSubscriptionRepository.save(subscription);
    }

    private AppUser upsertDemoUser() {
        AppUser user = appUserRepository.findByEmail(DemoAccountSupport.DEMO_VIEWER_EMAIL)
                .or(() -> appUserRepository.findByUsername(DemoAccountSupport.DEMO_VIEWER_USERNAME))
                .orElseGet(AppUser::new);

        user.setUsername(DemoAccountSupport.DEMO_VIEWER_USERNAME);
        user.setEmail(DemoAccountSupport.DEMO_VIEWER_EMAIL);
        user.setPassword(passwordEncoder.encode(DemoAccountSupport.DEMO_VIEWER_PASSWORD));
        user.setRole(Role.USER);
        user.setPlatformAccess(false);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        stamp(user);
        return appUserRepository.save(user);
    }

    private void upsertMembership(Tenant tenant, AppUser user) {
        TenantMember member = tenantMemberRepository.findByTenant_IdAndUser_Id(tenant.getId(), user.getId())
                .orElseGet(TenantMember::new);

        member.setTenant(tenant);
        member.setUser(user);
        member.setRole(TenantRole.MANAGER);
        member.setActive(true);
        member.setLandingPage("/dashboard");
        member.setThemePreference("SYSTEM");
        member.setEmailNotifications(false);
        member.setPushNotifications(false);
        member.setWeeklySummary(false);
        member.setCompactTables(false);
        stamp(member);
        tenantMemberRepository.save(member);
    }

    private List<Livestock> ensureLivestock(Tenant tenant) {
        if (livestockRepository.countActiveByTenantId(tenant.getId()) > 0) {
            return livestockRepository.findAllActive(tenant.getId());
        }

        createLivestock(
                tenant,
                "Demo Layers North",
                LivestockType.LAYER,
                520,
                LocalDate.now().minusWeeks(14),
                20,
                12,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.BATTERY_CAGE,
                "Main laying house with steady egg collection."
        );
        createLivestock(
                tenant,
                "Demo Layers South",
                LivestockType.LAYER,
                460,
                LocalDate.now().minusWeeks(11),
                17,
                9,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.DEEP_LITTER,
                "Secondary house with strong recovery after heat management."
        );
        createLivestock(
                tenant,
                "Demo Broilers West",
                LivestockType.BROILER,
                320,
                LocalDate.now().minusWeeks(6),
                4,
                6,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.BROODER_HOUSE,
                "Fast-moving batch being prepared for market buyers."
        );

        return livestockRepository.findAllActive(tenant.getId());
    }

    private List<FishPond> ensureFishPonds(Tenant tenant) {
        if (fishPondRepository.countActiveByTenantId(tenant.getId()) > 0) {
            return fishPondRepository.findAllActiveByTenantId(tenant.getId());
        }

        createFishPond(
                tenant,
                "Demo Hatchery Vat",
                FishPondType.HATCHING,
                18000,
                25000,
                48,
                FishFeedingSchedule.BOTH,
                FishPondStatus.ACTIVE,
                LocalDate.now().minusWeeks(8),
                "Stable hatchery vat with healthy fry output."
        );
        createFishPond(
                tenant,
                "Demo Grow-out Pond A",
                FishPondType.GROW_OUT,
                4200,
                6000,
                71,
                FishFeedingSchedule.BOTH,
                FishPondStatus.ACTIVE,
                LocalDate.now().minusWeeks(12),
                "Primary pond serving fresh fish buyers."
        );
        createFishPond(
                tenant,
                "Demo Holding Pond",
                FishPondType.HOLDING,
                980,
                1600,
                14,
                FishFeedingSchedule.EVENING,
                FishPondStatus.ACTIVE,
                LocalDate.now().minusWeeks(4),
                "Harvest-ready pond for late-week dispatch."
        );

        return fishPondRepository.findAllActiveByTenantId(tenant.getId());
    }

    private void ensureInventory(Tenant tenant) {
        if (!inventoryRepository.findAllActiveByTenantId(tenant.getId()).isEmpty()) {
            return;
        }

        createInventory(tenant, "Layer Mash 25kg", InventoryCategory.FEED, 125, 35, "bags", "11850", "Premier Feed", "Feed Room A", "Primary ration for the laying houses.");
        createInventory(tenant, "Broiler Finisher Feed", InventoryCategory.FEED, 68, 18, "bags", "10900", "Premier Feed", "Feed Room B", "Finisher stock for broiler batches.");
        createInventory(tenant, "Floating Feed 4mm", InventoryCategory.FEED, 96, 28, "bags", "17100", "Blue Crown Aqua", "Aqua Store", "Grow-out ration for pond stock.");
        createInventory(tenant, "Newcastle Vaccine", InventoryCategory.MEDICINE, 22, 6, "vials", "4300", "VetLine", "Cold Shelf", "Routine poultry vaccination reserve.");
        createInventory(tenant, "Catfish Fingerlings", InventoryCategory.FISH, 16000, 4500, "pieces", "120", "Delta Hatchery", "Nursery Bay", "Backup nursery stock for replenishment.");
        createInventory(tenant, "Harvest Crates", InventoryCategory.EQUIPMENT, 140, 35, "pieces", "7600", "Agro Depot", "Packing Hall", "Shared crate pool for eggs and fish.");
    }

    private void ensureSupplies(Tenant tenant) {
        if (!suppliesRepository.findAllActiveByTenantId(tenant.getId()).isEmpty()) {
            return;
        }

        createSupply(tenant, "Layer Mash 25kg", SupplyCategory.FEED, 90, "11600", LocalDate.now().minusDays(9), "Premier Feed", "Fresh laying-house restock after weekend orders.");
        createSupply(tenant, "Floating Feed 4mm", SupplyCategory.FEED, 44, "16950", LocalDate.now().minusDays(13), "Blue Crown Aqua", "Grow-out pond ration for the next feed cycle.");
        createSupply(tenant, "Catfish Fingerlings", SupplyCategory.FISH, 12000, "118", LocalDate.now().minusDays(21), "Delta Hatchery", "Supplementary nursery stock for the main ponds.");
        createSupply(tenant, "Newcastle Vaccine", SupplyCategory.MEDICINE, 12, "4250", LocalDate.now().minusDays(27), "VetLine", "Routine disease-prevention top-up.");
        createSupply(tenant, "Harvest Crates", SupplyCategory.EQUIPMENT, 30, "7400", LocalDate.now().minusDays(35), "Agro Depot", "Additional crates before the larger retail run.");
    }

    private void ensureEggProduction(Tenant tenant, List<Livestock> flocks) {
        if (!eggProductionRepo.findAllActiveVisibleToTenant(tenant.getId()).isEmpty()) {
            return;
        }

        List<Livestock> layerFlocks = flocks.stream()
                .filter(flock -> flock.getType() == LivestockType.LAYER)
                .sorted(Comparator.comparing(Livestock::getId))
                .toList();

        if (layerFlocks.size() < 2) {
            return;
        }

        createEggProduction(tenant, layerFlocks.get(0), LocalDate.now().minusDays(6), 796, 14, "North house stayed strong after ration balancing.");
        createEggProduction(tenant, layerFlocks.get(1), LocalDate.now().minusDays(5), 742, 19, "South house held stable with low shell breakage.");
        createEggProduction(tenant, layerFlocks.get(0), LocalDate.now().minusDays(3), 812, 12, "Morning collection exceeded target after cooler weather.");
        createEggProduction(tenant, layerFlocks.get(1), LocalDate.now().minusDays(1), 758, 15, "Cleaner nest boxes reduced damaged eggs.");
    }

    private void ensureFishHatches(Tenant tenant, List<FishPond> ponds) {
        if (!fishHatchRepository.findAllByTenant_IdAndDeletedFalse(tenant.getId()).isEmpty()) {
            return;
        }

        FishPond hatchery = ponds.stream()
                .filter(pond -> pond.getPondType() == FishPondType.HATCHING)
                .findFirst()
                .orElse(null);
        if (hatchery == null) {
            return;
        }

        createFishHatch(tenant, hatchery, 28, 42, LocalDate.now().minusDays(24), 81.4, 15000, "Strong hatch cycle with healthy fry survival.");
        createFishHatch(tenant, hatchery, 24, 39, LocalDate.now().minusDays(10), 78.6, 13600, "Second hatch cycle after water-quality adjustment.");
    }

    private void ensureFeeds(Tenant tenant, List<Livestock> flocks, List<FishPond> ponds) {
        if (!feedRepository.findAllActiveByTenantId(tenant.getId()).isEmpty()) {
            return;
        }

        Livestock leadLayer = flocks.stream()
                .filter(flock -> flock.getType() == LivestockType.LAYER)
                .findFirst()
                .orElse(null);
        Livestock broiler = flocks.stream()
                .filter(flock -> flock.getType() == LivestockType.BROILER)
                .findFirst()
                .orElse(null);
        FishPond hatchery = ponds.stream()
                .filter(pond -> pond.getPondType() == FishPondType.HATCHING)
                .findFirst()
                .orElse(null);
        FishPond growOut = ponds.stream()
                .filter(pond -> pond.getPondType() == FishPondType.GROW_OUT)
                .findFirst()
                .orElse(null);

        if (leadLayer != null) {
            createFeed(tenant, FeedBatchType.LAYER, leadLayer.getId(), "Layer Mash 25kg", 20, "11600", LocalDate.now().minusDays(5), "North laying house morning ration.");
            createFeed(tenant, FeedBatchType.LAYER, leadLayer.getId(), "Layer Mash 25kg", 18, "11600", LocalDate.now().minusDays(2), "Follow-up feed run for peak lay cycle.");
        }
        if (broiler != null) {
            createFeed(tenant, FeedBatchType.BROILER, broiler.getId(), "Broiler Finisher Feed", 13, "10800", LocalDate.now().minusDays(3), "Broiler west finishing cycle.");
        }
        if (hatchery != null) {
            createFeed(tenant, FeedBatchType.FISH, hatchery.getId(), "Floating Feed 4mm", 12, "16950", LocalDate.now().minusDays(4), "Hatchery ration before grading.");
        }
        if (growOut != null) {
            createFeed(tenant, FeedBatchType.FISH, growOut.getId(), "Floating Feed 4mm", 19, "16950", LocalDate.now().minusDays(1), "Grow-out pond main feed cycle.");
        }
    }

    private void ensureSales(Tenant tenant) {
        if (salesRepository.countByTenantId(tenant.getId()) > 0) {
            return;
        }

        createSale(tenant, "Premium Table Eggs - 360 crates", SalesCategory.LAYER, 360, "5800", LocalDate.now().minusDays(2), "Regional distributor", "Large supermarket order for weekend shelves.");
        createSale(tenant, "Fresh Catfish 780kg", SalesCategory.FISH, 780, "2550", LocalDate.now().minusDays(8), "Cold-chain buyer", "Fresh fish consignment for retail outlets.");
        createSale(tenant, "Broiler Birds", SalesCategory.LIVESTOCK, 190, "7200", LocalDate.now().minusDays(18), "Hotel chain", "Event catering delivery for a recurring buyer.");
        createSale(tenant, "Organic Manure Bags", SalesCategory.MANURE, 58, "2100", LocalDate.now().minusDays(31), "Crop cooperative", "Processed manure shipment to nearby farms.");
    }

    private void ensureTasks(Tenant tenant) {
        if (!taskRepository.findRecentActiveByTenantId(tenant.getId(), PageRequest.of(0, 1)).isEmpty()) {
            return;
        }

        createTask(tenant, "Check demo feed variance", "Compare poultry and fish feed usage against current stock before the next restock.", LocalDateTime.now().plusDays(2));
        createTask(tenant, "Prep Friday harvest board", "Review pond harvest targets and confirm cold-room crate readiness.", LocalDateTime.now().plusDays(4));
        createTask(tenant, "Schedule layer health round", "Confirm vaccination timing and note any egg-quality drift in the south house.", LocalDateTime.now().plusDays(6));
    }

    private Livestock createLivestock(
            Tenant tenant,
            String batchName,
            LivestockType type,
            int currentStock,
            LocalDate arrivalDate,
            int startingAgeInWeeks,
            int mortality,
            SourceType sourceType,
            PoultryKeepingMethod keepingMethod,
            String note
    ) {
        Livestock livestock = new Livestock();
        livestock.setTenantId(tenant.getId());
        livestock.setBatchName(batchName);
        livestock.setType(type);
        livestock.setCurrentStock(currentStock);
        livestock.setArrivalDate(arrivalDate);
        livestock.setStartingAgeInWeeks(startingAgeInWeeks);
        livestock.setMortality(mortality);
        livestock.setSourceType(sourceType);
        livestock.setKeepingMethod(keepingMethod);
        livestock.setNote(note);
        stamp(livestock);
        return livestockRepository.save(livestock);
    }

    private FishPond createFishPond(
            Tenant tenant,
            String pondName,
            FishPondType pondType,
            int currentStock,
            int capacity,
            int mortality,
            FishFeedingSchedule schedule,
            FishPondStatus status,
            LocalDate dateStocked,
            String note
    ) {
        FishPond pond = new FishPond();
        pond.setTenant(tenant);
        pond.setPondName(pondName);
        pond.setPondType(pondType);
        pond.setCurrentStock(currentStock);
        pond.setCapacity(capacity);
        pond.setMortalityCount(mortality);
        pond.setFeedingSchedule(schedule);
        pond.setStatus(status);
        pond.setPondLocation(FishPondLocation.FARM);
        pond.setDateStocked(dateStocked);
        pond.setLastWaterChange(LocalDate.now().minusDays(5));
        pond.setNote(note);
        stamp(pond);
        return fishPondRepository.save(pond);
    }

    private Inventory createInventory(
            Tenant tenant,
            String itemName,
            InventoryCategory category,
            int quantity,
            int threshold,
            String unit,
            String unitCost,
            String supplierName,
            String storageLocation,
            String note
    ) {
        Inventory inventory = new Inventory();
        inventory.setTenant(tenant);
        inventory.setItemName(itemName);
        inventory.setCategory(category);
        inventory.setQuantity(quantity);
        inventory.setMinThreshold(threshold);
        inventory.setUnit(unit);
        inventory.setUnitCost(money(unitCost));
        inventory.setSupplierName(supplierName);
        inventory.setStorageLocation(storageLocation);
        inventory.setNote(note);
        inventory.setLastUpdated(LocalDate.now().minusDays(1));
        stamp(inventory);
        return inventoryRepository.save(inventory);
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

    private Task createTask(Tenant tenant, String title, String description, LocalDateTime dueDate) {
        Task task = Task.builder()
                .title(title)
                .description(description)
                .type(TaskType.OTHER)
                .status(TaskStatus.PENDING)
                .source(TaskSource.MANUAL)
                .priority(2)
                .dueDate(dueDate)
                .tenant(tenant)
                .build();
        stamp(task);
        return taskRepository.save(task);
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void stamp(Auditable auditable) {
        if (auditable.getCreatedBy() == null) {
            auditable.setCreatedBy(ACTOR);
        }
        auditable.setUpdatedBy(ACTOR);
    }
}
