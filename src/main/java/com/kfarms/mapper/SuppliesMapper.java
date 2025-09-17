package com.kfarms.mapper;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;
import com.kfarms.entity.Supplies;

import java.time.LocalDate;

public class SuppliesMapper {
    public static SuppliesResponseDto toResponseDto(Supplies entity){
        SuppliesResponseDto dto = new SuppliesResponseDto();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setCategory(entity.getCategory());
        dto.setSupplierName(entity.getSupplierName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setDate(entity.getDate());

        // audits
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Supplies toEntity(SuppliesRequestDto dto){
        Supplies entity = new Supplies();
        entity.setItemName(dto.getItemName());
        entity.setQuantity(dto.getQuantity());
        entity.setUnitPrice(dto.getUnitPrice());

        // auto calculate total
        entity.setTotalPrice(dto.getQuantity() + dto.getUnitPrice());

        entity.setSupplierName(dto.getSupplierName());
        entity.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        return entity;
    }
}
