package com.kfarms.service.impl;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishHatchMapper;
import com.kfarms.repository.FishHatchRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishHatchService;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FishHatchServiceImpl implements FishHatchService {

    private final FishHatchRepository hatchRepo;
    private final FishPondRepository pondRepo;
    private final NotificationService notification;


    // CREATE
    @Override
    public FishHatchResponseDto create(FishHatchRequestDto request){
        FishPond pond = pondRepo.findById(request.getPondId())
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", request.getPondId()));

        FishHatch entity = FishHatchMapper.toEntity(request, pond);
        FishHatch saved = hatchRepo.save(entity);
        return FishHatchMapper.toResponseDto(saved);
    }

    // READ
    @Override
    public List<FishHatchResponseDto> getAll(){
        return hatchRepo.findAll()
                .stream()
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .map(FishHatchMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // READ - by ID
    @Override
    public FishHatchResponseDto getById(Long id){
        FishHatch hatch = hatchRepo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));
        return FishHatchMapper.toResponseDto(hatch);
    }

    // UPDATE
    @Override
    public FishHatchResponseDto update(Long id, FishHatchRequestDto request, String updatedBy) {
        FishHatch entity = hatchRepo.findById(id)
                .filter(f -> !Boolean.TRUE.equals(f.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));

        FishPond pond = pondRepo.findById(request.getPondId())
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", request.getPondId()));

        entity.setPond(pond);
        if (request.getHatchDate() != null) {
            entity.setHatchDate(request.getHatchDate());
        }
        entity.setMaleCount(request.getMaleCount());
        entity.setFemaleCount(request.getFemaleCount());
        entity.setQuantityHatched(request.getQuantityHatched());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(updatedBy);

        // recalculate hatch rate
        int totalParents = request.getMaleCount() + request.getFemaleCount();
        entity.setHatchRate(totalParents > 0
                ? (double) request.getQuantityHatched() / totalParents * 100
                : 0.0);

        hatchRepo.save(entity);
        return FishHatchMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    public void delete(Long id, String deletedBy){
        FishHatch entity = hatchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fish hatch record with ID " + id + " has already been deleted");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        hatchRepo.save(entity);
    }

    // RESTORE
    @Override
    public void restore(Long id) {
        FishHatch entity = hatchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fish hatch record with ID " + id + " has already been restored");
        }

        entity.setDeleted(false);
        entity.setDeletedAt(null);
        hatchRepo.save(entity);
    }

    // SUMMARY - analysis and reports
    @Override
    public Map<String, Object> getSummary() {
        long totalRecords = hatchRepo.count();

        // Step 1: Get all ponds (non-deleted)
        List<FishPond> ponds = pondRepo.findAll()
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .toList();

        // Step 2: Get counts grouped by pondId
        List<Object[]> counts = hatchRepo.countHatchesGroupedByPond();

        // Step 3: Convert count results to a quick lookup map
        Map<Long, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],   // pondId
                        row -> (Long) row[1]    // hatch count
                ));

        // Step 4: Build final { pondName: count } map (show 0 if no record)
        Map<String, Long> hatchCountByPond = ponds.stream()
                .collect(Collectors.toMap(
                        FishPond::getPondName,
                        p -> countMap.getOrDefault(p.getId(), 0L)
                ));

        // Step 5: Build summary response
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHatchRecords", totalRecords);
        summary.put("hatchCountByPond", hatchCountByPond);


        // ==== NOTIFICATION ====
        if (totalRecords == 0) {
            notification.createNotification(
                    "FISH",
                    "No Hatch Records Found",
                    "There are no fish hatch records in the system.",
                    null
            );
        }

        return summary;
    }
}
