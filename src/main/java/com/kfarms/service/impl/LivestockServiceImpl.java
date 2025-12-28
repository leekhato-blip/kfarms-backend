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
public class LivestockServiceImpl implements LivestockService {
    private final LivestockRepository repo;
    private final EggProductionRepo eggRepo;
    private final NotificationService notification;

    // CREATE - create Livestock
    @Override
    public  LivestockResponseDto create(LivestockRequestDto request, String createBy) {
        Livestock entity = LivestockMapper.toEntity(request);
        entity.setCreatedBy(createBy);
        repo.save(entity);
        return LivestockMapper.toResponseDto(entity);
    }

    // READ - get all Livestock (Pagination and Filtering)
    @Override
    public Map<String, Object> getAll(int page, int size, String batchName, String type, LocalDate arrivalDate){
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

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


        Specification<Livestock> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
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
        Livestock entity = repo.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        return LivestockMapper.toResponseDto(entity);
    }

    // UPDATE - update existing Livestock
    @Override
    public LivestockResponseDto update(Long id, LivestockRequestDto request, String updatedBy){
        Livestock entity = repo.findById(id)
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
        entity.setUpdatedBy(updatedBy);

        // Handle quantity & mortality smartly
        if (request.getMortality() != null && request.getMortality() > 0) {
            // NOTIFICATION
            notification.createNotification(
                    "LIVESTOCK",
                    "Mortality Recorded",
                    request.getMortality() + " deaths recorded in batch " + entity.getBatchName(),
                    null);
            int currentMortality = entity.getMortality() != null ? entity.getMortality() : 0;
            int currentQty = (entity.getCurrentStock() != null) ? entity.getCurrentStock() : 0;

            if (request.getMortality() > currentQty) {
                throw new IllegalArgumentException("Mortality cannot exceed current livestock quantity");
            }

            int newMortality = currentMortality + request.getMortality();
            entity.setMortality(newMortality);
            entity.setCurrentStock(currentQty - request.getMortality());
        } else if (request.getCurrentStock() != null) {
            // allow updating quantity directly (e.g manual correction)
            entity.setCurrentStock(request.getCurrentStock());
        }

        repo.save(entity);
        return LivestockMapper.toResponseDto(entity);
    }

    // DELETE - delete livestock by ID
    @Override
    public void delete(Long id, String deletedBy){
        Livestock entity = repo.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Livestock with ID " + id + " has already been deleted");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        repo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        Livestock entity = repo.findById(id)
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
        List<Livestock> all = repo.findAll()
                .stream()
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .toList();

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
        if (totalQuantity < 50) {
            notification.createNotification(
                    "LIVESTOCK",
                    "Low Livestock Count",
                    "Total livestock count has dropped below 50. Please inspect",
                    null);
        }
        if (totalMortality > 20) {
            notification.createNotification(
                    "LIVESTOCK",
                    "High Mortality Alert",
                    "More than 20 deaths record. Investigate possible disease or stress factors",
                    null);
        }

        return summary;
    }

    // ADJUST stock
    @Override
    public LivestockResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy) {
        Livestock livestock = repo.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", id));

        livestock.adjustStock(request.getQuantity(), request.getReason());
        livestock.setUpdatedBy(updatedBy);

        repo.save(livestock);
        return LivestockMapper.toResponseDto(livestock);
    }


}
