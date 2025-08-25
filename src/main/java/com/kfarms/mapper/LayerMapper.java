package com.kfarms.mapper;

import com.kfarms.dto.LayerDto;
import com.kfarms.entity.Layer;

public class LayerMapper {
    public static LayerDto toDto(Layer entity){
        LayerDto dto = new LayerDto();
        dto.setId(entity.getId());
        dto.setBatchName(entity.getBatchName());
        dto.setQuantity(entity.getQuantity());
        dto.setNotes(entity.getNotes());
        dto.setArrivalDate(entity.getArrivalDate());
        dto.setAgeInWeeks(entity.getAgeInWeeks());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());


        return dto;
    }

    public static Layer toEntity(LayerDto dto){
        Layer entity = new Layer();
        entity.setId(dto.getId());
        entity.setBatchName(dto.getBatchName());
        entity.setQuantity(dto.getQuantity());
        entity.setArrivalDate(dto.getArrivalDate());
        entity.setAgeInWeeks(dto.getAgeInWeeks());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
