package com.kfarms.mapper;

import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;

public class FeedMapper {
    public static FeedResponseDto toResponseDto(Feed entity){
        FeedResponseDto dto = new FeedResponseDto();
        dto.setId(entity.getId());
        dto.setBatchId(entity.getBatchId());
        dto.setBatchType(entity.getBatchType() != null ? entity.getBatchType().name() : null);
        dto.setType(dto.getBatchType());
        dto.setFeedName(entity.getFeedName());
        dto.setName(entity.getFeedName());
        dto.setItemName(entity.getFeedName());
        dto.setQuantityUsed(entity.getQuantityUsed());
        dto.setQuantity(entity.getQuantityUsed());
        dto.setUnitCost(entity.getUnitCost());
        dto.setNote(entity.getNote());
        dto.setDate(entity.getDate());

        // AUDITABLE
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
