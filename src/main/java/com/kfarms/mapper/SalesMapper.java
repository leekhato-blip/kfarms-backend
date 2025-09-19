package com.kfarms.mapper;

import com.kfarms.dto.SalesRequestDto;
import com.kfarms.dto.SalesResponseDto;
import com.kfarms.entity.Sales;
import com.kfarms.entity.SalesCategory;

import java.time.LocalDate;

public class SalesMapper {
    public static SalesResponseDto toResponseDto(Sales entity){
        SalesResponseDto dto = new SalesResponseDto();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setBuyer(entity.getBuyer());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
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

    public static Sales toEntity(SalesRequestDto dto) {
        Sales entity = new Sales();
        entity.setItemName(dto.getItemName());

        // if buyer is not provided, set a default name
        entity.setBuyer(dto.getBuyer() != null && !dto.getBuyer().isBlank()
                ? dto.getBuyer()
                : "Walk-in Customer");

        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());
        entity.setNotes(dto.getNotes());

        // set Category (TYPE)
        if (dto.getCategory() != null) {
            entity.setCategory(SalesCategory.valueOf(dto.getCategory().toUpperCase()));
        }

        // auto calculate total price (quantity * unitPrice)
        entity.setTotalPrice(dto.getQuantity() * dto.getUnitPrice());

        // default date == today if not provided
        entity.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        return entity;
    }
}
