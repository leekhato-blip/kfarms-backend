package com.kfarms.mapper;

import com.kfarms.dto.InventoryDto;
import com.kfarms.entity.Inventory;

public class InventoryMapper {
    public static InventoryDto toDto(Inventory entity){
        InventoryDto dto = new InventoryDto();
        dto.setId(entity.getId());
        dto.setItem(entity.getItem());
        dto.setCategory(entity.getCategory());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());
        dto.setLastUpdated(entity.getLastUpdated());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Inventory toEntity(InventoryDto dto){
        Inventory entity = new Inventory();
        entity.setId(dto.getId());
        entity.setItem(dto.getItem());
        entity.setCategory(dto.getCategory());
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit());
        entity.setLastUpdated(dto.getLastUpdated());
        return entity;
    }
}
