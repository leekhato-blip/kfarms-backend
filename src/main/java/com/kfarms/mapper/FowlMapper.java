package com.kfarms.mapper;

import com.kfarms.dto.FowlDto;
import com.kfarms.entity.Fowl;

public class FowlMapper {
    public static FowlDto toDto(Fowl entity){
        FowlDto dto = new FowlDto();
        dto.setId(entity.getId());
        dto.setBatchName(entity.getBatchName());
        dto.setQuantity(entity.getQuantity());
        dto.setArrivalDate(entity.getArrivalDate());
        dto.setAgeInWeeks(entity.getAgeInWeeks());
        dto.setNotes(entity.getNotes());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Fowl toEntity(FowlDto dto){
        Fowl entity = new Fowl();
        entity.setBatchName(dto.getBatchName());
        entity.setQuantity(dto.getQuantity());
        entity.setArrivalDate(dto.getArrivalDate());
        entity.setAgeInWeeks(dto.getAgeInWeeks());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
