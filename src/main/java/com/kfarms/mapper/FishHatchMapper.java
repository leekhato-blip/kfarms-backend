package com.kfarms.mapper;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;

import java.time.LocalDate;
public class FishHatchMapper {

    public static FishHatchResponseDto toResponseDto(FishHatch entity) {
        FishHatchResponseDto dto = new FishHatchResponseDto();
        dto.setId(entity.getId());
        dto.setHatchDate(entity.getHatchDate());
        dto.setMaleCount(entity.getMaleCount());
        dto.setFemaleCount(entity.getFemaleCount());
        dto.setQuantityHatched(entity.getQuantityHatched());
        dto.setHatchRate(entity.getHatchRate()); // percentage
        dto.setNote(entity.getNote());

        if (entity.getPond() != null) {
            dto.setPondId(entity.getPond().getId());
            dto.setPondName(entity.getPond().getPondName());
        }

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static FishHatch toEntity(FishHatchRequestDto request, FishPond pond) {
        FishHatch entity = new FishHatch();
        entity.setPond(pond);

        LocalDate hatchDate = request.getHatchDate() != null
                ? request.getHatchDate()
                : LocalDate.now();
        entity.setHatchDate(hatchDate);

        entity.setMaleCount(request.getMaleCount());
        entity.setFemaleCount(request.getFemaleCount());
        entity.setQuantityHatched(request.getQuantityHatched());
        entity.setNote(request.getNote());

        int totalParents = request.getMaleCount() + request.getFemaleCount();

        double hatchRatePercent = totalParents > 0
                ? (request.getQuantityHatched() * 100.0) / totalParents
                : 0.0;

        hatchRatePercent = Math.round(hatchRatePercent * 100.0) / 100.0; // 2dp
        entity.setHatchRate(hatchRatePercent);

        return entity;
    }
}