package com.kfarms.service.impl;


import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.LivestockMapper;
import com.kfarms.repository.EggProductionRepo;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.service.LivestockService;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantPlanGuardService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LivestockServiceImpl implements LivestockService {
    private final LivestockRepository repo;
    private final EggProductionRepo eggRepo;
    private final NotificationService notification;
    private final TenantPlanGuardService planGuardService;

    // CREATE - create Livestock
    @Override
    public  LivestockResponseDto create(LivestockRequestDto request, String createBy) {
        Tenant tenant = planGuardService.requireCurrentTenant();
        Long tenantId = tenant.getId();
        long activeBatches = repo.countActiveByTenantId(tenantId);
        int maxPoultryFlocks = planGuardService.maxPoultryFlocksForPlan(tenant.getPlan());
        if (maxPoultryFlocks != Integer.MAX_VALUE && activeBatches >= maxPoultryFlocks) {
            throw new IllegalArgumentException(
                    "Poultry flock limit reached for the " + tenant.getPlan().name() + " plan."
            );
        }

        Livestock entity = LivestockMapper.toEntity(request);
        entity.setTenantId(tenantId);
        entity.setCreatedBy(createBy);
        repo.save(entity);
        return LivestockMapper.toResponseDto(entity);
    }

    // READ - get all Livestock (Pagination and Filtering)
    @Override
    public Map<String, Object> getAll(int page, int size, String batchName, String type, LocalDate arrivalDate, Boolean deleted) {

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");

        Pageable pageable = PageRequest.of(page, size, sort);



        // Convert type string to enum(if provided)
        LivestockType typeEnum = null;
        if(type != null && !type.isBlank()){
            try{
                typeEnum = LivestockType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ex){
                throw new IllegalArgumentException(
                        "Invalid livestock type: '" + type + "' . Allowed values: " + Arrays.toString(LivestockType.values())
                );
            }
        }

        final LivestockType typeEnumFinal = typeEnum;

        Long tenantId = TenantContext.getTenantId();

        Specification<Livestock> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (Boolean.TRUE.equals(deleted)) {
                predicates.add(cb.isTrue(root.get("deleted")));
            } else {
                predicates.add(cb.isFalse(root.get("deleted")));
            }
            
            if (batchName != null && !batchName.isBlank()) {
                // use lower on expression and lowercase the param for case-insensitive search
                predicates.add(cb.like(cb.lower(root.get("batchName")), "%" + batchName.toLowerCase() + "%"));
            }
            if (typeEnumFinal != null) {
                predicates.add(cb.equal(root.get("type"), typeEnumFinal));
            }
            if (arrivalDate != null) {
                predicates.add(cb.equal(root.get("arrivalDate"), arrivalDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Livestock> livestockPage = repo.findAll(spec, pageable);

        List<LivestockResponseDto> items = livestockPage
                .getContent()
                .stream()
                .map(LivestockMapper::toResponseDto)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", livestockPage.getNumber());
        result.put("size", livestockPage.getSize());
        result.put("totalItems", livestockPage.getTotalElements());
        result.put("totalPages", livestockPage.getTotalPages());
        result.put("hasNext", livestockPage.hasNext());
        result.put("hasPrevious", livestockPage.hasPrevious());

        return result;

    }

    // READ - get Livestock by ID
    @Override
    public LivestockResponseDto getById(Long id) {
        Long tenantId = TenantContext.getTenantId();

        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        return LivestockMapper.toResponseDto(entity);
    }

    // UPDATE - update existing Livestock
    @Override
    public LivestockResponseDto update(Long id, LivestockRequestDto request, String updatedBy){
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        // Update fields from request
        if (request.getBatchName() != null) {
            entity.setBatchName(request.getBatchName());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getArrivalDate() != null) {
            entity.setArrivalDate(request.getArrivalDate());
        }
        if (request.getSourceType() != null) {
            entity.setSourceType(request.getSourceType());
        }
        entity.setStartingAgeInWeeks(request.getStartingAgeInWeeks() != null ? request.getStartingAgeInWeeks() : entity.getStartingAgeInWeeks());
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }
        if (request.getKeepingMethod() != null) {
            entity.setKeepingMethod(request.getKeepingMethod());
        } else if (request.getType() != null && request.getType() != LivestockType.LAYER) {
            entity.setKeepingMethod(null);
        }
        entity.setUpdatedBy(updatedBy);

        // Handle quantity & mortality smartly
        if (request.getMortality() != null && request.getMortality() > 0) {
            int currentMortality = entity.getMortality() != null ? entity.getMortality() : 0;
            int currentQty = (entity.getCurrentStock() != null) ? entity.getCurrentStock() : 0;

            if (request.getMortality() > currentQty) {
                throw new IllegalArgumentException("Mortality cannot exceed current livestock quantity");
            }

            int newMortality = currentMortality + request.getMortality();
            entity.setMortality(newMortality);
            entity.setCurrentStock(currentQty - request.getMortality());
        } else if (request.getCurrentStock() != null) {
            // allow updating quantity directly (e.g. manual correction)
            entity.setCurrentStock(request.getCurrentStock());
        }

        repo.save(entity);
        return LivestockMapper.toResponseDto(entity);
    }

    // DELETE - delete livestock by ID
    @Override
    public void delete(Long id, String deletedBy){
        Long tenantId = TenantContext.getTenantId();

        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Livestock with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // DELETE (permanently)
    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        repo.delete(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Livestock with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary(){
        // filter out deleted items
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> all = repo.findAllActive(tenantId);

        Map<String, Object> summary = new HashMap<>();

        int totalQuantity = all.stream()
                .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                .sum();
        int totalMortality = all.stream().mapToInt(l -> l.getMortality() != null ? l.getMortality() : 0).sum();

        // Total livestock batches
        summary.put("totalLivestockBatches", all.size());
        // Total number of Mortality
        summary.put("totalMortality", totalMortality);
        // total number of livestock alive
        summary.put("totalQuantityAlive", totalQuantity);


        // count by type (alive only)
        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getType().name(),
                        Collectors.summingLong(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                ));
        summary.put("countByType", countByType);

        // ==== NOTIFICATIONS ====
        if (!all.isEmpty() && totalQuantity > 0 && totalQuantity < 50) {
            notification.createNotification(
                    tenantId,
                    "LIVESTOCK",
                    "Low Livestock Count",
                    "Total livestock count has dropped below 50. Please inspect",
                    null);
        }
        if (!all.isEmpty() && totalQuantity > 0 && totalMortality > 20) {
            notification.createNotification(
                    tenantId,
                    "LIVESTOCK",
                    "High Mortality Alert",
                    "More than 20 deaths record. Investigate possible disease or stress factors",
                    null);
        }

        return summary;
    }

    @Override
    public Map<String, Object> getOverview(int rangeDays) {
        Long tenantId = TenantContext.getTenantId();

        List<Livestock> all = repo.findAllActive(tenantId);

        int totalAlive = all.stream()
                .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                .sum();

        int totalMortality = all.stream()
                .mapToInt(l -> l.getMortality() != null ? l.getMortality() : 0)
                .sum();

        int totalBatches = all.size();

        double mortalityRate = 0.0;
        int totalStartedApprox = totalAlive + totalMortality;
        if (totalStartedApprox > 0) {
            mortalityRate = (totalMortality * 100.0) / totalStartedApprox;
        }

        // countByType (alive only)
        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getType().name(),
                        Collectors.summingLong(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                ));

        // Layers stage breakdown (computed)
        List<Livestock> layers = all.stream()
                .filter(l -> l.getType() == LivestockType.LAYER)
                .toList();

        int layersAlive = layers.stream()
                .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                .sum();

        Map<String, Integer> stageBreakdown = new LinkedHashMap<>();
        stageBreakdown.put("CHICKS", 0);
        stageBreakdown.put("GROWERS", 0);
        stageBreakdown.put("POINT_OF_LAY", 0);
        stageBreakdown.put("IN_LAY", 0);
        stageBreakdown.put("OLD_STOCK", 0);

        for (Livestock l : layers) {
            int alive = l.getCurrentStock() != null ? l.getCurrentStock() : 0;
            int ageWeeks = calculateAgeWeeks(l);
            String stage = resolveLayerStage(ageWeeks);
            stageBreakdown.put(stage, stageBreakdown.get(stage) + alive);
        }

        // Batch cards (rich list for UI)
        List<Map<String, Object>> batchCards = all.stream()
                .sorted(Comparator.comparing(Livestock::getId).reversed())
                .map(l -> {
                    int alive = l.getCurrentStock() != null ? l.getCurrentStock() : 0;
                    int mort = l.getMortality() != null ? l.getMortality() : 0;
                    int started = alive + mort;

                    double batchMortRate = 0.0;
                    if (started > 0) {
                        batchMortRate = (mort * 100.0) / started;
                    }

                    int ageWeeks = calculateAgeWeeks(l);
                    String stage = (l.getType() == LivestockType.LAYER) ? resolveLayerStage(ageWeeks) : "N/A";
                    String risk = resolveRisk(batchMortRate);

                    LocalDateTime lastUpdated = l.getUpdatedAt() != null ? l.getUpdatedAt() : l.getCreatedAt();

                    Map<String, Object> card = new LinkedHashMap<>();
                    card.put("id", l.getId());
                    card.put("batchName", l.getBatchName());
                    card.put("type", l.getType().name());

                    card.put("arrivalDate", l.getArrivalDate());
                    card.put("ageWeeks", ageWeeks);
                    card.put("stage", stage);

                    card.put("alive", alive);
                    card.put("mortalityTotal", mort);
                    card.put("mortalityRate", round2(batchMortRate));
                    card.put("risk", risk);

                    card.put("lastUpdated", lastUpdated);

                    return card;
                })
                .toList();

        // Attention needed (rules-based, NOT saved to DB)
        List<Map<String, Object>> attentionNeeded = new ArrayList<>();

        // Global attention
        if (totalAlive < 50) {
            attentionNeeded.add(attention("LOW_TOTAL_STOCK",
                    "Total livestock count is low (" + totalAlive + ")."));
        }

        // Critical batches attention (top 3)
        batchCards.stream()
                .filter(c -> "CRITICAL".equals(c.get("risk")))
                .limit(3)
                .forEach(c -> attentionNeeded.add(attention(
                        "HIGH_MORTALITY_RATE",
                        "High mortality rate in " + c.get("batchName") + " (" + c.get("mortalityRate") + "%)."
                )));

        // Recent activities (best-effort using updatedAt/createdAt)
        List<Map<String, Object>> recentActivities = all.stream()
                .map(l -> {
                    LocalDateTime t = l.getUpdatedAt() != null ? l.getUpdatedAt() : l.getCreatedAt();
                    if (t == null) return null;

                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("time", t);
                    a.put("title", "Batch Updated");
                    a.put("details", l.getBatchName() + " (" + l.getType().name() + ")");
                    return a;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")))
                .limit(10)
                .toList();

        // Build final response map
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalAlive", totalAlive);
        totals.put("totalMortality", totalMortality);
        totals.put("totalBatches", totalBatches);
        totals.put("mortalityRate", round2(mortalityRate));

        Map<String, Object> layersMap = new LinkedHashMap<>();
        layersMap.put("totalAlive", layersAlive);
        layersMap.put("stageBreakdown", stageBreakdown);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("rangeDays", rangeDays);
        meta.put("lastUpdated", LocalDateTime.now());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totals", totals);
        response.put("countByType", countByType);
        response.put("layers", layersMap);
        response.put("batchCards", batchCards);
        response.put("attentionNeeded", attentionNeeded);
        response.put("recentActivities", recentActivities);
        response.put("meta", meta);

        return response;
    }

    private int calculateAgeWeeks(Livestock l) {
        int starting = l.getStartingAgeInWeeks() != 0 ? l.getStartingAgeInWeeks() : 0;

        if (l.getArrivalDate() == null) return starting;

        long weeksSinceArrival = ChronoUnit.WEEKS.between(l.getArrivalDate(), LocalDate.now());
        if (weeksSinceArrival < 0) weeksSinceArrival = 0;

        return starting + (int) weeksSinceArrival;
    }

    private String resolveLayerStage(int ageWeeks) {
        if (ageWeeks >= 70) return "OLD_STOCK";
        if (ageWeeks >= 21) return "IN_LAY";
        if (ageWeeks >= 18) return "POINT_OF_LAY";
        if (ageWeeks >= 7) return "GROWERS";
        return "CHICKS";
    }

    private String resolveRisk(double mortalityRatePercent) {
        if (mortalityRatePercent >= 10.0) return "CRITICAL";
        if (mortalityRatePercent >= 4.0) return "WATCHLIST";
        return "HEALTHY";
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Map<String, Object> attention(String code, String message) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("code", code);
        a.put("message", message);
        return a;
    }


    // ADJUST stock
    @Override
    public LivestockResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();

        Livestock livestock = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        livestock.adjustStock(request.getQuantity(), request.getReason());
        livestock.setUpdatedBy(updatedBy);

        repo.save(livestock);
        return LivestockMapper.toResponseDto(livestock);
    }



}
