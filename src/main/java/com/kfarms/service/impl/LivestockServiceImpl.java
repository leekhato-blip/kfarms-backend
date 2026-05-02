package com.kfarms.service.impl;

import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.dto.MortalityRecordRequestDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockMortalityRecord;
import com.kfarms.entity.LivestockType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.LivestockMapper;
import com.kfarms.repository.LivestockMortalityRecordRepository;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.service.LivestockService;
import com.kfarms.service.NotificationService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantPlanGuardService;
import com.kfarms.tenant.service.TenantRecordAuditService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class LivestockServiceImpl implements LivestockService {
    private final LivestockRepository repo;
    private final LivestockMortalityRecordRepository mortalityRecordRepository;
    private final NotificationService notification;
    private final TenantPlanGuardService planGuardService;
    private final TenantRecordAuditService tenantRecordAuditService;

    @Override
    public LivestockResponseDto create(LivestockRequestDto request, String createBy) {
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
        tenantRecordAuditService.created(
                tenantId,
                createBy,
                "LIVESTOCK",
                entity.getId(),
                livestockTargetName(entity),
                livestockSummary(entity),
                "Created livestock record for " + livestockTargetName(entity) + "."
        );
        return toResponseDto(entity, null);
    }

    @Override
    public Map<String, Object> getAll(int page, int size, String batchName, String type, LocalDate arrivalDate, Boolean deleted) {
        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);

        LivestockType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = LivestockType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ex) {
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
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildLivestockMortalitySnapshots(
                mortalityRecordRepository.findAllByTenantIdAndDeletedFalse(tenantId)
        );

        List<LivestockResponseDto> items = livestockPage.getContent().stream()
                .map(entity -> toResponseDto(entity, mortalitySnapshots.get(entity.getId())))
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

    @Override
    public LivestockResponseDto getById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        return buildResponseForTenant(tenantId, entity);
    }

    @Override
    public LivestockResponseDto update(Long id, LivestockRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        String previousSummary = livestockSummary(entity);

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
        entity.setStartingAgeInWeeks(
                request.getStartingAgeInWeeks() != null ? request.getStartingAgeInWeeks() : entity.getStartingAgeInWeeks()
        );
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }
        if (request.getKeepingMethod() != null) {
            entity.setKeepingMethod(request.getKeepingMethod());
        } else if (request.getType() != null && request.getType() != LivestockType.LAYER) {
            entity.setKeepingMethod(null);
        }
        entity.setUpdatedBy(updatedBy);

        if (request.getMortality() != null && request.getMortality() > 0) {
            applyMortality(entity, request.getMortality());
        } else if (request.getCurrentStock() != null) {
            entity.setCurrentStock(request.getCurrentStock());
        }

        repo.save(entity);

        if (request.getMortality() != null && request.getMortality() > 0) {
            recordMortalityEvent(
                    entity,
                    request.getMortality(),
                    LocalDate.now(),
                    request.getNote(),
                    updatedBy
            );
        }

        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "LIVESTOCK",
                entity.getId(),
                livestockTargetName(entity),
                previousSummary,
                livestockSummary(entity),
                "Updated livestock record for " + livestockTargetName(entity) + "."
        );

        return buildResponseForTenant(tenantId, entity);
    }

    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        String previousSummary = livestockSummary(entity);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Livestock with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
        tenantRecordAuditService.deleted(
                tenantId,
                deletedBy,
                "LIVESTOCK",
                entity.getId(),
                livestockTargetName(entity),
                previousSummary,
                "Deleted livestock record for " + livestockTargetName(entity) + "."
        );
    }

    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock entity = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        repo.delete(entity);
    }

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

    @Override
    public Map<String, Object> getSummary() {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> all = repo.findAllActive(tenantId);
        List<LivestockMortalityRecord> mortalityRecords = mortalityRecordRepository.findAllByTenantIdAndDeletedFalse(tenantId);

        Map<String, Object> summary = new HashMap<>();
        int totalQuantity = all.stream()
                .mapToInt(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                .sum();
        int totalMortality = all.stream()
                .mapToInt(l -> l.getMortality() != null ? l.getMortality() : 0)
                .sum();

        summary.put("totalLivestockBatches", all.size());
        summary.put("totalMortality", totalMortality);
        summary.put("totalQuantityAlive", totalQuantity);
        summary.put("weeklyMortality", sumMortalitySince(mortalityRecords, weekStart()));
        summary.put("monthlyMortality", sumMortalitySince(mortalityRecords, monthStart()));

        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getType().name(),
                        Collectors.summingLong(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                ));
        summary.put("countByType", countByType);

        if (totalQuantity < 50) {
            notification.createNotification(
                    tenantId,
                    "LIVESTOCK",
                    "Low Livestock Count",
                    "Total livestock count has dropped below 50. Please inspect",
                    null
            );
        }
        if (totalMortality > 20) {
            notification.createNotification(
                    tenantId,
                    "LIVESTOCK",
                    "High Mortality Alert",
                    "More than 20 deaths record. Investigate possible disease or stress factors",
                    null
            );
        }

        return summary;
    }

    @Override
    public Map<String, Object> getOverview(int rangeDays) {
        Long tenantId = TenantContext.getTenantId();
        List<Livestock> all = repo.findAllActive(tenantId);
        List<LivestockMortalityRecord> mortalityRecords = mortalityRecordRepository.findAllByTenantIdAndDeletedFalse(tenantId);
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildLivestockMortalitySnapshots(mortalityRecords);

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

        Map<String, Long> countByType = all.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getType().name(),
                        Collectors.summingLong(l -> l.getCurrentStock() != null ? l.getCurrentStock() : 0)
                ));

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

        for (Livestock livestock : layers) {
            int alive = livestock.getCurrentStock() != null ? livestock.getCurrentStock() : 0;
            int ageWeeks = calculateAgeWeeks(livestock);
            String stage = resolveLayerStage(ageWeeks);
            stageBreakdown.put(stage, stageBreakdown.get(stage) + alive);
        }

        List<Map<String, Object>> batchCards = all.stream()
                .sorted(Comparator.comparing(Livestock::getId).reversed())
                .map(livestock -> {
                    int alive = livestock.getCurrentStock() != null ? livestock.getCurrentStock() : 0;
                    int mortality = livestock.getMortality() != null ? livestock.getMortality() : 0;
                    int started = alive + mortality;
                    double batchMortalityRate = started > 0 ? (mortality * 100.0) / started : 0.0;
                    int ageWeeks = calculateAgeWeeks(livestock);
                    String stage = livestock.getType() == LivestockType.LAYER ? resolveLayerStage(ageWeeks) : "N/A";
                    String risk = resolveRisk(batchMortalityRate);
                    LocalDateTime lastUpdated = livestock.getUpdatedAt() != null ? livestock.getUpdatedAt() : livestock.getCreatedAt();
                    MortalitySnapshot snapshot = mortalitySnapshots.getOrDefault(livestock.getId(), MortalitySnapshot.EMPTY);

                    Map<String, Object> card = new LinkedHashMap<>();
                    card.put("id", livestock.getId());
                    card.put("batchName", livestock.getBatchName());
                    card.put("type", livestock.getType().name());
                    card.put("arrivalDate", livestock.getArrivalDate());
                    card.put("ageWeeks", ageWeeks);
                    card.put("stage", stage);
                    card.put("alive", alive);
                    card.put("mortalityTotal", mortality);
                    card.put("mortalityRate", round2(batchMortalityRate));
                    card.put("mortalityThisWeek", snapshot.weeklyCount());
                    card.put("mortalityThisMonth", snapshot.monthlyCount());
                    card.put("lastMortalityDate", snapshot.lastMortalityDate());
                    card.put("risk", risk);
                    card.put("lastUpdated", lastUpdated);
                    return card;
                })
                .toList();

        List<Map<String, Object>> attentionNeeded = new ArrayList<>();
        if (totalAlive < 50) {
            attentionNeeded.add(attention("LOW_TOTAL_STOCK", "Total livestock count is low (" + totalAlive + ")."));
        }
        batchCards.stream()
                .filter(card -> "CRITICAL".equals(card.get("risk")))
                .limit(3)
                .forEach(card -> attentionNeeded.add(attention(
                        "HIGH_MORTALITY_RATE",
                        "High mortality rate in " + card.get("batchName") + " (" + card.get("mortalityRate") + "%)."
                )));

        List<Map<String, Object>> recentActivities = Stream.concat(
                        all.stream().map(livestock -> {
                            LocalDateTime time = livestock.getUpdatedAt() != null ? livestock.getUpdatedAt() : livestock.getCreatedAt();
                            if (time == null) return null;

                            Map<String, Object> activity = new LinkedHashMap<>();
                            activity.put("time", time);
                            activity.put("title", "Batch Updated");
                            activity.put("details", livestock.getBatchName() + " (" + livestock.getType().name() + ")");
                            return activity;
                        }),
                        mortalityRecords.stream().map(record -> {
                            LocalDate mortalityDate = resolveMortalityDate(record.getMortalityDate(), record.getCreatedAt());
                            if (mortalityDate == null || record.getLivestock() == null) return null;

                            Map<String, Object> activity = new LinkedHashMap<>();
                            activity.put("time", mortalityDate.atStartOfDay());
                            activity.put("title", "Mortality Recorded");
                            activity.put(
                                    "details",
                                    record.getLivestock().getBatchName() + " · " + formatCount(record.getCount()) + " birds"
                            );
                            return activity;
                        })
                )
                .filter(Objects::nonNull)
                .sorted((left, right) -> ((LocalDateTime) right.get("time")).compareTo((LocalDateTime) left.get("time")))
                .limit(10)
                .toList();

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("totalAlive", totalAlive);
        totals.put("totalMortality", totalMortality);
        totals.put("totalBatches", totalBatches);
        totals.put("mortalityRate", round2(mortalityRate));
        totals.put("weeklyMortality", sumMortalitySince(mortalityRecords, weekStart()));
        totals.put("monthlyMortality", sumMortalitySince(mortalityRecords, monthStart()));

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
        response.put("mortalityByBatch", batchCards);
        response.put("attentionNeeded", attentionNeeded);
        response.put("recentActivities", recentActivities);
        response.put("meta", meta);
        return response;
    }

    @Override
    public LivestockResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock livestock = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        String previousSummary = livestockSummary(livestock);

        livestock.adjustStock(request.getQuantity(), request.getReason());
        livestock.setUpdatedBy(updatedBy);
        repo.save(livestock);
        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "LIVESTOCK",
                livestock.getId(),
                livestockTargetName(livestock),
                previousSummary,
                livestockSummary(livestock),
                "Adjusted livestock stock for " + livestockTargetName(livestock) + "."
        );
        return buildResponseForTenant(tenantId, livestock);
    }

    @Override
    public LivestockResponseDto recordMortality(Long id, MortalityRecordRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        Livestock livestock = repo.findByIdAndTenantId(id, tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        String previousSummary = livestockSummary(livestock);

        int count = Optional.ofNullable(request.getCount()).orElse(0);
        if (count <= 0) {
            throw new IllegalArgumentException("Mortality count must be greater than 0");
        }

        applyMortality(livestock, count);
        livestock.setUpdatedBy(updatedBy);
        repo.save(livestock);

        recordMortalityEvent(
                livestock,
                count,
                request.getMortalityDate() != null ? request.getMortalityDate() : LocalDate.now(),
                request.getNote(),
                updatedBy
        );

        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "LIVESTOCK",
                livestock.getId(),
                livestockTargetName(livestock),
                previousSummary,
                livestockSummary(livestock),
                "Recorded livestock mortality for " + livestockTargetName(livestock) + "."
        );

        return buildResponseForTenant(tenantId, livestock);
    }

    private void applyMortality(Livestock livestock, int count) {
        int currentMortality = Optional.ofNullable(livestock.getMortality()).orElse(0);
        int currentStock = Optional.ofNullable(livestock.getCurrentStock()).orElse(0);
        if (count > currentStock) {
            throw new IllegalArgumentException("Mortality cannot exceed current livestock quantity");
        }

        livestock.setMortality(currentMortality + count);
        livestock.setCurrentStock(currentStock - count);
    }

    private LivestockResponseDto buildResponseForTenant(Long tenantId, Livestock livestock) {
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildLivestockMortalitySnapshots(
                mortalityRecordRepository.findAllByTenantIdAndDeletedFalse(tenantId)
        );
        return toResponseDto(livestock, mortalitySnapshots.get(livestock.getId()));
    }

    private LivestockResponseDto toResponseDto(Livestock livestock, MortalitySnapshot snapshot) {
        LivestockResponseDto dto = LivestockMapper.toResponseDto(livestock);
        MortalitySnapshot safeSnapshot = snapshot != null ? snapshot : MortalitySnapshot.EMPTY;
        dto.setMortalityThisWeek(safeSnapshot.weeklyCount());
        dto.setMortalityThisMonth(safeSnapshot.monthlyCount());
        dto.setLastMortalityDate(safeSnapshot.lastMortalityDate());
        return dto;
    }

    private void recordMortalityEvent(
            Livestock livestock,
            Integer count,
            LocalDate mortalityDate,
            String note,
            String actor
    ) {
        if (livestock == null || count == null || count <= 0) return;

        LivestockMortalityRecord record = new LivestockMortalityRecord();
        record.setTenantId(livestock.getTenantId());
        record.setLivestock(livestock);
        record.setCount(count);
        record.setMortalityDate(mortalityDate != null ? mortalityDate : LocalDate.now());
        record.setNote(note);
        record.setCreatedBy(actor);
        record.setUpdatedBy(actor);
        mortalityRecordRepository.save(record);
    }

    private String livestockTargetName(Livestock livestock) {
        String batchName = livestock != null ? livestock.getBatchName() : null;
        return batchName != null && !batchName.isBlank() ? batchName.trim() : "Livestock record";
    }

    private String livestockSummary(Livestock livestock) {
        if (livestock == null) {
            return "";
        }
        return String.format(
                "%s • Stock %s • Mortality %s • Type %s",
                livestockTargetName(livestock),
                Optional.ofNullable(livestock.getCurrentStock()).orElse(0),
                Optional.ofNullable(livestock.getMortality()).orElse(0),
                livestock.getType() != null ? livestock.getType().name() : "UNKNOWN"
        );
    }

    private Map<Long, MortalitySnapshot> buildLivestockMortalitySnapshots(List<LivestockMortalityRecord> records) {
        LocalDate weekStart = weekStart();
        LocalDate monthStart = monthStart();
        Map<Long, Integer> weekly = new HashMap<>();
        Map<Long, Integer> monthly = new HashMap<>();
        Map<Long, LocalDate> lastDates = new HashMap<>();

        for (LivestockMortalityRecord record : records) {
            if (record == null || record.getLivestock() == null) continue;
            Long livestockId = record.getLivestock().getId();
            if (livestockId == null) continue;

            int count = Optional.ofNullable(record.getCount()).orElse(0);
            LocalDate mortalityDate = resolveMortalityDate(record.getMortalityDate(), record.getCreatedAt());
            if (count <= 0 || mortalityDate == null) continue;

            if (!mortalityDate.isBefore(weekStart)) {
                weekly.merge(livestockId, count, Integer::sum);
            }
            if (!mortalityDate.isBefore(monthStart)) {
                monthly.merge(livestockId, count, Integer::sum);
            }

            LocalDate previousDate = lastDates.get(livestockId);
            if (previousDate == null || mortalityDate.isAfter(previousDate)) {
                lastDates.put(livestockId, mortalityDate);
            }
        }

        Set<Long> ids = new HashSet<>();
        ids.addAll(weekly.keySet());
        ids.addAll(monthly.keySet());
        ids.addAll(lastDates.keySet());

        Map<Long, MortalitySnapshot> snapshots = new HashMap<>();
        ids.forEach(id -> snapshots.put(
                id,
                new MortalitySnapshot(
                        weekly.getOrDefault(id, 0),
                        monthly.getOrDefault(id, 0),
                        lastDates.get(id)
                )
        ));
        return snapshots;
    }

    private int sumMortalitySince(List<LivestockMortalityRecord> records, LocalDate startDate) {
        return records.stream()
                .filter(Objects::nonNull)
                .filter(record -> {
                    LocalDate mortalityDate = resolveMortalityDate(record.getMortalityDate(), record.getCreatedAt());
                    return mortalityDate != null && !mortalityDate.isBefore(startDate);
                })
                .mapToInt(record -> Optional.ofNullable(record.getCount()).orElse(0))
                .sum();
    }

    private int calculateAgeWeeks(Livestock livestock) {
        int starting = livestock.getStartingAgeInWeeks() != 0 ? livestock.getStartingAgeInWeeks() : 0;
        if (livestock.getArrivalDate() == null) return starting;

        long weeksSinceArrival = ChronoUnit.WEEKS.between(livestock.getArrivalDate(), LocalDate.now());
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

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Map<String, Object> attention(String code, String message) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("message", message);
        return item;
    }

    private LocalDate weekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate monthStart() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate resolveMortalityDate(LocalDate mortalityDate, LocalDateTime createdAt) {
        if (mortalityDate != null) return mortalityDate;
        return createdAt != null ? createdAt.toLocalDate() : null;
    }

    private String formatCount(Integer value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Optional.ofNullable(value).orElse(0));
    }

    private record MortalitySnapshot(Integer weeklyCount, Integer monthlyCount, LocalDate lastMortalityDate) {
        private static final MortalitySnapshot EMPTY = new MortalitySnapshot(0, 0, null);
    }
}
