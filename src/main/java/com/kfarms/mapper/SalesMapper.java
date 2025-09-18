package com.kfarms.mapper;

import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;

import java.time.LocalDate;

public class SalesMapper {
    public static SalesResponseDto toDto(Sales entity){
        SalesResponseDto dto = new SalesResponseDto();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setBuyer(entity.getBuyer());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotaPrice(entity.getTotalPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setDate(entity.getDate());
        dto.setNotes(entity.getNotes());

        // audit
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Sales toEntity(SalesResponseDto dto){
        Sales entity = new Sales();
        entity.setItemName(entity.getItemName());
        entity.setBuyer(dto.getBuyer());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());

        // set Category (TYPE)
        if (dto.getCategory() != null) {
            entity.setCategory(SalesCategory.valueOf(dto.getCategory().toUpperCase()));
        }

        // auto calculate total price (quantity * unitPrice)
        if (dto.getCategory() != null) {
            entity.setTotalPrice(dto.getQuantity() * dto.getUnitPrice());
        }

        entity.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());

        return entity;
    }
}
