package com.kfarms.mapper;

import com.kfarms.dto.SuppliesDto;
import com.kfarms.entity.Supplies;

public class SuppliesMapper {
    public static SuppliesDto toDto(Supplies entity){
        SuppliesDto dto = new SuppliesDto();
        dto.setId(entity.getId());
        dto.setItemName(entity.getItemName());
        dto.setSupplier(entity.getSupplier());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSupplyDate(entity.getSupplyDate());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Supplies toEntity(SuppliesDto dto){
        Supplies entity = new Supplies();
        entity.setId(dto.getId());
        entity.setItemName(dto.getItemName());
        entity.setSupplier(dto.getSupplier());
        entity.setQuantity(dto.getQuantity());
        entity.setPrice(dto.getPrice());
        entity.setSupplyDate(dto.getSupplyDate());
        return entity;
    }
}
