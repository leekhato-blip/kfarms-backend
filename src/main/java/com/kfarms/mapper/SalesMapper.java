package com.kfarms.mapper;

import com.kfarms.dto.SalesDto;
import com.kfarms.entity.Sales;

public class SalesMapper {
    public static SalesDto toDto(Sales entity){
        SalesDto dto = new SalesDto();
        dto.setId(entity.getId());
        dto.setProductType(entity.getProductType());
        dto.setBuyer(entity.getBuyer());
        dto.setPrice(entity.getPrice());
        dto.setQuantity(entity.getQuantity());
        dto.setSaleDate(entity.getSaleDate());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Sales toEntity(SalesDto dto){
        Sales entity = new Sales();
        entity.setId(dto.getId());
        entity.setProductType(dto.getProductType());
        entity.setBuyer(dto.getBuyer());
        entity.setQuantity(dto.getQuantity());
        entity.setPrice(dto.getPrice());
        entity.setSaleDate(dto.getSaleDate());
        return entity;
    }
}
