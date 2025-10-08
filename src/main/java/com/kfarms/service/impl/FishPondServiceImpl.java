package com.kfarms.service.impl;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;
import com.kfarms.entity.FishFeedingSchedule;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.FishPondStatus;
import com.kfarms.entity.FishPondType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishPondMapper;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishPondService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FishPondServiceImpl implements FishPondService {
    private final FishPondRepository repo;
    public FishPondServiceImpl(FishPondRepository repo){this.repo = repo;}

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
            LocalDate lastWaterChange
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<FishPond> spec = (root, query, cb) ->  {
            List<Predicate> predicates = new ArrayList<>();

            // Filter: pondName (case-insensitive contains)
            if (pondName != null && !pondName.isBlank()) {
                try {
                    predicates.add(cb.like(cb.lower(root.get("pondName")), "%" + pondName.toLowerCase() + "%"));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond status: " + status);
                }
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
                predicates.add(cb.equal(root.get("status"), FishPondStatus.valueOf(status.toUpperCase())));
            }
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
        Optional<FishPond> fishPond = repo.findById(id);
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
            entity.setMortalityCount(entity.getMortalityCount() + mortality);
            entity.setCurrentStock(Math.max(entity.getCurrentStock() - mortality, 0));
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
    public void delete(Long id){
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("FishPond", "id", id);
        }
        repo.deleteById(id);
    }

    // SUMMARY - Dashboard, Report and Analysis
    @Override
    public Map<String, Object> getSummary() {
        List<FishPond> all = repo.findAll();
        Map<String, Object> summary = new HashMap<>();

        // Total FishPond record
        summary.put("totalFishPonds", all.size());
        // Total quantity
        summary.put("totalFishes", all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCurrentStock()).orElse(0))
                .sum());

        // Total capacity
        summary.put("totalQuantity", all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getCapacity()).orElse(0))
                .sum());

        // Total Mortality
        summary.put("totalMortality", all.stream()
                .mapToInt(f -> Optional.ofNullable(f.getMortalityCount()).orElse(0))
                .sum());

        // status breakdown
        Map<String, Long> countByStatus = all.stream()
                        .filter(f -> f.getStatus() != null)
                        .collect(Collectors.groupingBy(f -> f.getStatus().name(), Collectors.counting()));
        summary.put("countByStatus", countByStatus);

        // total by pondType
        Map<String, Long> countByType = all.stream()
                        .filter(f -> f.getPondType() != null)
                                .collect(Collectors.groupingBy(f -> f.getPondType().name(), Collectors.counting()));
        summary.put("countByPondType", countByType);

        // last water changed (latest)
        all.stream()
                .map(FishPond::getLastWaterChange)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .ifPresent(last -> summary.put("lastUpdated", last));
        return summary;
    }

    // ADJUST stock
    @Override
    public FishPondResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        FishPond pond = repo.findById(id)
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
