package com.kfarms.mapper;

import com.kfarms.dto.FishPondDto;
import com.kfarms.entity.FishPond;

public class FishPondMapper {
    public static FishPondDto toDto(FishPond entity){
        FishPondDto dto = new FishPondDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setQuantity(entity.getQuantity());
        dto.setCapacity(entity.getCapacity());
        dto.setLastWaterChangeDate(entity.getLastWaterChangeDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static FishPond toEntity(FishPondDto dto){
        FishPond entity = new FishPond();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setQuantity(dto.getQuantity());
        entity.setCapacity(dto.getCapacity());
        entity.setLastWaterChangeDate(dto.getLastWaterChangeDate());
        entity.setStatus(dto.getStatus());
        return entity;
    }
}
