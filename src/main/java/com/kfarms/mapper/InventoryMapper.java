package com.kfarms.mapper;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryMapper {
    public static InventoryResponseDto toResponseDto(Inventory entity){
        InventoryResponseDto dto = new InventoryResponseDto();

        // fields
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setSku(entity.getSku());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());
        dto.setUnitCost(entity.getUnitCost());
        dto.setTotalValue(resolveTotalValue(entity));
        dto.setNote(entity.getNote());
        dto.setMinThreshold(entity.getMinThreshold());
        dto.setSupplierName(entity.getSupplierName());
        dto.setStorageLocation(entity.getStorageLocation());
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
        entity.setSku(blankToNull(dto.getSku()));
        entity.setQuantity(dto.getQuantity());
        entity.setUnit(dto.getUnit().trim());
        entity.setUnitCost(dto.getUnitCost());
        entity.setSupplierName(blankToNull(dto.getSupplierName()));
        entity.setStorageLocation(blankToNull(dto.getStorageLocation()));
        entity.setNote(blankToNull(dto.getNote()));
        entity.setMinThreshold(dto.getMinThreshold() != null ? dto.getMinThreshold() : 0);
        entity.setLastUpdated(dto.getLastUpdated() != null ? dto.getLastUpdated() : LocalDate.now());
        return entity;
    }

    private static BigDecimal resolveTotalValue(Inventory entity) {
        if (entity.getUnitCost() == null || entity.getQuantity() == null) {
            return null;
        }
        return entity.getUnitCost().multiply(BigDecimal.valueOf(entity.getQuantity()));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
