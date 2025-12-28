package com.kfarms.mapper;


import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.entity.Livestock;
import com.kfarms.entity.LivestockType;
import com.kfarms.entity.SourceType;

import java.time.LocalDate;

public class LivestockMapper {

    // Convert DTO → Entity
    public static Livestock toEntity(LivestockRequestDto request) {
        Livestock entity = new Livestock();

        // Batch name (mandatory)
        entity.setBatchName(request.getBatchName() != null ? request.getBatchName().trim().toUpperCase() : null);

        // LIVESTOCK TYPE
        if (request.getType() != null && !request.getType().name().isBlank()){
            try {
                entity.setType(LivestockType.valueOf(request.getType().name().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid livestock type: " + request.getType().name());
            }
        }

        // SOURCE TYPE
        if (request.getSourceType() != null) {
            entity.setSourceType(request.getSourceType());
        } else {
            entity.setSourceType(SourceType.SUPPLIER);
        }
        // STOCK INFO
        entity.setCurrentStock(request.getCurrentStock() != null ? request.getCurrentStock() : 0);
        entity.setMortality(request.getMortality() != null ? request.getMortality() : 0);

        // Starting age: 0 for FARM_BIRTH, else use provided value or 0
        int starting = (request.getSourceType() == SourceType.FARM_BIRTH) ? 0 :
                (request.getStartingAgeInWeeks() != null ? Math.max(0, request.getStartingAgeInWeeks()) : 0);
        entity.setStartingAgeInWeeks(starting);

        // ARRIVAL DATE
        entity.setArrivalDate(
                request.getArrivalDate() != null ? request.getArrivalDate() : LocalDate.now()
        );

        // NOTE
        entity.setNote(request.getNote());

        return entity;
    }

    public static LivestockResponseDto toResponseDto(Livestock entity){
        LivestockResponseDto response = new LivestockResponseDto();
        response.setId(entity.getId());
        response.setBatchName(entity.getBatchName() != null ? entity.getBatchName().trim().toUpperCase() : null);
        response.setCurrentStock(entity.getCurrentStock());
        response.setType(entity.getType() != null ? entity.getType() : null);
        response.setArrivalDate(entity.getArrivalDate());
        response.setSourceType(entity.getSourceType() != null ? SourceType.valueOf(entity.getSourceType().name().toUpperCase()) : null);
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
