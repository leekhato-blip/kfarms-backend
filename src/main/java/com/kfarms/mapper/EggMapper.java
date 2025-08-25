package com.kfarms.mapper;

import com.kfarms.dto.EggDto;
import com.kfarms.entity.Egg;
import com.kfarms.entity.Layer;
import org.springframework.stereotype.Component;

@Component
public class EggMapper {
    public static EggDto toDto(Egg entity){
        EggDto dto = new EggDto();
        dto.setId(entity.getId());
        dto.setQuantity((entity.getQuantity()));
        dto.setCollectionDate(entity.getCollectionDate());
        dto.setNotes(entity.getNotes());

        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getLayer() != null) {
            dto.setLayerId(entity.getLayer().getId());
        }
        return dto;
    }

    public static Egg toEntity (EggDto dto){
        Egg entity = new Egg();
        entity.setId(dto.getId());
        System.out.println("Looking for layer with ID: " + dto.getLayerId());
        entity.setQuantity(dto.getQuantity());
        entity.setCollectionDate(dto.getCollectionDate());
        entity.setNotes(dto.getNotes());
        return entity;
    }
}
