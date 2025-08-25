package com.kfarms.mapper;

import com.kfarms.dto.DuckDto;
import com.kfarms.entity.Duck;

public class DuckMapper {
    public static DuckDto toDto(Duck entity){
        DuckDto dto = new DuckDto();
        dto.setId(entity.getId());
        dto.setBatchName(entity.getBatchName());
        dto.setQuantity(entity.getQuantity());
        dto.setNotes(entity.getNotes());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Duck toEntity(DuckDto dto){
        Duck entity = new Duck();
        entity.setId(dto.getId());
        entity.setBatchName(dto.getBatchName());
        entity.setQuantity(dto.getQuantity());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
