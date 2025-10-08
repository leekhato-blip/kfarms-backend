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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EggProductionServiceImpl implements EggProductionService {
    private final EggProductionRepo repo;
    private final LivestockRepository livestockRepo;

    // CREATE
    @Override
    public EggProductionResponseDto create(EggProductionRequestDto request) {
        Livestock livestock = livestockRepo.findById(request.getLivestockId())
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", request.getLivestockId()));

        EggProduction entity = EggProductionMapper.toEntity(request, livestock);
        entity.setLivestock(livestock);
        repo.save(entity);

        return EggProductionMapper.toResponseDto(entity);
    }

    // READ - all eggs (pagination + filter)
    @Override
    public Map<String, Object> getAll(int page, int size, Long livestockId, LocalDate collectionDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("collectionDate").descending());

        Specification<EggProduction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (livestockId != null) {
                predicates.add(cb.equal(root.get("livestock").get("id"), livestockId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));
        return EggProductionMapper.toResponseDto(entity);
    }

    // UPDATE
    @Override
    public EggProductionResponseDto update(Long id, EggProductionRequestDto request, String updatedBy) {
        EggProduction entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Egg", "id", id));

        if (request.getGoodEggs() != null) entity.setGoodEggs(request.getGoodEggs());
        if (request.getDamagedEggs() != null) entity.setDamagedEggs(request.getDamagedEggs());
        if (request.getCollectionDate() != null) entity.setCollectionDate(request.getCollectionDate());
        if (request.getNote() != null) entity.setNotes(request.getNote());

        entity.setUpdatedBy(updatedBy);
        repo.save(entity);
        return EggProductionMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Egg", "id", id);
        }
        repo.deleteById(id);
    }

    // SUMMARY
    @Override
    public Map<String, Object> getSummary() {
        List<EggProduction> all = repo.findAll();

        int totalCollected = all.stream().mapToInt(e -> e.getGoodEggs() != null ? e.getGoodEggs() : 0).sum();
        int totalCracked = all.stream().mapToInt(e -> e.getDamagedEggs() != null ? e.getDamagedEggs() : 0).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCollected", totalCollected);
        summary.put("totalCracked", totalCracked);

        // Group by livestock batch name
        Map<String, Integer> countByBatch = all.stream()
                .filter(e -> e.getLivestock() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getLivestock().getBatchName(),
                        Collectors.summingInt(e -> e.getGoodEggs() != null ? e.getGoodEggs() : 0)
                ));

        summary.put("countByBatch", countByBatch);

        return summary;
    }
}
