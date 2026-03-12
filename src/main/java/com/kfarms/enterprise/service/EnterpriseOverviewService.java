package com.kfarms.enterprise.service;

import com.kfarms.enterprise.entity.EnterpriseSite;
import com.kfarms.enterprise.repository.EnterpriseSiteRepository;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.Livestock;
import com.kfarms.repository.EggProductionRepo;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.repository.TenantMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnterpriseOverviewService {

    private final EnterpriseSiteRepository enterpriseSiteRepository;
    private final SalesRepository salesRepository;
    private final SuppliesRepository suppliesRepository;
    private final FeedRepository feedRepository;
    private final EggProductionRepo eggProductionRepo;
    private final LivestockRepository livestockRepository;
    private final FishPondRepository fishPondRepository;
    private final TenantMemberRepository tenantMemberRepository;

    public List<EnterpriseSite> listSites(Tenant tenant) {
        return enterpriseSiteRepository.findByTenant_IdOrderByNameAsc(tenant.getId());
    }

    public Map<String, Object> buildOverview(Tenant tenant) {
        Long tenantId = tenant.getId();
        LocalDate today = LocalDate.now();
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate lastThirtyDays = today.minusDays(29);
        LocalDate lastFourteenDays = today.minusDays(13);

        List<EnterpriseSite> sites = listSites(tenant);
        List<Livestock> livestockBatches = livestockRepository.findAllActive(tenantId);
        List<FishPond> fishPonds = fishPondRepository.findAllActiveByTenantId(tenantId);
        List<TenantMember> members = tenantMemberRepository.findByTenant_Id(tenantId);

        BigDecimal currentMonthRevenue = safe(salesRepository.sumTotalBetween(tenantId, currentMonthStart, today));
        BigDecimal currentMonthExpenses = safe(suppliesRepository.sumSupplyCostBetween(tenantId, currentMonthStart, today));
        BigDecimal feedUsedLastThirtyDays = BigDecimal.valueOf(safeDouble(feedRepository.sumFeedUsedBetween(tenantId, lastThirtyDays, today)));
        long eggsLastFourteenDays = safeLong(eggProductionRepo.sumEggsBetween(tenantId, lastFourteenDays, today));

        long activeBirds = livestockBatches.stream().mapToLong(batch -> safeInt(batch.getCurrentStock())).sum();
        long poultryMortality = livestockBatches.stream().mapToLong(batch -> safeInt(batch.getMortality())).sum();
        long totalFishStock = fishPonds.stream().mapToLong(pond -> safeInt(pond.getCurrentStock())).sum();
        long fishMortality = fishPonds.stream().mapToLong(pond -> safeInt(pond.getMortalityCount())).sum();

        BigDecimal siteRevenue = BigDecimal.ZERO;
        BigDecimal siteExpenses = BigDecimal.ZERO;
        BigDecimal siteFeedUsage = BigDecimal.ZERO;
        BigDecimal siteFishHarvest = BigDecimal.ZERO;
        long siteEggProjection = 0;
        int activeSiteCount = 0;

        List<Map<String, Object>> sitePerformance = new ArrayList<>();
        List<Map<String, Object>> alerts = new ArrayList<>();
        Map<String, BigDecimal> moduleRevenue = new LinkedHashMap<>();
        Map<String, BigDecimal> moduleExpenses = new LinkedHashMap<>();

        for (EnterpriseSite site : sites) {
            if (Boolean.TRUE.equals(site.getActive())) {
                activeSiteCount += 1;
            }

            BigDecimal revenue = safe(site.getCurrentMonthRevenue());
            BigDecimal expenses = safe(site.getCurrentMonthExpenses());
            BigDecimal margin = revenue.subtract(expenses);
            BigDecimal feedUsage = safe(site.getCurrentFeedUsageKg());
            BigDecimal mortalityRate = safe(site.getCurrentMortalityRate());

            siteRevenue = siteRevenue.add(revenue);
            siteExpenses = siteExpenses.add(expenses);
            siteFeedUsage = siteFeedUsage.add(feedUsage);
            siteFishHarvest = siteFishHarvest.add(safe(site.getProjectedFishHarvestKg()));
            siteEggProjection += safeInt(site.getProjectedEggOutput30d());

            int enabledModules = (Boolean.TRUE.equals(site.getPoultryEnabled()) ? 1 : 0)
                    + (Boolean.TRUE.equals(site.getFishEnabled()) ? 1 : 0);
            BigDecimal moduleRevenueShare = enabledModules > 1
                    ? revenue.divide(BigDecimal.valueOf(enabledModules), 2, RoundingMode.HALF_UP)
                    : revenue;
            BigDecimal moduleExpenseShare = enabledModules > 1
                    ? expenses.divide(BigDecimal.valueOf(enabledModules), 2, RoundingMode.HALF_UP)
                    : expenses;

            if (Boolean.TRUE.equals(site.getPoultryEnabled())) {
                moduleRevenue.merge("POULTRY", moduleRevenueShare, BigDecimal::add);
                moduleExpenses.merge("POULTRY", moduleExpenseShare, BigDecimal::add);
            }
            if (Boolean.TRUE.equals(site.getFishEnabled())) {
                moduleRevenue.merge("FISH_FARMING", moduleRevenueShare, BigDecimal::add);
                moduleExpenses.merge("FISH_FARMING", moduleExpenseShare, BigDecimal::add);
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", site.getId());
            row.put("name", site.getName());
            row.put("code", site.getCode());
            row.put("location", site.getLocation());
            row.put("managerName", site.getManagerName());
            row.put("active", site.getActive());
            row.put("poultryEnabled", site.getPoultryEnabled());
            row.put("fishEnabled", site.getFishEnabled());
            row.put("poultryHouseCount", safeInt(site.getPoultryHouseCount()));
            row.put("pondCount", safeInt(site.getPondCount()));
            row.put("activeBirdCount", safeInt(site.getActiveBirdCount()));
            row.put("fishStockCount", safeInt(site.getFishStockCount()));
            row.put("currentMonthRevenue", revenue);
            row.put("currentMonthExpenses", expenses);
            row.put("currentMonthMargin", margin);
            row.put("currentFeedUsageKg", feedUsage);
            row.put("projectedEggOutput30d", safeInt(site.getProjectedEggOutput30d()));
            row.put("projectedFishHarvestKg", safe(site.getProjectedFishHarvestKg()));
            row.put("currentMortalityRate", mortalityRate);
            row.put("revenuePerPoultryHouse", perUnit(revenue, safeInt(site.getPoultryHouseCount())));
            row.put("revenuePerPond", perUnit(revenue, safeInt(site.getPondCount())));
            row.put("notes", site.getNotes());
            sitePerformance.add(row);

            if (margin.compareTo(BigDecimal.ZERO) < 0) {
                alerts.add(alert("FINANCE", site.getName() + " is running negative cashflow this month."));
            }
            if (mortalityRate.compareTo(BigDecimal.valueOf(5)) >= 0) {
                alerts.add(alert("MORTALITY", site.getName() + " has a mortality rate above 5%."));
            }
        }

        double poultryMortalityRate = calculateRate(poultryMortality, activeBirds + poultryMortality);
        double fishMortalityRate = calculateRate(fishMortality, totalFishStock + fishMortality);

        BigDecimal projectedCashflow = (currentMonthRevenue.compareTo(BigDecimal.ZERO) > 0 || currentMonthExpenses.compareTo(BigDecimal.ZERO) > 0)
                ? currentMonthRevenue.subtract(currentMonthExpenses)
                : siteRevenue.subtract(siteExpenses);
        BigDecimal feedForecast = feedUsedLastThirtyDays.compareTo(BigDecimal.ZERO) > 0
                ? feedUsedLastThirtyDays.multiply(BigDecimal.valueOf(1.08)).setScale(2, RoundingMode.HALF_UP)
                : siteFeedUsage.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
        long eggForecast = eggsLastFourteenDays > 0
                ? Math.round((eggsLastFourteenDays / 14.0) * 30.0)
                : siteEggProjection;
        BigDecimal fishHarvestForecast = siteFishHarvest.compareTo(BigDecimal.ZERO) > 0
                ? siteFishHarvest
                : BigDecimal.valueOf(totalFishStock).multiply(BigDecimal.valueOf(0.35)).setScale(2, RoundingMode.HALF_UP);

        if (projectedCashflow.compareTo(BigDecimal.ZERO) < 0) {
            alerts.add(alert("FINANCE", "Projected cashflow for the next 30 days is negative."));
        }
        if (poultryMortalityRate >= 4.0) {
            alerts.add(alert("POULTRY", "Poultry mortality is above the 4% watch threshold."));
        }
        if (fishMortalityRate >= 6.0) {
            alerts.add(alert("FISH", "Fish mortality is above the 6% watch threshold."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("overview", Map.of(
                "totalSites", sites.size(),
                "activeSites", activeSiteCount,
                "currentMonthRevenue", currentMonthRevenue,
                "currentMonthExpenses", currentMonthExpenses,
                "projectedCashflow30d", projectedCashflow,
                "totalBirdsTracked", activeBirds,
                "totalFishTracked", totalFishStock
        ));
        payload.put("forecasts", Map.of(
                "feedDemand30dKg", feedForecast,
                "eggOutput30d", eggForecast,
                "fishHarvest60dKg", fishHarvestForecast,
                "poultryMortalityRate", round(poultryMortalityRate),
                "fishMortalityRate", round(fishMortalityRate)
        ));
        payload.put("sitePerformance", sitePerformance);
        payload.put("modulePerformance", buildModulePerformance(moduleRevenue, moduleExpenses));
        payload.put("teamMix", buildTeamMix(members));
        payload.put("poultryBatchHighlights", buildPoultryHighlights(livestockBatches));
        payload.put("fishPondHighlights", buildFishHighlights(fishPonds));
        payload.put("alerts", alerts);
        return payload;
    }

    private List<Map<String, Object>> buildModulePerformance(
            Map<String, BigDecimal> revenueByModule,
            Map<String, BigDecimal> expenseByModule
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String module : List.of("POULTRY", "FISH_FARMING")) {
            BigDecimal revenue = safe(revenueByModule.get(module));
            BigDecimal expenses = safe(expenseByModule.get(module));
            rows.add(Map.of(
                    "module", module,
                    "estimatedRevenue", revenue,
                    "estimatedExpenses", expenses,
                    "estimatedMargin", revenue.subtract(expenses)
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildTeamMix(List<TenantMember> members) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TenantMember member : members) {
            if (!Boolean.TRUE.equals(member.getActive())) {
                continue;
            }
            String key = member.getRole() != null ? member.getRole().name() : "STAFF";
            counts.merge(key, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        counts.forEach((role, count) -> rows.add(Map.of("role", role, "count", count)));
        return rows;
    }

    private List<Map<String, Object>> buildPoultryHighlights(List<Livestock> batches) {
        return batches.stream()
                .sorted((left, right) -> Integer.compare(safeInt(right.getCurrentStock()), safeInt(left.getCurrentStock())))
                .limit(4)
                .map(batch -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("batchName", batch.getBatchName());
                    row.put("type", batch.getType() != null ? batch.getType().name() : "UNKNOWN");
                    row.put("currentStock", safeInt(batch.getCurrentStock()));
                    row.put("mortality", safeInt(batch.getMortality()));
                    row.put("risk", safeInt(batch.getMortality()) >= 100 ? "WATCHLIST" : "STABLE");
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> buildFishHighlights(List<FishPond> ponds) {
        return ponds.stream()
                .sorted((left, right) -> Integer.compare(safeInt(right.getCurrentStock()), safeInt(left.getCurrentStock())))
                .limit(4)
                .map(pond -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("pondName", pond.getPondName());
                    row.put("status", pond.getStatus() != null ? pond.getStatus().name() : "UNKNOWN");
                    row.put("currentStock", safeInt(pond.getCurrentStock()));
                    row.put("mortality", safeInt(pond.getMortalityCount()));
                    return row;
                })
                .toList();
    }

    private Map<String, Object> alert(String type, String message) {
        return Map.of("type", type, "message", message);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal perUnit(BigDecimal total, int units) {
        if (units <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
    }
}
