package com.kfarms.mapper;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.SourceType;

public class LivestockMapper {
    public static Livestock toEntity(LivestockRequest dto){
        Livestock entity = new Livestock();
        entity.setBatchName(dto.getBatchName());
        entity.setQuantity(dto.getQuantity());
        entity.setType(dto.getType());
        entity.setNote(dto.getNote());
        // Default mortality to 0 if not provided
        entity.setMortality(dto.getMortality() != null ? dto.getMortality() : 0 );
        entity.setArrivalDate(dto.getArrivalDate());
        entity.setSourceType(dto.getSourceType());

        // Starting age: 0 for FARM_BIRTH, else use provided value or 0
        int starting = (dto.getSourceType() == SourceType.FARM_BIRTH) ? 0 :
                (dto.getStartingAgeInWeeks() != null ? Math.max(0, dto.getStartingAgeInWeeks()) : 0);
        entity.setStartingAgeInWeeks(starting);
        return entity;
    }

    public static LivestockResponse toResponse(Livestock entity){
        LivestockResponse response = new LivestockResponse();
        response.setId(entity.getId());
        response.setBatchName(entity.getBatchName());
        response.setQuantity(entity.getQuantity());
        response.setType(entity.getType());
        response.setArrivalDate(entity.getArrivalDate());

        response.setSourceType(entity.getSourceType());
        response.setStartingAgeInWeeks(entity.getStartingAgeInWeeks());
        response.setAgeInWeeks(entity.getAgeInWeeks()); // derived
        response.setMortality(entity.getMortality());
        response.setNote(entity.getNote());

        // audit
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
