package com.kfarms.mapper;

import com.kfarms.dto.HatchDto;
import com.kfarms.entity.Hatch;

public class HatchMapper {
    public static HatchDto toDto(Hatch entity){
        HatchDto dto = new HatchDto();
        dto.setId(entity.getId());
        dto.setHatchDate(entity.getHatchDate());
        dto.setHatchRate(entity.getHatchRate());
        dto.setMaleCount(entity.getMaleCount());
        dto.setFemaleCount(entity.getFemaleCount());
        dto.setNotes(entity.getNotes());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Hatch toEntity(HatchDto dto){
        Hatch entity = new Hatch();
        entity.setId(dto.getId());
        entity.setHatchDate(dto.getHatchDate());
        entity.setHatchRate(dto.getHatchRate());
        entity.setMaleCount(dto.getMaleCount());
        entity.setFemaleCount(dto.getFemaleCount());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
