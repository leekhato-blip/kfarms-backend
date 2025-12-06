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
import com.kfarms.service.NotificationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EggProductionServiceImpl implements EggProductionService {
    private final EggProductionRepo repo;
    private final LivestockRepository livestockRepo;
    private final NotificationService notification;

    // CREATE
    @Override
    public EggProductionResponseDto create(EggProductionRequestDto request) {
        Livestock livestock = livestockRepo.findById(request.getBatchId())
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", request.getBatchId()));

        EggProduction entity = EggProductionMapper.toEntity(request, livestock);
        entity.setLivestock(livestock);
        repo.save(entity);

        return EggProductionMapper.toResponseDto(entity);
    }

    // READ - all eggs (pagination + filter)
    @Override
    public Map<String, Object> getAll(int page, int size, Long batchId, LocalDate collectionDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("collectionDate").descending());

        Specification<EggProduction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (batchId != null) {
                predicates.add(cb.equal(root.get("livestock").get("id"), batchId));
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
    public EggProductionResponseDto getById(Long id) {
        EggProduction entity = repo.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));
        return EggProductionMapper.toResponseDto(entity);
    }

    // UPDATE
    @Override
    public EggProductionResponseDto update(Long id, EggProductionRequestDto request, String updatedBy) {
        EggProduction entity = repo.findById(id)
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));

        if (request.getGoodEggs() != 0) entity.setGoodEggs(request.getGoodEggs());
        if (request.getDamagedEggs() != 0) entity.setDamagedEggs(request.getDamagedEggs());
        if (request.getCollectionDate() != null) entity.setCollectionDate(request.getCollectionDate());
        if (request.getNote() != null) entity.setNote(request.getNote());

        entity.setUpdatedBy(updatedBy);
        repo.save(entity);
        return EggProductionMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    public void delete(Long id, String deletedBy) {
        EggProduction entity = repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("EggProduction", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Egg Record with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        EggProduction entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EggProduction", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Egg record with ID " + id + " has already been restored");
        }
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        repo.save(entity);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<EggProduction> all = repo.findAll()
                .stream()
                .filter(e -> !Boolean.TRUE.equals(e.getDeleted()))
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

        // Monthly Summary
        Map<String, Integer> monthlyProduction = all.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCollectionDate().getYear() + "-" + String.format("%02d", e.getCollectionDate().getMonthValue()),
                        Collectors.summingInt(EggProduction::getGoodEggs) // sum of good eggs
                ));


        List<EggProduction> monthly = all.stream()
                .filter(e -> e.getCollectionDate() != null
                        && e.getCollectionDate().getMonthValue() == currentMonth
                        && e.getCollectionDate().getYear() == currentYear)
                .toList();

        int monthlyGood = monthly.stream().mapToInt(e -> e.getGoodEggs() != 0 ? e.getGoodEggs() : 0).sum();
        int monthlyDamaged = monthly.stream().mapToInt(e -> e.getDamagedEggs() != 0 ? e.getDamagedEggs() : 0).sum();
        int monthlyCrates = monthlyGood / 30;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGoodEggs", totalGood);
        summary.put("totalCracked", totalDamaged);
        summary.put("totalCratesProduced", totalCrates);
        summary.put("monthlyGoodEggs", monthlyGood);
        summary.put("monthlyCracked", monthlyDamaged);
        summary.put("monthlyCratesProduced", monthlyCrates);
        summary.put("countByBatch", countByBatch);
        summary.put("MonthlyProduction", monthlyProduction);

        // ==== NOTIFICATION ====
        if (monthlyCrates < 5) {
            notification.createNotification(
                    "LAYER",
                    "Low Egg production",
                    "Egg production this month is below expected levels.",
                    null
            );
        }

        if (monthlyDamaged > (monthlyGood * 0.2)) {
            notification.createNotification(
                    "LAYER",
                    "High Damaged Eggs",
                    "More than 20% of eggs collected this month are damaged.",
                    null
            );
        }

        if (monthly.isEmpty()) {
            notification.createNotification(
                    "LAYER",
                    "No Egg Records",
                    "No egg production recorded for this month yet.",
                    null
            );
        }

        return summary;
    }
}
