package com.kfarms.mapper;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;

import java.time.LocalDate;

public class FishHatchMapper {
    public static FishHatchResponseDto toResponseDto(FishHatch entity){
        FishHatchResponseDto dto = new FishHatchResponseDto();
        dto.setId(entity.getId());
        dto.setHatchDate(entity.getHatchDate() != null ? dto.getHatchDate() : LocalDate.now());
        dto.setMaleCount(entity.getMaleCount());
        dto.setFemaleCount(entity.getFemaleCount());
        dto.setQuantityHatched(entity.getQuantityHatched());
        dto.setHatchRate(entity.getHatchRate());
        dto.setNote(entity.getNote());

        if (entity.getPond() != null) {
            dto.setPondId(entity.getPond().getId());
            dto.setPondName(entity.getPond().getPondName());
        }

        // AUDITABLE
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static FishHatch toEntity(FishHatchRequestDto request, FishPond pond) {
        FishHatch entity = new FishHatch();
        entity.setPond(pond);
        entity.setHatchDate(request.getHatchDate() != null ? request.getHatchDate() : LocalDate.now());
        entity.setMaleCount(request.getMaleCount());
        entity.setFemaleCount(request.getFemaleCount());
        entity.setQuantityHatched(request.getQuantityHatched());

        // Calculate hatch rate
        int totalParents = request.getMaleCount() + request.getFemaleCount();
        double hatchRate = 0.0;
        if (totalParents > 0) {
            hatchRate = (double) request.getQuantityHatched() / totalParents;
            hatchRate = Math.round(hatchRate * 100.0) / 100.0; // round to 2 dp
        }

        entity.setNote(request.getNote());
        return entity;
    }
}
