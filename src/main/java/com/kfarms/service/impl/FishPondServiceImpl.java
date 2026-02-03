package com.kfarms.service.impl;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.*;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishPondMapper;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishPondService;
import com.kfarms.service.NotificationService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FishPondServiceImpl implements FishPondService {

    private final FishPondRepository repo;
    private final NotificationService notification;

    // CREATE - add new fishPond
    public FishPondResponseDto create(FishPondRequestDto dto){
        FishPond entity = FishPondMapper.toEntity(dto);
        FishPond saved = repo.save(entity);

        // calculate nextWaterChange before returning response
        FishPondResponseDto response = FishPondMapper.toResponseDto(saved);
        response.setNextWaterChange(calculateNextWaterChange(saved));
        return response;
    }

    // READ - get all fishPond with filtering and pagination
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
        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "id");

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<FishPond> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // deleted filter (default: show non-deleted)
            if (Boolean.TRUE.equals(deleted)) {
                predicates.add(cb.isTrue(root.get("deleted")));
            } else {
                predicates.add(cb.isFalse(root.get("deleted")));
            }

            // Filter: pondName (case-insensitive contains)
            if (pondName != null && !pondName.isBlank()) {
                predicates.add(
                        cb.like(cb.lower(root.get("pondName")), "%" + pondName.trim().toLowerCase() + "%")
                );
            }

            // Filter: pondType (enum)
            if (pondType != null && !pondType.isBlank()) {
                try {
                    FishPondType typeEnum = FishPondType.valueOf(pondType.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("pondType"), typeEnum));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond type: " + pondType);
                }
            }

            // Filter: status (enum)
            if (status != null && !status.isBlank()) {
                try {
                    FishPondStatus statusEnum = FishPondStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond status: " + status);
                }
            }

            // Filter: lastWaterChange
            if (lastWaterChange != null) {
                predicates.add(cb.equal(root.get("lastWaterChange"), lastWaterChange));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FishPond> fishPondPage = repo.findAll(spec, pageable);

        List<FishPondResponseDto> items = fishPondPage.getContent().stream()
                .map(f -> {
                    FishPondResponseDto dto = FishPondMapper.toResponseDto(f);
                    dto.setNextWaterChange(calculateNextWaterChange(f));
                    return dto;
                })
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

    // READ - get fishPond by ID
    @Override
    public FishPondResponseDto getById(Long id){
        Optional<FishPond> fishPond = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()));


        return fishPond.map(f -> {
            FishPondResponseDto dto = FishPondMapper.toResponseDto(f);
            // set nextWaterChange when fetching by ID
            dto.setNextWaterChange(calculateNextWaterChange(f));
            return dto;
        }).orElse(null);
    }

    // UPDATE - update existing fishPond by ID
    @Override
    public FishPondResponseDto update(Long id, FishPondRequestDto request, String updatedBy){
        FishPond entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        // Update only non-null fields
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

        // Handle mortality -> automatically reduce stock
        if (request.getMortalityCount() != null) {
            int mortality = request.getMortalityCount();

            int existingMortality = Optional
                    .ofNullable(entity.getMortalityCount())
                    .orElse(0);

            int existingStock = Optional
                    .ofNullable(entity.getCurrentStock())
                    .orElse(0);

            entity.setMortalityCount(existingMortality + mortality);
            entity.setCurrentStock(Math.max(existingStock - mortality, 0));
        }


        // Update current stock only if explicitly sent (and not already adjusted by mortality
        if (request.getCurrentStock() != null && request.getMortalityCount() ==  null) {
            entity.setCurrentStock(request.getCurrentStock());
        }

        // handle enums safely
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

        // auditing
        entity.setUpdatedBy(updatedBy);

        FishPond saved = repo.save(entity);

        // Attach nextWaterChange
        FishPondResponseDto response = FishPondMapper.toResponseDto(saved);
        response.setNextWaterChange(calculateNextWaterChange(saved));
        return response;

    }

    // DELETE - delete existing entity by ID
    public void delete(Long id, String deletedBy){
        FishPond entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fishpond with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }


    // DELETE (permanent)
    @Override
    public void permanentDelete(Long id, String deletedBy) {
        FishPond entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));
        repo.delete(entity);
    }

    // RESTORE
    public void restore(Long id) {
        FishPond entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("FishPond record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // SUMMARY - Dashboard, Report and Analysis
    @Override
    public Map<String, Object> getSummary() {
        List<FishPond> all = repo.findAll()
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .toList();

        Map<String, Object> summary = new HashMap<>();

        // Total quantity
        int totalFishes = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCurrentStock()).orElse(0))
                .sum();

        // Total capacity
        int totalQuantity = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCapacity()).orElse(0))
                .sum();

        // Total empty ponds
        long emptyPonds = all.stream()
                .filter(f -> Optional.ofNullable(f.getCurrentStock()).orElse(0) == 0)
                .count();
        summary.put("totalEmptyPonds", emptyPonds);

        // Total Mortality
        int totalMortality = all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getMortalityCount()).orElse(0))
                .sum();

        // status breakdown
        Map<String, Long> countByStatus = all.stream()
                .filter(f -> f.getStatus() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getStatus().name(),
                        Collectors.counting()
                ));

        // total by pondType
        Map<String, Long> countByType = all.stream()
                .filter(f -> f.getPondType() != null)
                .collect(Collectors.groupingBy(
                        f -> f.getPondType().name(),
                        Collectors.counting()
                ));

        // ===================== Monthly stock totals (Jan–Dec 2026) =====================
        int year = 2026;
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        Map<String, Long> monthlyStockTotals = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyStockTotals.put(String.format("%04d-%02d", year, m), 0L);
        }

        // IMPORTANT:
        // This must come from a "stock records / movements / stocking events" table.
        // If you don’t have that table, monthly stock totals can’t be historically correct.
        // Replace fishStockRepo with your actual repo (e.g. FishPondStockingRepo, FishStockRecordRepo, etc.)
        for (Object[] row : repo.sumMonthlyStockTotals(start, end)) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            long total = row[2] == null ? 0L : ((Number) row[2]).longValue();
            monthlyStockTotals.put(String.format("%04d-%02d", y, m), total);
        }

        summary.put("monthlyStockTotals", monthlyStockTotals);

        summary.put("totalFishPonds", all.size());
        summary.put("totalFishes", totalFishes);
        summary.put("totalQuantity", totalQuantity);
        summary.put("totalMortality", totalMortality);
        summary.put("countByStatus", countByStatus);
        summary.put("countByPondType", countByType);

        // last water changed (latest)
        all.stream()
                .map(FishPond::getLastWaterChange)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastUpdated", last));

        // === 🌟 ALERT LOGIC ===
        Map<String, Object> alerts = new HashMap<>();

        // 🐟 Low Stock Alert
        if (totalFishes < 100) {
            alerts.put("fishLow", "Fish stock is below normal levels!");
            notification.createNotification(
                    "FISH",
                    "Low Fish Stock",
                    "Total fish count is below 100.",
                    null
            );
        }

        // ⚠️ High Mortality Alert
        if (totalFishes > 0) {
            double mortalityRate = (double) totalMortality / totalFishes * 100;
            if (mortalityRate > 10) {
                String msg = String.format("Mortality rate is %.2f%% — above 10%% threshold.", mortalityRate);
                alerts.put("highMortality", msg);
                notification.createNotification(
                        "FISH",
                        "High Mortality",
                        msg,
                        null
                );
            }
        }

        // 💧 Water Change Reminder
        LocalDate today = LocalDate.now();
        List<FishPond> dueForWaterChange = all.stream()
                .filter(f -> {
                    LocalDate nextChange = calculateNextWaterChange(f);
                    return nextChange != null && !nextChange.isAfter(today);
                })
                .toList();

        int dueCount = dueForWaterChange.size();
        summary.put("dueWaterChangeCount", dueCount);

        if (dueCount > 0) {
            String msg = dueCount + " pond(s) need water change.";
            alerts.put("waterChangeDue", msg);
            notification.createNotification(
                    "FISH",
                    "Water Change Due",
                    dueCount + " pond(s) require water change today or earlier.",
                    null
            );
        }

        summary.put("alerts", alerts);
        return summary;
    }

    // ADJUST stock
    @Override
    public FishPondResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        FishPond pond = repo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        pond.adjustStock(request.getQuantity(), request.getReason());
        pond.setUpdatedBy(updatedBy);

        repo.save(pond);
        return FishPondMapper.toResponseDto(pond);
    }

    // HELPER METHOD: calculate next water change dynamically
    private LocalDate calculateNextWaterChange(FishPond pond) {
        if (pond.getLastWaterChange() == null) return null;

        // smart rules
        switch (pond.getPondType()) {
            case HATCHING -> {
                return pond.getLastWaterChange().plusDays(2); // more frequent
            }
            case GROW_OUT -> {
                return pond.getLastWaterChange().plusDays(5); // less frequent
            }
            case BROODSTOCK, HOLDING -> {
                return pond.getLastWaterChange().plusDays(7); // weekly
            }
            default -> {
                return pond.getLastWaterChange().plusDays(7); // fallback
            }
        }
    }
}
