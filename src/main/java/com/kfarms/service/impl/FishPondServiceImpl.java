package com.kfarms.service.impl;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.dto.MortalityRecordRequestDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.*;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishPondMapper;
import com.kfarms.repository.FishPondMortalityRecordRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishPondService;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FishPondServiceImpl implements FishPondService {

    private final FishPondRepository repo;
    private final FishPondMortalityRecordRepository mortalityRecordRepository;
    private final NotificationService notification;
    private final TenantPlanGuardService planGuardService;
    private final TenantRecordAuditService tenantRecordAuditService;

    @Override
    public FishPondResponseDto create(FishPondRequestDto dto) {
        Tenant tenant = planGuardService.requireCurrentTenant();
        long activePonds = repo.countActiveByTenantId(tenant.getId());
        int maxFishPonds = planGuardService.maxFishPondsForPlan(tenant.getPlan());
        if (maxFishPonds != Integer.MAX_VALUE && activePonds >= maxFishPonds) {
            throw new IllegalArgumentException(
                    "Fish pond limit reached for the " + tenant.getPlan().name() + " plan."
            );
        }

        FishPond entity = FishPondMapper.toEntity(dto);
        entity.setTenant(tenant);
        FishPond saved = repo.save(entity);
        tenantRecordAuditService.created(
                tenant.getId(),
                entity.getCreatedBy(),
                "FISH_POND",
                saved.getId(),
                fishPondTargetName(saved),
                fishPondSummary(saved),
                "Created fish pond record for " + fishPondTargetName(saved) + "."
        );
        return toResponseDto(saved, null);
    }

    @Override
    public Map<String, Object> getAll(
            int page,
            int size,
            String pondName,
            String pondType,
            String status,
            LocalDate lastWaterChange,
            Boolean deleted
    ) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context");
        }

        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<FishPond> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));

            if (Boolean.TRUE.equals(deleted)) {
                predicates.add(cb.isTrue(root.get("deleted")));
            } else {
                predicates.add(cb.isFalse(root.get("deleted")));
            }

            if (pondName != null && !pondName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("pondName")), "%" + pondName.trim().toLowerCase() + "%"));
            }
            if (pondType != null && !pondType.isBlank()) {
                try {
                    FishPondType typeEnum = FishPondType.valueOf(pondType.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("pondType"), typeEnum));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond type: " + pondType);
                }
            }
            if (status != null && !status.isBlank()) {
                try {
                    FishPondStatus statusEnum = FishPondStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond status: " + status);
                }
            }
            if (lastWaterChange != null) {
                predicates.add(cb.equal(root.get("lastWaterChange"), lastWaterChange));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FishPond> fishPondPage = repo.findAll(spec, pageable);
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildMortalitySnapshots(
                mortalityRecordRepository.findAllActiveByTenantId(tenantId)
        );

        List<FishPondResponseDto> items = fishPondPage.getContent().stream()
                .map(pond -> toResponseDto(pond, mortalitySnapshots.get(pond.getId())))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("totalItems", fishPondPage.getTotalElements());
        result.put("totalPages", fishPondPage.getTotalPages());
        result.put("hasNext", fishPondPage.hasNext());
        result.put("hasPrevious", fishPondPage.hasPrevious());
        return result;
    }

    @Override
    public FishPondResponseDto getById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Optional<FishPond> fishPond = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()));
        return fishPond.map(pond -> buildResponseForTenant(tenantId, pond)).orElse(null);
    }

    @Override
    public FishPondResponseDto update(Long id, FishPondRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        FishPond entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        String previousSummary = fishPondSummary(entity);

        if (request.getPondName() != null) {
            entity.setPondName(request.getPondName());
        }
        if (request.getCapacity() != null) {
            entity.setCapacity(request.getCapacity());
        }
        if (request.getLastWaterChange() != null) {
            entity.setLastWaterChange(request.getLastWaterChange());
        }
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }

        if (request.getMortalityCount() != null && request.getMortalityCount() > 0) {
            applyMortality(entity, request.getMortalityCount());
        }

        if (request.getCurrentStock() != null && (request.getMortalityCount() == null || request.getMortalityCount() == 0)) {
            entity.setCurrentStock(request.getCurrentStock());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(FishPondStatus.valueOf(request.getStatus().trim().toUpperCase()));
        }
        if (request.getFeedingSchedule() != null && !request.getFeedingSchedule().isBlank()) {
            entity.setFeedingSchedule(
                    Enum.valueOf(FishFeedingSchedule.class, request.getFeedingSchedule().trim().toUpperCase())
            );
        }
        if (request.getPondType() != null && !request.getPondType().isBlank()) {
            entity.setPondType(
                    Enum.valueOf(FishPondType.class, request.getPondType().trim().toUpperCase())
            );
        }

        entity.setUpdatedBy(updatedBy);
        FishPond saved = repo.save(entity);

        if (request.getMortalityCount() != null && request.getMortalityCount() > 0) {
            recordMortalityEvent(
                    saved,
                    request.getMortalityCount(),
                    LocalDate.now(),
                    request.getNote(),
                    updatedBy
            );
        }

        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "FISH_POND",
                saved.getId(),
                fishPondTargetName(saved),
                previousSummary,
                fishPondSummary(saved),
                "Updated fish pond record for " + fishPondTargetName(saved) + "."
        );

        return buildResponseForTenant(tenantId, saved);
    }

    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        FishPond entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        String previousSummary = fishPondSummary(entity);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fishpond with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
        tenantRecordAuditService.deleted(
                tenantId,
                deletedBy,
                "FISH_POND",
                entity.getId(),
                fishPondTargetName(entity),
                previousSummary,
                "Deleted fish pond record for " + fishPondTargetName(entity) + "."
        );
    }

    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        FishPond entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        repo.delete(entity);
    }

    @Override
    public void restore(Long id) {
        Long tenantId = TenantContext.getTenantId();
        FishPond entity = repo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("FishPond record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    @Override
    public Map<String, Object> getSummary() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Missing tenant context");
        }

        List<FishPond> all = repo.findAllActiveByTenantId(tenantId);
        List<FishPondMortalityRecord> mortalityRecords = mortalityRecordRepository.findAllActiveByTenantId(tenantId);
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildMortalitySnapshots(mortalityRecords);

        Map<String, Object> summary = new HashMap<>();

        int totalFishes = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCurrentStock()).orElse(0))
                .sum();
        int totalQuantity = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCapacity()).orElse(0))
                .sum();
        long emptyPonds = all.stream()
                .filter(f -> Optional.ofNullable(f.getCurrentStock()).orElse(0) == 0)
                .count();
        int totalMortality = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getMortalityCount()).orElse(0))
                .sum();

        Map<String, Long> countByStatus = all.stream()
                .filter(f -> f.getStatus() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Long> countByType = all.stream()
                .filter(f -> f.getPondType() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getPondType().name(),
                        Collectors.counting()
                ));

        int year = 2026;
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Map<String, Long> monthlyStockTotals = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyStockTotals.put(String.format("%04d-%02d", year, month), 0L);
        }
        for (Object[] row : repo.sumMonthlyStockTotalsByTenant(tenantId, start, end)) {
            int rowYear = ((Number) row[0]).intValue();
            int rowMonth = ((Number) row[1]).intValue();
            long total = row[2] == null ? 0L : ((Number) row[2]).longValue();
            monthlyStockTotals.put(String.format("%04d-%02d", rowYear, rowMonth), total);
        }

        List<Map<String, Object>> pondCards = all.stream()
                .sorted(Comparator.comparing(FishPond::getPondName, String.CASE_INSENSITIVE_ORDER))
                .map(pond -> {
                    MortalitySnapshot snapshot = mortalitySnapshots.getOrDefault(pond.getId(), MortalitySnapshot.EMPTY);
                    Map<String, Object> card = new LinkedHashMap<>();
                    card.put("id", pond.getId());
                    card.put("pondName", pond.getPondName());
                    card.put("pondType", pond.getPondType() != null ? pond.getPondType().name() : null);
                    card.put("status", pond.getStatus() != null ? pond.getStatus().name() : null);
                    card.put("currentStock", Optional.ofNullable(pond.getCurrentStock()).orElse(0));
                    card.put("capacity", Optional.ofNullable(pond.getCapacity()).orElse(0));
                    card.put("mortalityTotal", Optional.ofNullable(pond.getMortalityCount()).orElse(0));
                    card.put("mortalityThisWeek", snapshot.weeklyCount());
                    card.put("mortalityThisMonth", snapshot.monthlyCount());
                    card.put("lastMortalityDate", snapshot.lastMortalityDate());
                    card.put("nextWaterChange", calculateNextWaterChange(pond));
                    return card;
                })
                .toList();

        summary.put("totalEmptyPonds", emptyPonds);
        summary.put("monthlyStockTotals", monthlyStockTotals);
        summary.put("totalFishPonds", all.size());
        summary.put("totalFishes", totalFishes);
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalMortality", totalMortality);
        summary.put("weeklyMortality", sumMortalitySince(mortalityRecords, weekStart()));
        summary.put("monthlyMortality", sumMortalitySince(mortalityRecords, monthStart()));
        summary.put("countByStatus", countByStatus);
        summary.put("countByPondType", countByType);
        summary.put("pondCards", pondCards);

        all.stream()
                .map(FishPond::getLastWaterChange)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastUpdated", last));

        Map<String, Object> alerts = new HashMap<>();
        if (totalFishes < 100) {
            alerts.put("fishLow", "Fish stock is below normal levels!");
            notification.createNotification(
                    tenantId,
                    "FISH",
                    "Low Fish Stock",
                    "Total fish count is below 100.",
                    null
            );
        }

        if (totalFishes > 0) {
            double mortalityRate = (double) totalMortality / totalFishes * 100;
            if (mortalityRate > 10) {
                String message = String.format("Mortality rate is %.2f%% — above 10%% threshold.", mortalityRate);
                alerts.put("highMortality", message);
                notification.createNotification(
                        tenantId,
                        "FISH",
                        "High Mortality",
                        message,
                        null
                );
            }
        }

        LocalDate today = LocalDate.now();
        List<FishPond> dueForWaterChange = all.stream()
                .filter(pond -> {
                    LocalDate nextChange = calculateNextWaterChange(pond);
                    return nextChange != null && !nextChange.isAfter(today);
                })
                .toList();

        int dueCount = dueForWaterChange.size();
        summary.put("dueWaterChangeCount", dueCount);
        if (dueCount > 0) {
            alerts.put("waterChangeDue", dueCount + " pond(s) need water change.");
            notification.createNotification(
                    tenantId,
                    "FISH",
                    "Water Change Due",
                    dueCount + " pond(s) require water change today or earlier.",
                    null
            );
        }

        summary.put("alerts", alerts);
        return summary;
    }

    @Override
    public FishPondResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        FishPond pond = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        String previousSummary = fishPondSummary(pond);

        pond.adjustStock(request.getQuantity(), request.getReason());
        pond.setUpdatedBy(updatedBy);
        repo.save(pond);
        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "FISH_POND",
                pond.getId(),
                fishPondTargetName(pond),
                previousSummary,
                fishPondSummary(pond),
                "Adjusted fish pond stock for " + fishPondTargetName(pond) + "."
        );
        return buildResponseForTenant(tenantId, pond);
    }

    @Override
    public FishPondResponseDto recordMortality(Long id, MortalityRecordRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        FishPond pond = repo.findByIdAndTenant_Id(id, tenantId)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        String previousSummary = fishPondSummary(pond);

        int count = Optional.ofNullable(request.getCount()).orElse(0);
        if (count <= 0) {
            throw new IllegalArgumentException("Mortality count must be greater than 0");
        }

        applyMortality(pond, count);
        pond.setUpdatedBy(updatedBy);
        repo.save(pond);

        recordMortalityEvent(
                pond,
                count,
                request.getMortalityDate() != null ? request.getMortalityDate() : LocalDate.now(),
                request.getNote(),
                updatedBy
        );

        tenantRecordAuditService.updated(
                tenantId,
                updatedBy,
                "FISH_POND",
                pond.getId(),
                fishPondTargetName(pond),
                previousSummary,
                fishPondSummary(pond),
                "Recorded fish pond mortality for " + fishPondTargetName(pond) + "."
        );

        return buildResponseForTenant(tenantId, pond);
    }

    private void applyMortality(FishPond pond, int count) {
        int currentStock = Optional.ofNullable(pond.getCurrentStock()).orElse(0);
        if (count > currentStock) {
            throw new IllegalArgumentException("Mortality cannot exceed current fish stock");
        }

        int currentMortality = Optional.ofNullable(pond.getMortalityCount()).orElse(0);
        pond.setMortalityCount(currentMortality + count);
        pond.setCurrentStock(currentStock - count);
    }

    private FishPondResponseDto buildResponseForTenant(Long tenantId, FishPond pond) {
        Map<Long, MortalitySnapshot> mortalitySnapshots = buildMortalitySnapshots(
                mortalityRecordRepository.findAllActiveByTenantId(tenantId)
        );
        return toResponseDto(pond, mortalitySnapshots.get(pond.getId()));
    }

    private FishPondResponseDto toResponseDto(FishPond pond, MortalitySnapshot snapshot) {
        FishPondResponseDto dto = FishPondMapper.toResponseDto(pond);
        dto.setNextWaterChange(calculateNextWaterChange(pond));
        MortalitySnapshot safeSnapshot = snapshot != null ? snapshot : MortalitySnapshot.EMPTY;
        dto.setMortalityThisWeek(safeSnapshot.weeklyCount());
        dto.setMortalityThisMonth(safeSnapshot.monthlyCount());
        dto.setLastMortalityDate(safeSnapshot.lastMortalityDate());
        return dto;
    }

    private void recordMortalityEvent(
            FishPond pond,
            Integer count,
            LocalDate mortalityDate,
            String note,
            String actor
    ) {
        if (pond == null || count == null || count <= 0) return;

        FishPondMortalityRecord record = new FishPondMortalityRecord();
        record.setPond(pond);
        record.setCount(count);
        record.setMortalityDate(mortalityDate != null ? mortalityDate : LocalDate.now());
        record.setNote(note);
        record.setCreatedBy(actor);
        record.setUpdatedBy(actor);
        mortalityRecordRepository.save(record);
    }

    private String fishPondTargetName(FishPond pond) {
        String pondName = pond != null ? pond.getPondName() : null;
        return pondName != null && !pondName.isBlank() ? pondName.trim() : "Fish pond record";
    }

    private String fishPondSummary(FishPond pond) {
        if (pond == null) {
            return "";
        }
        return String.format(
                "%s • Stock %s • Mortality %s • Type %s",
                fishPondTargetName(pond),
                Optional.ofNullable(pond.getCurrentStock()).orElse(0),
                Optional.ofNullable(pond.getMortalityCount()).orElse(0),
                pond.getPondType() != null ? pond.getPondType().name() : "UNKNOWN"
        );
    }

    private Map<Long, MortalitySnapshot> buildMortalitySnapshots(List<FishPondMortalityRecord> records) {
        LocalDate weekStart = weekStart();
        LocalDate monthStart = monthStart();
        Map<Long, Integer> weekly = new HashMap<>();
        Map<Long, Integer> monthly = new HashMap<>();
        Map<Long, LocalDate> lastDates = new HashMap<>();

        for (FishPondMortalityRecord record : records) {
            if (record == null || record.getPond() == null) continue;
            Long pondId = record.getPond().getId();
            if (pondId == null) continue;

            int count = Optional.ofNullable(record.getCount()).orElse(0);
            LocalDate mortalityDate = resolveMortalityDate(record.getMortalityDate(), record.getCreatedAt());
            if (count <= 0 || mortalityDate == null) continue;

            if (!mortalityDate.isBefore(weekStart)) {
                weekly.merge(pondId, count, Integer::sum);
            }
            if (!mortalityDate.isBefore(monthStart)) {
                monthly.merge(pondId, count, Integer::sum);
            }

            LocalDate previousDate = lastDates.get(pondId);
            if (previousDate == null || mortalityDate.isAfter(previousDate)) {
                lastDates.put(pondId, mortalityDate);
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

    private int sumMortalitySince(List<FishPondMortalityRecord> records, LocalDate startDate) {
        return records.stream()
                .filter(Objects::nonNull)
                .filter(record -> {
                    LocalDate mortalityDate = resolveMortalityDate(record.getMortalityDate(), record.getCreatedAt());
                    return mortalityDate != null && !mortalityDate.isBefore(startDate);
                })
                .mapToInt(record -> Optional.ofNullable(record.getCount()).orElse(0))
                .sum();
    }

    private LocalDate calculateNextWaterChange(FishPond pond) {
        if (pond.getLastWaterChange() == null) return null;

        switch (pond.getPondType()) {
            case HATCHING -> {
                return pond.getLastWaterChange().plusDays(2);
            }
            case GROW_OUT -> {
                return pond.getLastWaterChange().plusDays(5);
            }
            case BROODSTOCK, HOLDING -> {
                return pond.getLastWaterChange().plusDays(7);
            }
            default -> {
                return pond.getLastWaterChange().plusDays(7);
            }
        }
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

    private record MortalitySnapshot(Integer weeklyCount, Integer monthlyCount, LocalDate lastMortalityDate) {
        private static final MortalitySnapshot EMPTY = new MortalitySnapshot(0, 0, null);
    }
}
