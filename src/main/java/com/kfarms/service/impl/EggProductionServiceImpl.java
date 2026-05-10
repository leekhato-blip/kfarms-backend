package com.kfarms.service.impl;

import com.kfarms.dto.EggProductionRequestDto;
import com.kfarms.dto.EggProductionResponseDto;
import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Livestock;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.EggProductionMapper;
import com.kfarms.repository.EggProductionRepo;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.service.EggProductionService;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.repository.TenantRepository;
import com.kfarms.tenant.service.TenantContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EggProductionServiceImpl implements EggProductionService {
    private final EggProductionRepo repo;
    private final LivestockRepository livestockRepo;
    private final TenantRepository tenantRepo;

    // CREATE
    @Override
    public EggProductionResponseDto create(EggProductionRequestDto request) {
        Long tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        Livestock livestock = livestockRepo.findByIdAndTenantId(request.getBatchId(), tenantId)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", request.getBatchId()));

        EggProduction entity = EggProductionMapper.toEntity(request, livestock);
        entity.setLivestock(livestock);
        entity.setTenant(tenant);
        entity.setNote(trimToNull(request.getNote()));
        repo.save(entity);

        return EggProductionMapper.toResponseDto(entity);
    }

    // READ - all eggs (pagination + filter)
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAll(int page, int size, Long batchId, LocalDate collectionDate, Boolean deleted) {
        Sort sort = Boolean.TRUE.equals(deleted)
                ? Sort.by(Sort.Direction.DESC, "deletedAt").and(Sort.by(Sort.Direction.DESC, "id"))
                : Sort.by(Sort.Direction.DESC, "collectionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);
        Long tenantId = TenantContext.getTenantId();

        Specification<EggProduction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> livestockJoin = root.join("livestock", JoinType.LEFT);
            Join<Object, Object> tenantJoin = root.join("tenant", JoinType.LEFT);

            predicates.add(cb.or(
                    cb.equal(tenantJoin.get("id"), tenantId),
                    cb.and(
                            cb.isNull(root.get("tenant")),
                            cb.equal(livestockJoin.get("tenantId"), tenantId)
                    )
            ));
            predicates.add(cb.equal(root.get("deleted"), Boolean.TRUE.equals(deleted)));

            if (batchId != null) {
                predicates.add(cb.equal(livestockJoin.get("id"), batchId));
            }
            if (collectionDate != null) {
                predicates.add(cb.equal(root.get("collectionDate"), collectionDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<EggProduction> pageResult = repo.findAll(spec, pageable);
        List<EggProductionResponseDto> items = pageResult.getContent().stream()
                .map(EggProductionMapper::toResponseDto)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("page", pageResult.getNumber());
        result.put("size", pageResult.getSize());
        result.put("totalItems", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("hasNext", pageResult.hasNext());
        result.put("hasPrevious", pageResult.hasPrevious());

        return result;
    }

    // READ - by ID
    @Override
    @Transactional(readOnly = true)
    public EggProductionResponseDto getById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        EggProduction entity = repo.findVisibleByIdAndTenantId(id, tenantId)
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));
        return EggProductionMapper.toResponseDto(entity);
    }

    // UPDATE
    @Override
    public EggProductionResponseDto update(Long id, EggProductionRequestDto request, String updatedBy) {
        Long tenantId = TenantContext.getTenantId();
        EggProduction entity = repo.findVisibleByIdAndTenantId(id, tenantId)
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));
        attachTenantIfMissing(entity, tenantId);

        if (request.getBatchId() != null) {
            Livestock livestock = livestockRepo.findByIdAndTenantId(request.getBatchId(), tenantId)
                    .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                    .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", request.getBatchId()));
            entity.setLivestock(livestock);
        }

        entity.setGoodEggs(request.getGoodEggs());
        entity.setDamagedEggs(request.getDamagedEggs());
        if (request.getCollectionDate() != null) entity.setCollectionDate(request.getCollectionDate());
        entity.setNote(trimToNull(request.getNote()));
        entity.calculateCrates();

        entity.setUpdatedBy(updatedBy);
        repo.save(entity);
        return EggProductionMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    public void delete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        EggProduction entity = repo.findVisibleByIdAndTenantId(id, tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException("EggProduction", "id", id));
        attachTenantIfMissing(entity, tenantId);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Egg Record with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    @Override
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = TenantContext.getTenantId();
        EggProduction entity = repo.findVisibleByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EggProduction", "id", id));
        attachTenantIfMissing(entity, tenantId);

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Egg record with ID " + id + " must be moved to trash before permanent delete");
        }

        repo.save(entity);
        int deletedCount = repo.hardDeleteByIdAndTenantId(id, tenantId);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("EggProduction", "id", id);
        }
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Long tenantId = TenantContext.getTenantId();
        EggProduction entity = repo.findVisibleByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("EggProduction", "id", id));
        attachTenantIfMissing(entity, tenantId);

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Egg record with ID " + id + " has already been restored");
        }
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // SUMMARY
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {

        Long tenantId = TenantContext.getTenantId();

        List<EggProduction> all = repo.findAllActiveVisibleToTenant(tenantId);
        List<EggProduction> datedRecords = all.stream()
                .filter(e -> e.getCollectionDate() != null)
                .toList();

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        int totalGood = all.stream().mapToInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0).sum();
        int totalDamaged = all.stream().mapToInt(e -> e.getDamagedEggs() != 0 ? e.getDamagedEggs() : 0).sum();
        int totalCrates = totalGood / 30;


        // Group by livestock batch
        Map<String, Integer> countByBatch = all.stream()
                .filter(e -> e.getLivestock() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getLivestock().getBatchName(),
                        Collectors.summingInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0)
                ));
        Map<String, Integer> orderedCountByBatch = countByBatch.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<EggProduction> currentYearRecords = datedRecords.stream()
                .filter(e -> e.getCollectionDate().getYear() == currentYear)
                .toList();

        // Monthly Summary for the current year only
        Map<String, Integer> monthlyProduction = currentYearRecords.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCollectionDate().getYear() + "-" + String.format("%02d", e.getCollectionDate().getMonthValue()),
                        Collectors.summingInt(EggProduction::getGoodEggs) // sum of good eggs
                ));
        Map<String, Integer> orderedMonthlyProduction = monthlyProduction.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));


        List<EggProduction> monthly = datedRecords.stream()
                .filter(e -> e.getCollectionDate() != null
                        && e.getCollectionDate().getMonthValue() == currentMonth
                        && e.getCollectionDate().getYear() == currentYear)
                .toList();

        int monthlyGood = monthly.stream().mapToInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0).sum();
        int monthlyDamaged = monthly.stream().mapToInt(e -> e.getDamagedEggs() != 0 ? e.getDamagedEggs() : 0).sum();
        int monthlyCrates = monthlyGood / 30;
        int todayGood = all.stream()
                .filter(e -> now.equals(e.getCollectionDate()))
                .mapToInt(EggProduction::getGoodEggs)
                .sum();
        int todayDamaged = all.stream()
                .filter(e -> now.equals(e.getCollectionDate()))
                .mapToInt(EggProduction::getDamagedEggs)
                .sum();
        int todayCrates = all.stream()
                .filter(e -> now.equals(e.getCollectionDate()))
                .mapToInt(EggProduction::getCratesProduced)
                .sum();
        LocalDate lastCollectionDate = datedRecords.stream()
                .map(EggProduction::getCollectionDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        Map.Entry<String, Integer> topBatch = orderedCountByBatch.entrySet().stream().findFirst().orElse(null);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRecords", all.size());
        summary.put("activeBatchCount", orderedCountByBatch.size());
        summary.put("totalGoodEggs", totalGood);
        summary.put("totalCracked", totalDamaged);
        summary.put("totalCratesProduced", totalCrates);
        summary.put("todayGoodEggs", todayGood);
        summary.put("todayCracked", todayDamaged);
        summary.put("todayCratesProduced", todayCrates);
        summary.put("monthlyGoodEggs", monthlyGood);
        summary.put("monthlyCracked", monthlyDamaged);
        summary.put("monthlyCratesProduced", monthlyCrates);
        summary.put("lastCollectionDate", lastCollectionDate);
        summary.put("averageGoodEggsPerRecord", all.isEmpty() ? 0 : totalGood / all.size());
        summary.put("countByBatch", orderedCountByBatch);
        summary.put("productionYear", currentYear);
        summary.put("MonthlyProduction", orderedMonthlyProduction);
        summary.put("monthlyProduction", orderedMonthlyProduction);
        summary.put("topBatch", topBatch == null
                ? null
                : Map.of(
                        "name", topBatch.getKey(),
                        "goodEggs", topBatch.getValue()
                ));

        return summary;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void attachTenantIfMissing(EggProduction entity, Long tenantId) {
        if (entity.getTenant() != null) {
            return;
        }

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
        entity.setTenant(tenant);
    }
}
