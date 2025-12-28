package com.kfarms.mapper;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;

import java.time.LocalDate;

public class InventoryMapper {
    public static InventoryResponseDto toResponseDto(Inventory entity){
        InventoryResponseDto dto = new InventoryResponseDto();

        // fields
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());
        dto.setNote(entity.getNote());
        dto.setMinThreshold(entity.getMinThreshold());
        dto.setLastUpdated(entity.getLastUpdated());

        // audits
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Inventory toEntity(InventoryRequestDto dto){
        Inventory entity = new Inventory();
        entity.setItemName(dto.getItemName());
        if (dto.getCategory() != null && !dto.getCategory().isBlank()) {
            entity.setCategory(InventoryCategory.valueOf(dto.getCategory().trim().toUpperCase()));
        }
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit());
        entity.setNote(dto.getNote());
        entity.setMinThreshold(dto.getMinThreshold());
        entity.setLastUpdated(dto.getLastUpdated() != null ? dto.getLastUpdated() : LocalDate.now());
        return entity;
    }
}