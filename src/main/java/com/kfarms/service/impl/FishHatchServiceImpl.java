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
import com.kfarms.tenant.service.TenantContext;
import com.kfarms.tenant.service.TenantRecordAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FishHatchServiceImpl implements FishHatchService {

    private final FishHatchRepository hatchRepo;
    private final FishPondRepository pondRepo;
    private final NotificationService notification;
    private final TenantRecordAuditService tenantRecordAuditService;


    // CREATE
    @Override
    public FishHatchResponseDto create(FishHatchRequestDto request){
        FishPond pond = getTenantPond(request.getPondId());
        Long tenantId = requireTenantId();

        FishHatch entity = FishHatchMapper.toEntity(request, pond);
        entity.setTenant(pond.getTenant());
        FishHatch saved = hatchRepo.save(entity);
        tenantRecordAuditService.created(
                tenantId,
                entity.getCreatedBy(),
                "FISH_HATCH",
                saved.getId(),
                fishHatchTargetName(saved),
                fishHatchSummary(saved),
                "Created fish hatch record for " + fishHatchTargetName(saved) + "."
        );
        return FishHatchMapper.toResponseDto(saved);
    }

    // READ
    @Override
    public List<FishHatchResponseDto> getAll(Boolean deleted){
        Long tenantId = requireTenantId();
        List<FishHatch> hatchRecords = Boolean.TRUE.equals(deleted)
                ? hatchRepo.findAllByTenant_IdAndDeletedTrue(tenantId)
                : hatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId);

        return hatchRecords.stream()
                .map(FishHatchMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // READ - by ID
    @Override
    public FishHatchResponseDto getById(Long id){
        FishHatch hatch = getTenantHatch(id, false);
        return FishHatchMapper.toResponseDto(hatch);
    }

    // UPDATE
    @Override
    public FishHatchResponseDto update(Long id, FishHatchRequestDto request, String updatedBy) {

        FishHatch entity = getTenantHatch(id, false);
        FishPond pond = getTenantPond(request.getPondId());
        String previousSummary = fishHatchSummary(entity);

        // --- core fields ---
        entity.setPond(pond);
        entity.setTenant(pond.getTenant());

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
        tenantRecordAuditService.updated(
                requireTenantId(),
                updatedBy,
                "FISH_HATCH",
                entity.getId(),
                fishHatchTargetName(entity),
                previousSummary,
                fishHatchSummary(entity),
                "Updated fish hatch record for " + fishHatchTargetName(entity) + "."
        );

        return FishHatchMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    @Transactional
    public void delete(Long id, String deletedBy){
        FishHatch entity = getTenantHatch(id, true);
        String previousSummary = fishHatchSummary(entity);

        if (Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fish hatch record with ID " + id + " has already been deleted");
        }

        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setUpdatedBy(deletedBy);
        hatchRepo.save(entity);
        tenantRecordAuditService.deleted(
                requireTenantId(),
                deletedBy,
                "FISH_HATCH",
                entity.getId(),
                fishHatchTargetName(entity),
                previousSummary,
                "Deleted fish hatch record for " + fishHatchTargetName(entity) + "."
        );
    }

    @Override
    @Transactional
    public void permanentDelete(Long id, String deletedBy) {
        Long tenantId = requireTenantId();
        FishHatch entity = getTenantHatch(id, true);

        if (!Boolean.TRUE.equals(entity.getDeleted())) {
            throw new IllegalArgumentException("Fish hatch record with ID " + id + " must be moved to trash before permanent delete");
        }

        pondRepo.clearHatchBatchReferences(entity.getId(), tenantId);
        int deletedCount = hatchRepo.hardDeleteByIdAndTenantId(entity.getId(), tenantId);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("FishHatch", "id", id);
        }
    }

    // RESTORE
    @Override
    @Transactional
    public void restore(Long id) {
        FishHatch entity = getTenantHatch(id, true);

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
        Long tenantId = requireTenantId();
        List<FishHatch> activeHatches = hatchRepo.findAllByTenant_IdAndDeletedFalse(tenantId);
        List<FishPond> ponds = pondRepo.findAll()
                .stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .filter(p -> p.getTenant() != null && Objects.equals(p.getTenant().getId(), tenantId))
                .toList();

        long totalRecords = activeHatches.size();

        Map<Long, Long> countMap = activeHatches.stream()
                .filter(h -> h.getPond() != null && h.getPond().getId() != null)
                .collect(Collectors.groupingBy(h -> h.getPond().getId(), Collectors.counting()));

        Map<String, Long> hatchCountByPond = ponds.stream()
                .collect(Collectors.toMap(
                        FishPond::getPondName,
                        p -> countMap.getOrDefault(p.getId(), 0L),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHatchRecords", totalRecords);
        summary.put("hatchCountByPond", hatchCountByPond);

        Map<String, Long> hatchCountByPondType = new LinkedHashMap<>();
        for (FishPondType type : FishPondType.values()) {
            hatchCountByPondType.put(type.name(), 0L);
        }
        activeHatches.stream()
                .map(FishHatch::getPond)
                .filter(Objects::nonNull)
                .map(FishPond::getPondType)
                .filter(Objects::nonNull)
                .forEach(type -> hatchCountByPondType.put(type.name(), hatchCountByPondType.get(type.name()) + 1));

        summary.put("hatchCountByPondType", hatchCountByPondType);

        int year = LocalDate.now().getYear();
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        Map<String, Long> monthlyHatchTotals = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyHatchTotals.put(String.format("%04d-%02d", year, m), 0L);
        }

        activeHatches.stream()
                .filter(h -> h.getHatchDate() != null)
                .filter(h -> !h.getHatchDate().isBefore(start) && !h.getHatchDate().isAfter(end))
                .forEach(h -> {
                    String key = String.format("%04d-%02d", h.getHatchDate().getYear(), h.getHatchDate().getMonthValue());
                    monthlyHatchTotals.put(key, monthlyHatchTotals.getOrDefault(key, 0L) + h.getQuantityHatched());
                });

        summary.put("monthlyHatchTotals", monthlyHatchTotals);
        summary.put("fryHatchedTotal", activeHatches.stream().mapToLong(FishHatch::getQuantityHatched).sum());
        summary.put("avgHatchRate", activeHatches.stream().mapToDouble(FishHatch::getHatchRate).average().orElse(0.0));
        summary.put("dueWaterChangeCount", 0L);


        return summary;
    }

    private Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    private FishPond getTenantPond(Long pondId) {
        Long tenantId = requireTenantId();
        return pondRepo.findById(pondId)
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .filter(p -> p.getTenant() != null && Objects.equals(p.getTenant().getId(), tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", pondId));
    }

    private FishHatch getTenantHatch(Long id, boolean includeDeleted) {
        Long tenantId = requireTenantId();
        FishHatch hatch = hatchRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));

        if (!includeDeleted && Boolean.TRUE.equals(hatch.getDeleted())) {
            throw new ResourceNotFoundException("FishHatch", "id", id);
        }
        return hatch;
    }

    private String fishHatchTargetName(FishHatch hatch) {
        if (hatch == null || hatch.getPond() == null) {
            return "Fish hatch record";
        }
        String pondName = hatch.getPond().getPondName();
        return pondName != null && !pondName.isBlank() ? pondName.trim() : "Fish hatch record";
    }

    private String fishHatchSummary(FishHatch hatch) {
        if (hatch == null) {
            return "";
        }
        return String.format(
                "Hatched %s • Male %s • Female %s • Date %s",
                hatch.getQuantityHatched(),
                hatch.getMaleCount(),
                hatch.getFemaleCount(),
                hatch.getHatchDate() != null ? hatch.getHatchDate().toString() : "N/A"
        );
    }
}
