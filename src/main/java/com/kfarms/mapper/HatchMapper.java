package com.kfarms.mapper;

import com.kfarms.dto.HatchDto;
import com.kfarms.entity.FishHatch;

public class HatchMapper {
    public static HatchDto toDto(FishHatch entity){
        HatchDto dto = new HatchDto();
        dto.setId(entity.getId());
        dto.setHatchDate(entity.getHatchDate());
        dto.setHatchRate(entity.getHatchRate());
        dto.setMaleCount(entity.getMaleCount());
        dto.setFemaleCount(entity.getFemaleCount());
        dto.setNote(entity.getNote());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static FishHatch toEntity(HatchDto dto){
        FishHatch entity = new FishHatch();
        entity.setId(dto.getId());
        entity.setHatchDate(dto.getHatchDate());
        entity.setHatchRate(dto.getHatchRate());
        entity.setMaleCount(dto.getMaleCount());
        entity.setFemaleCount(dto.getFemaleCount());
        entity.setNote(dto.getNote());
        return entity;
    }
}
