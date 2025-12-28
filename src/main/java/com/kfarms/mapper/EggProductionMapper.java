package com.kfarms.mapper;

import com.kfarms.dto.EggProductionRequestDto;
import com.kfarms.dto.EggProductionResponseDto;
import com.kfarms.entity.EggProduction;
import com.kfarms.entity.Livestock;

public class EggProductionMapper {

    public static EggProduction toEntity(EggProductionRequestDto request, Livestock livestock) {
        EggProduction entity = new EggProduction();
        entity.setLivestock(livestock);
        if (request.getCollectionDate() != null) {
            entity.setCollectionDate(request.getCollectionDate());
        }
        entity.setGoodEggs(request.getGoodEggs());
        entity.setDamagedEggs(request.getDamagedEggs());
        entity.setNote(request.getNote());
        entity.calculateCrates();
        return entity;
    }

    public static EggProductionResponseDto toResponseDto(EggProduction entity){
        EggProductionResponseDto response = new EggProductionResponseDto();
        response.setId(entity.getId());
        response.setBatchId(entity.getLivestock().getId());
        response.setBatchName(entity.getLivestock().getBatchName());
        response.setCollectionDate(entity.getCollectionDate());
        response.setGoodEggs(entity.getGoodEggs());
        response.setDamagedEggs(entity.getDamagedEggs());
        response.setCratesProduced(entity.getCratesProduced());
        response.setNote(entity.getNote());

        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedBy(entity.getUpdatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        return response;
    }
}
