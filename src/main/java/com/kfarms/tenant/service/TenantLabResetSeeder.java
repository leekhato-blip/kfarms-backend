package com.kfarms.tenant.service;

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
import com.kfarms.entity.NotificationType;
import com.kfarms.entity.AppUser;
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
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantAuditAction;
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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kfarms.demo.reset-lab", havingValue = "true")
public class TenantLabResetSeeder implements ApplicationRunner {

    private static final String ACTOR = "SYSTEM:LAB_RESET";
    private static final String DEFAULT_USER_PASSWORD = "FarmDemo@2026";
    private static final String FALLBACK_PLATFORM_OWNER_PASSWORD = "PlatformOwner@2026";
    private static final String PLATFORM_OWNER_EMAIL = "leekhato@gmail.com";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final TenantAuditLogService tenantAuditLogService;
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
    private final NotificationService notificationService;

    private record OwnerSnapshot(String username, String email, String passwordHash) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        OwnerSnapshot ownerSnapshot = snapshotPlatformOwner();

        resetDatabase();

        createUser(
                ownerSnapshot.username(),
                ownerSnapshot.email(),
                ownerSnapshot.passwordHash(),
                Role.PLATFORM_ADMIN,
                true
        );
        createUser("roots.ops", "platform.ops@demo.kfarms.local", Role.PLATFORM_ADMIN, true);
        createUser("roots.analyst", "analyst@demo.kfarms.local", Role.USER, true);
        createUser("roots.support", "support@demo.kfarms.local", Role.USER, true);
        createUser("roots.billing", "billing@demo.kfarms.local", Role.USER, true);
        createUser("roots.success", "success@demo.kfarms.local", Role.USER, true);

        Tenant umarTenant = createTenant(
                "Umar Poultry Farm",
                "umar-farms",
                TenantPlan.FREE,
                true,
                false,
                "ops@umarfarms.example",
                "+2348030000101",
                "Kubwa, Abuja",
                "Africa/Lagos"
        );
        createSubscription(umarTenant, TenantPlan.FREE, "0");
        seedFreeTenant(umarTenant);

        Tenant isahTenant = createTenant(
                "Isah Aqua Farm",
                "isah-farms",
                TenantPlan.PRO,
                false,
                true,
                "ops@isahfarm.example",
                "+2348030000202",
                "Ikorodu, Lagos",
                "Africa/Lagos"
        );
        createSubscription(isahTenant, TenantPlan.PRO, "10000");
        seedProTenant(isahTenant);

        Tenant deltaTenant = createTenant(
                "Delta Integrated Farms",
                "delta-integrated",
                TenantPlan.ENTERPRISE,
                true,
                true,
                "ops@deltafarms.example",
                "+2348030000303",
                "Asaba, Delta",
                "Africa/Lagos"
        );
        createSubscription(deltaTenant, TenantPlan.ENTERPRISE, "120000");
        seedEnterpriseTenant(deltaTenant);

        log.info("Tenant lab reset completed with 3 clean tenants, 2 platform admins, and seeded farm data.");
    }

    private OwnerSnapshot snapshotPlatformOwner() {
        AppUser existingOwner = appUserRepository.findByEmail(PLATFORM_OWNER_EMAIL).orElse(null);
        if (existingOwner == null) {
            return new OwnerSnapshot(
                    "kato",
                    PLATFORM_OWNER_EMAIL,
                    passwordEncoder.encode(FALLBACK_PLATFORM_OWNER_PASSWORD)
            );
        }

        return new OwnerSnapshot(
                existingOwner.getUsername(),
                existingOwner.getEmail(),
                existingOwner.getPassword()
        );
    }

    private void resetDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    billing_checkout_sessions,
                    billing_invoices,
                    billing_subscriptions,
                    egg_production,
                    feed,
                    fish_hatch_records,
                    fish_pond,
                    fish_stock_movement,
                    hatch_record,
                    health_event_advice,
                    health_events,
                    inventory,
                    livestock,
                    notification,
                    password_reset_token,
                    recurring_task_rules,
                    recurring_times,
                    sales,
                    supplies,
                    support_assistant_messages,
                    support_ticket_messages,
                    support_tickets,
                    tasks,
                    tenant_audit_logs,
                    tenant_invitations,
                    tenant_members,
                    tenant,
                    app_user
                RESTART IDENTITY CASCADE
                """);
    }

    private AppUser createUser(String username, String email, Role role, boolean enabled) {
        return createUser(username, email, passwordEncoder.encode(DEFAULT_USER_PASSWORD), role, enabled);
    }

    private AppUser createUser(String username, String email, String passwordHash, Role role, boolean enabled) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setRole(role);
        user.setEnabled(enabled);
        stamp(user);
        return appUserRepository.save(user);
    }

    private Tenant createTenant(
            String name,
            String slug,
            TenantPlan plan,
            boolean poultryEnabled,
            boolean fishEnabled,
            String contactEmail,
            String contactPhone,
            String address,
            String timezone
    ) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(slug);
        tenant.setPlan(plan);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setPoultryEnabled(poultryEnabled);
        tenant.setFishEnabled(fishEnabled);
        tenant.setContactEmail(contactEmail);
        tenant.setContactPhone(contactPhone);
        tenant.setAddress(address);
        tenant.setCurrency("NGN");
        tenant.setTimezone(timezone);
        tenant.setWatermarkEnabled(Boolean.TRUE);
        stamp(tenant);
        return tenantRepository.save(tenant);
    }

    private void createSubscription(Tenant tenant, TenantPlan plan, String amount) {
        BillingSubscription subscription = new BillingSubscription();
        subscription.setTenant(tenant);
        subscription.setPlan(plan);
        subscription.setStatus(BillingSubscriptionStatus.ACTIVE);
        subscription.setProvider("MANUAL");
        subscription.setAmount(money(amount));
        subscription.setCurrency("NGN");
        subscription.setBillingInterval("MONTHLY");
        subscription.setNextBillingDate(LocalDate.now().plusMonths(1));
        subscription.setCancelAtPeriodEnd(false);
        subscription.setSubscriptionReference("SUB-" + tenant.getSlug().toUpperCase().replace("-", ""));
        subscription.setPaymentChannel("BANK_TRANSFER");
        stamp(subscription);
        billingSubscriptionRepository.save(subscription);
    }

    private void seedFreeTenant(Tenant tenant) {
        AppUser owner = createUser("umar.owner", "umar.owner@demo.kfarms.local", Role.USER, true);
        AppUser admin = createUser("umar.admin", "umar.admin@demo.kfarms.local", Role.USER, true);
        AppUser manager = createUser("umar.manager", "umar.manager@demo.kfarms.local", Role.USER, true);
        AppUser staff = createUser("umar.staff", "umar.staff@demo.kfarms.local", Role.USER, true);

        createMembership(tenant, owner, TenantRole.OWNER, true, "/poultry");
        createMembership(tenant, admin, TenantRole.ADMIN, true, "/poultry");
        createMembership(tenant, manager, TenantRole.MANAGER, false, "/poultry");
        createMembership(tenant, staff, TenantRole.STAFF, false, "/poultry");

        List<Livestock> flocks = new ArrayList<>();
        flocks.add(createLivestock(
                tenant,
                "Umar Layers A",
                LivestockType.LAYER,
                480,
                LocalDate.now().minusWeeks(10),
                16,
                6,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.BATTERY_CAGE,
                "Peak lay cycle with steady feed conversion."
        ));
        flocks.add(createLivestock(
                tenant,
                "Umar Layers B",
                LivestockType.LAYER,
                420,
                LocalDate.now().minusWeeks(9),
                15,
                8,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.DEEP_LITTER,
                "Secondary house with strong egg recovery after heat stress."
        ));
        flocks.add(createLivestock(
                tenant,
                "Umar Broilers C",
                LivestockType.BROILER,
                300,
                LocalDate.now().minusWeeks(4),
                3,
                5,
                SourceType.SUPPLIER,
                null,
                "Fast moving broiler pen prepared for market week."
        ));

        createInventory(tenant, "Layer Mash 25kg", InventoryCategory.FEED, 110, 30, "bags", "11950", "Golden Feed Mills", "Feed Room A", "Primary laying ration");
        createInventory(tenant, "Grower Mash 25kg", InventoryCategory.FEED, 42, 15, "bags", "9800", "Golden Feed Mills", "Feed Room B", "Used for broilers and growers");
        createInventory(tenant, "Newcastle Vaccine", InventoryCategory.MEDICINE, 18, 6, "vials", "4200", "VetLine", "Cold Shelf", "Monthly vaccination stock");
        createInventory(tenant, "Plastic Egg Trays", InventoryCategory.EQUIPMENT, 180, 50, "pieces", "450", "Agro Depot", "Packaging Rack", "Reusable crate-ready trays");

        createSupply(tenant, "Layer Mash 25kg", SupplyCategory.FEED, 80, "11500", LocalDate.now().minusDays(7), "Golden Feed Mills", "Fresh batch for the two layer houses.");
        createSupply(tenant, "Grower Mash 25kg", SupplyCategory.FEED, 24, "9800", LocalDate.now().minusDays(13), "Golden Feed Mills", "Broiler top-up before market finish.");
        createSupply(tenant, "Newcastle Vaccine", SupplyCategory.MEDICINE, 10, "4300", LocalDate.now().minusDays(21), "VetLine", "Emergency disease-prevention restock.");
        createSupply(tenant, "Plastic Egg Trays", SupplyCategory.EQUIPMENT, 120, "450", LocalDate.now().minusDays(16), "Agro Depot", "Extra packaging for direct delivery customers.");

        createEggProduction(tenant, flocks.get(0), LocalDate.now().minusDays(3), 438, 9, "Morning collection stayed strong despite heat.");
        createEggProduction(tenant, flocks.get(0), LocalDate.now().minusDays(2), 441, 7, "Improved shell quality after mineral adjustment.");
        createEggProduction(tenant, flocks.get(1), LocalDate.now().minusDays(3), 388, 11, "Deep litter house recovered from last week's stress.");
        createEggProduction(tenant, flocks.get(1), LocalDate.now().minusDays(1), 395, 8, "Good lay and low breakage after nest-box cleanup.");

        createFeed(tenant, FeedBatchType.LAYER, flocks.get(0).getId(), "Layer Mash 25kg", 18, "10800", LocalDate.now().minusDays(4), "Two morning runs for House A.");
        createFeed(tenant, FeedBatchType.LAYER, flocks.get(1).getId(), "Layer Mash 25kg", 16, "10800", LocalDate.now().minusDays(3), "House B ration with oyster shell supplement.");
        createFeed(tenant, FeedBatchType.BROILER, flocks.get(2).getId(), "Grower Mash 25kg", 11, "9600", LocalDate.now().minusDays(2), "Broiler grower feed before final finishing.");
        createFeed(tenant, FeedBatchType.LAYER, flocks.get(0).getId(), "Layer Mash 25kg", 17, "10800", LocalDate.now().minusDays(1), "Late day top-up after extra egg demand.");

        createSale(tenant, "Table Eggs - 220 crates", SalesCategory.LAYER, 220, "5600", LocalDate.now().minusDays(1), "Fresh basket supermarket", "Large order for weekend pickup.");
        createSale(tenant, "Spent Layer Hens", SalesCategory.LIVESTOCK, 18, "9500", LocalDate.now().minusDays(6), "Mile 12 trader", "Cull sale after production review.");
        createSale(tenant, "Poultry Manure Bags", SalesCategory.MANURE, 40, "1800", LocalDate.now().minusDays(4), "Vegetable growers collective", "Bagged litter sold to nearby farmers.");

        createTask(tenant, "Vaccinate Umar Layers A", "Apply scheduled Newcastle booster for House A.", LocalDateTime.now().plusDays(2));
        createTask(tenant, "Prepare broiler market weights", "Confirm final counts and average weight before Friday sales.", LocalDateTime.now().plusDays(3));

        notificationService.createNotification(tenant.getId(), NotificationType.GENERAL.name(), "Umar farm ready", "Free poultry workspace seeded with 3 active flocks and live records.", null);
        notificationService.createNotification(tenant.getId(), NotificationType.FINANCE.name(), "Egg cashflow pulse", "Recent egg sales and supply purchases are now available for tenant-scoping checks.", null);
    }

    private void seedProTenant(Tenant tenant) {
        AppUser owner = createUser("isah.owner", "isah.owner@demo.kfarms.local", Role.USER, true);
        AppUser admin = createUser("isah.admin", "isah.admin@demo.kfarms.local", Role.USER, true);
        AppUser manager = createUser("isah.manager", "isah.manager@demo.kfarms.local", Role.USER, true);

        createMembership(tenant, owner, TenantRole.OWNER, true, "/fish-ponds");
        createMembership(tenant, admin, TenantRole.ADMIN, true, "/fish-ponds");
        createMembership(tenant, manager, TenantRole.MANAGER, true, "/fish-ponds");
        for (int index = 1; index <= 7; index++) {
            AppUser staff = createUser(
                    "isah.staff" + index,
                    "isah.staff" + index + "@demo.kfarms.local",
                    Role.USER,
                    true
            );
            createMembership(tenant, staff, TenantRole.STAFF, true, "/fish-ponds");
        }

        FishPond broodPond = createFishPond(tenant, "Isah Brood Pond East", FishPondType.BROODSTOCK, 650, 900, 12, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(18), "Stable broodstock with strong spawning response.");
        FishPond hatcheryVat = createFishPond(tenant, "Isah Hatchery Vat A", FishPondType.HATCHING, 22000, 30000, 90, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(8), "High-survival hatchery vat for catfish fry.");
        FishPond growOutA = createFishPond(tenant, "Isah Grow-out Pond A", FishPondType.GROW_OUT, 3800, 5000, 75, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(12), "Primary pond feeding contract buyers.");
        FishPond growOutB = createFishPond(tenant, "Isah Grow-out Pond B", FishPondType.GROW_OUT, 3400, 5000, 62, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(10), "Balanced growth curve after grading.");
        FishPond nursery = createFishPond(tenant, "Isah Nursery Tank 1", FishPondType.HOLDING, 1600, 2500, 22, FishFeedingSchedule.MORNING, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(6), "Nursery tank for sorted juveniles.");
        FishPond harvestPond = createFishPond(tenant, "Isah Harvest Pond", FishPondType.HOLDING, 900, 1500, 18, FishFeedingSchedule.EVENING, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(4), "Holding pond for harvest-ready stock.");

        createFishHatch(tenant, hatcheryVat, 35, 55, LocalDate.now().minusDays(24), 82.5, 18000, "Strong hatch with balanced brood ratio.");
        createFishHatch(tenant, hatcheryVat, 32, 48, LocalDate.now().minusDays(11), 79.2, 16200, "Second cycle with slightly lower hatch rate due to weather.");

        createInventory(tenant, "Floating Feed 2mm", InventoryCategory.FEED, 90, 25, "bags", "14800", "Blue Crown Aqua", "Feed Shed", "Starter pellets for fry and juveniles");
        createInventory(tenant, "Floating Feed 4mm", InventoryCategory.FEED, 55, 18, "bags", "17100", "Blue Crown Aqua", "Feed Shed", "Grow-out ration for pond stock");
        createInventory(tenant, "Catfish Fingerlings", InventoryCategory.FISH, 12000, 3000, "pieces", "120", "Delta Hatchery", "Nursery Tank", "Healthy mixed-sex fingerlings");
        createInventory(tenant, "Water Test Strips", InventoryCategory.EQUIPMENT, 14, 4, "packs", "3200", "Aqua Tools", "Lab Shelf", "Daily pond chemistry checks");
        createInventory(tenant, "Salt Mineral Mix", InventoryCategory.MEDICINE, 18, 5, "bags", "5400", "VetLine Aqua", "Medicine Locker", "Stress reduction and treatment support");

        createSupply(tenant, "Floating Feed 2mm", SupplyCategory.FEED, 70, "14600", LocalDate.now().minusDays(8), "Blue Crown Aqua", "Restock for hatchery and nursery units.");
        createSupply(tenant, "Floating Feed 4mm", SupplyCategory.FEED, 40, "16900", LocalDate.now().minusDays(6), "Blue Crown Aqua", "Grow-out ration for the two main ponds.");
        createSupply(tenant, "Catfish Fingerlings", SupplyCategory.FISH, 10000, "120", LocalDate.now().minusDays(19), "Delta Hatchery", "Supplementary juvenile purchase for Pond B.");
        createSupply(tenant, "Aerator Repair Parts", SupplyCategory.EQUIPMENT, 6, "18500", LocalDate.now().minusDays(14), "Aqua Tools", "Critical spare parts for oxygen control.");

        createFeed(tenant, FeedBatchType.FISH, broodPond.getId(), "Floating Feed 4mm", 6, "16800", LocalDate.now().minusDays(4), "Brood pond ration and breeder conditioning.");
        createFeed(tenant, FeedBatchType.FISH, hatcheryVat.getId(), "Floating Feed 2mm", 14, "14600", LocalDate.now().minusDays(3), "Starter ration for hatchery vat.");
        createFeed(tenant, FeedBatchType.FISH, growOutA.getId(), "Floating Feed 4mm", 18, "16900", LocalDate.now().minusDays(2), "Growth feed for market fish pond A.");
        createFeed(tenant, FeedBatchType.FISH, growOutB.getId(), "Floating Feed 4mm", 17, "16900", LocalDate.now().minusDays(2), "Growth feed for market fish pond B.");
        createFeed(tenant, FeedBatchType.FISH, harvestPond.getId(), "Floating Feed 4mm", 7, "16900", LocalDate.now().minusDays(1), "Finishers for harvest-ready fish.");

        createSale(tenant, "Fresh Catfish 620kg", SalesCategory.FISH, 620, "2400", LocalDate.now().minusDays(2), "Lekki cold room", "Bulk chilled fish order.");
        createSale(tenant, "Smoked Catfish Packs", SalesCategory.FISH, 180, "3500", LocalDate.now().minusDays(5), "Hotel supply desk", "Processed packs for hospitality buyers.");
        createSale(tenant, "Fingerling Transfer Lot", SalesCategory.FISH, 2500, "140", LocalDate.now().minusDays(9), "Cooperative pond owner", "Juvenile stock sold after sorting.");

        createTask(tenant, "Flush hatchery filters", "Clean hatchery vat screens and confirm oxygen flow.", LocalDateTime.now().plusDays(1));
        createTask(tenant, "Grade Pond B fish", "Sort mixed sizes before the next feed cycle.", LocalDateTime.now().plusDays(4));

        notificationService.createNotification(tenant.getId(), NotificationType.GENERAL.name(), "Isah farm ready", "Pro fish workspace seeded with ponds, hatches, feeds, and fish sales.", null);
        notificationService.createNotification(tenant.getId(), NotificationType.SUPPLIES.name(), "Feed warehouse stocked", "Floating feed, fingerlings, and equipment records are available for verification.", null);
    }

    private void seedEnterpriseTenant(Tenant tenant) {
        AppUser owner = createUser("delta.owner", "delta.owner@demo.kfarms.local", Role.USER, true);
        AppUser admin = createUser("delta.admin", "delta.admin@demo.kfarms.local", Role.USER, true);
        AppUser manager = createUser("delta.manager", "delta.manager@demo.kfarms.local", Role.USER, true);

        createMembership(tenant, owner, TenantRole.OWNER, true, "/dashboard");
        createMembership(tenant, admin, TenantRole.ADMIN, true, "/dashboard");
        createMembership(tenant, manager, TenantRole.MANAGER, true, "/dashboard");
        for (int index = 1; index <= 12; index++) {
            AppUser staff = createUser(
                    "delta.staff" + index,
                    "delta.staff" + index + "@demo.kfarms.local",
                    Role.USER,
                    true
            );
            createMembership(tenant, staff, TenantRole.STAFF, true, "/dashboard");
        }

        List<Livestock> flocks = new ArrayList<>();
        flocks.add(createLivestock(
                tenant,
                "Delta Layers North",
                LivestockType.LAYER,
                900,
                LocalDate.now().minusWeeks(12),
                17,
                12,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.BATTERY_CAGE,
                "North house is the best-performing layer unit on site."
        ));
        flocks.add(createLivestock(
                tenant,
                "Delta Layers South",
                LivestockType.LAYER,
                850,
                LocalDate.now().minusWeeks(11),
                16,
                15,
                SourceType.SUPPLIER,
                PoultryKeepingMethod.DEEP_LITTER,
                "South house recovered from a ventilation adjustment."
        ));
        flocks.add(createLivestock(
                tenant,
                "Delta Broilers West",
                LivestockType.BROILER,
                600,
                LocalDate.now().minusWeeks(5),
                2,
                9,
                SourceType.SUPPLIER,
                null,
                "West broiler unit aligned to restaurant delivery cycle."
        ));
        flocks.add(createLivestock(
                tenant,
                "Delta Broilers East",
                LivestockType.BROILER,
                520,
                LocalDate.now().minusWeeks(4),
                2,
                7,
                SourceType.SUPPLIER,
                null,
                "East broiler unit staggered for market continuity."
        ));

        FishPond hatchery = createFishPond(tenant, "Delta Hatchery A", FishPondType.HATCHING, 26000, 32000, 95, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(7), "Enterprise hatchery supporting internal pond stocking.");
        FishPond growOne = createFishPond(tenant, "Delta Grow-out Pond 1", FishPondType.GROW_OUT, 4200, 5200, 70, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(13), "Main pond for wholesale buyers.");
        FishPond growTwo = createFishPond(tenant, "Delta Grow-out Pond 2", FishPondType.GROW_OUT, 3900, 5000, 66, FishFeedingSchedule.BOTH, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(12), "Second pond balanced for steady harvest.");
        FishPond holding = createFishPond(tenant, "Delta Holding Pond", FishPondType.HOLDING, 1200, 1800, 24, FishFeedingSchedule.EVENING, FishPondStatus.ACTIVE, LocalDate.now().minusWeeks(5), "Holding pond for processing pipeline.");

        createFishHatch(tenant, hatchery, 42, 62, LocalDate.now().minusDays(20), 84.7, 21400, "High-yield hatch used for internal pond stocking.");
        createFishHatch(tenant, hatchery, 38, 58, LocalDate.now().minusDays(9), 81.3, 19800, "Second cycle with improved fry survival.");

        createInventory(tenant, "Enterprise Layer Mash", InventoryCategory.FEED, 240, 60, "bags", "11900", "Premier Feed", "Warehouse A", "Primary ration for both layer houses");
        createInventory(tenant, "Enterprise Finisher Feed", InventoryCategory.FEED, 110, 35, "bags", "10800", "Premier Feed", "Warehouse B", "Broiler finisher feed");
        createInventory(tenant, "Floating Feed 4mm", InventoryCategory.FEED, 130, 40, "bags", "17000", "Blue Crown Aqua", "Aqua Store", "Grow-out fish feed");
        createInventory(tenant, "Catfish Fingerlings", InventoryCategory.FISH, 18000, 5000, "pieces", "125", "Delta Hatchery", "Nursery Bay", "Internal hatchery support stock");
        createInventory(tenant, "Poultry Vaccines", InventoryCategory.MEDICINE, 30, 8, "vials", "4600", "VetLine", "Cold Shelf", "Cross-house vaccination reserve");
        createInventory(tenant, "Harvest Crates", InventoryCategory.EQUIPMENT, 160, 40, "pieces", "7800", "Agro Depot", "Packing Hall", "Used for eggs and fish logistics");

        createSupply(tenant, "Enterprise Layer Mash", SupplyCategory.FEED, 180, "11800", LocalDate.now().minusDays(8), "Premier Feed", "Full laying-house restock after sales cycle.");
        createSupply(tenant, "Enterprise Finisher Feed", SupplyCategory.FEED, 95, "10750", LocalDate.now().minusDays(6), "Premier Feed", "Broiler feed for dual-house schedule.");
        createSupply(tenant, "Catfish Fingerlings", SupplyCategory.FISH, 15000, "123", LocalDate.now().minusDays(15), "Delta Hatchery", "Supplementary stock transfer into grow-out ponds.");
        createSupply(tenant, "Water Pump Service Kit", SupplyCategory.EQUIPMENT, 4, "26500", LocalDate.now().minusDays(10), "Aqua Tools", "Pump maintenance before the rainy cycle.");

        createEggProduction(tenant, flocks.get(0), LocalDate.now().minusDays(3), 812, 18, "North house maintained excellent lay rates.");
        createEggProduction(tenant, flocks.get(0), LocalDate.now().minusDays(1), 824, 15, "North house improved after ration balancing.");
        createEggProduction(tenant, flocks.get(1), LocalDate.now().minusDays(2), 768, 21, "South house stayed stable with lower breakage.");
        createEggProduction(tenant, flocks.get(1), LocalDate.now().minusDays(1), 776, 19, "Strong recovery after lighting adjustment.");

        createFeed(tenant, FeedBatchType.LAYER, flocks.get(0).getId(), "Enterprise Layer Mash", 30, "11800", LocalDate.now().minusDays(3), "North laying house feed run.");
        createFeed(tenant, FeedBatchType.LAYER, flocks.get(1).getId(), "Enterprise Layer Mash", 28, "11800", LocalDate.now().minusDays(2), "South laying house feed run.");
        createFeed(tenant, FeedBatchType.BROILER, flocks.get(2).getId(), "Enterprise Finisher Feed", 18, "10750", LocalDate.now().minusDays(2), "Broiler west finishing cycle.");
        createFeed(tenant, FeedBatchType.BROILER, flocks.get(3).getId(), "Enterprise Finisher Feed", 16, "10750", LocalDate.now().minusDays(1), "Broiler east finishing cycle.");
        createFeed(tenant, FeedBatchType.FISH, hatchery.getId(), "Floating Feed 4mm", 16, "17000", LocalDate.now().minusDays(3), "Hatchery feeding run.");
        createFeed(tenant, FeedBatchType.FISH, growOne.getId(), "Floating Feed 4mm", 21, "17000", LocalDate.now().minusDays(2), "Grow-out pond one feed cycle.");
        createFeed(tenant, FeedBatchType.FISH, growTwo.getId(), "Floating Feed 4mm", 19, "17000", LocalDate.now().minusDays(2), "Grow-out pond two feed cycle.");
        createFeed(tenant, FeedBatchType.FISH, holding.getId(), "Floating Feed 4mm", 8, "17000", LocalDate.now().minusDays(1), "Holding pond finishers.");

        createSale(tenant, "Premium Table Eggs - 420 crates", SalesCategory.LAYER, 420, "5800", LocalDate.now().minusDays(1), "Regional distributor", "Enterprise egg order for supermarkets.");
        createSale(tenant, "Fresh Catfish 840kg", SalesCategory.FISH, 840, "2550", LocalDate.now().minusDays(2), "Cold-chain buyer", "Fresh fish consignment for retail stores.");
        createSale(tenant, "Broiler Birds", SalesCategory.LIVESTOCK, 260, "7200", LocalDate.now().minusDays(4), "Hotel chain", "Broiler delivery for event catering.");
        createSale(tenant, "Organic Manure Bags", SalesCategory.MANURE, 85, "2200", LocalDate.now().minusDays(5), "Crop farms network", "Processed manure shipment.");

        createTask(tenant, "Reconcile integrated feed usage", "Match poultry and fish feed usage against current stock.", LocalDateTime.now().plusDays(2));
        createTask(tenant, "Prepare enterprise harvest board", "Update harvest-ready ponds and poultry market windows.", LocalDateTime.now().plusDays(4));
        createTask(tenant, "Inspect cold-room logistics", "Confirm crates, ice, and dispatch staging before the next big sale.", LocalDateTime.now().plusDays(6));

        notificationService.createNotification(tenant.getId(), NotificationType.GENERAL.name(), "Delta farm ready", "Enterprise workspace seeded with both poultry and fish operations.", null);
        notificationService.createNotification(tenant.getId(), NotificationType.FINANCE.name(), "Integrated revenue mix", "Eggs, fish, broilers, and manure sales are available for cross-module validation.", null);
    }

    private TenantMember createMembership(Tenant tenant, AppUser user, TenantRole role, boolean active, String landingPage) {
        TenantMember member = new TenantMember();
        member.setTenant(tenant);
        member.setUser(user);
        member.setRole(role);
        member.setActive(active);
        member.setLandingPage(landingPage);
        member.setThemePreference("SYSTEM");
        member.setEmailNotifications(true);
        member.setPushNotifications(true);
        member.setWeeklySummary(true);
        member.setCompactTables(false);
        stamp(member);
        TenantMember saved = tenantMemberRepository.save(member);

        tenantAuditLogService.record(
                tenant,
                ACTOR,
                TenantAuditAction.MEMBER_ROLE_CHANGED,
                "MEMBER",
                saved.getId(),
                user.getUsername(),
                user.getEmail(),
                "NONE",
                role.name(),
                "Seeded member " + user.getEmail() + " as " + role.name() + "."
        );

        tenantAuditLogService.record(
                tenant,
                ACTOR,
                active ? TenantAuditAction.MEMBER_ACTIVATED : TenantAuditAction.MEMBER_DEACTIVATED,
                "MEMBER",
                saved.getId(),
                user.getUsername(),
                user.getEmail(),
                active ? "false" : "true",
                String.valueOf(active),
                active
                        ? "Seeded active access for " + user.getEmail() + "."
                        : "Seeded inactive access for " + user.getEmail() + " to preserve plan seat limits."
        );

        return saved;
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
        auditable.setCreatedBy(ACTOR);
        auditable.setUpdatedBy(ACTOR);
    }
}
