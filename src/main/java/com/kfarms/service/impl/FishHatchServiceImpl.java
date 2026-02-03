package com.kfarms.service.impl;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.entity.FishPondType;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishHatchMapper;
import com.kfarms.repository.FishHatchRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishHatchService;
import com.kfarms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", request.getPondId()));

        // --- core fields ---
        entity.setPond(pond);

        if (request.getHatchDate() != null) {
            entity.setHatchDate(request.getHatchDate());
        }

        entity.setMaleCount(request.getMaleCount());
        entity.setFemaleCount(request.getFemaleCount());
        entity.setQuantityHatched(request.getQuantityHatched());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(updatedBy);

        // --- hatch rate (%) ---
        int totalParents = request.getMaleCount() + request.getFemaleCount();

        double hatchRatePercent = totalParents > 0
                ? (request.getQuantityHatched() * 100.0) / totalParents
                : 0.0;

        hatchRatePercent = Math.round(hatchRatePercent * 100.0) / 100.0; // 2dp
        entity.setHatchRate(hatchRatePercent);

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
                        p -> countMap.getOrDefault(p.getId(), 0L),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        // Step 5: Build summary response
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHatchRecords", totalRecords);
        summary.put("hatchCountByPond", hatchCountByPond);

        // ===================== Hatch by pond type (ALL TYPES WITH 0s) =====================
        Map<String, Long> hatchCountByPondType = new LinkedHashMap<>();

        // init all enum values to 0
        for (FishPondType type : FishPondType.values()) {
            hatchCountByPondType.put(type.name(), 0L);
        }

        // fill actual counts from DB
        for (Object[] row : hatchRepo.countHatchesByPondType()) {
            String pondType = String.valueOf(row[0]); // enum name
            long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
            hatchCountByPondType.put(pondType, count);
        }

        summary.put("hatchCountByPondType", hatchCountByPondType);

        // ===================== Monthly hatch totals (Jan–Dec 2026) =====================
        int year = 2026;
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        Map<String, Long> monthlyHatchTotals = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyHatchTotals.put(String.format("%04d-%02d", year, m), 0L);
        }

        for (Object[] row : hatchRepo.sumMonthlyHatchTotals(start, end)) {
            int y = ((Number) row[0]).intValue();
            int m = ((Number) row[1]).intValue();
            long total = row[2] == null ? 0L : ((Number) row[2]).longValue();
            monthlyHatchTotals.put(String.format("%04d-%02d", y, m), total);
        }

        summary.put("monthlyHatchTotals", monthlyHatchTotals);

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
