package com.kfarms.mapper;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;

public class FeedMapper {
    public static FeedResponseDto toResponseDto(Feed entity){
        FeedResponseDto dto = new FeedResponseDto();
        dto.setId(entity.getId());
        dto.setBatchId(entity.getBatchId());
        dto.setBatchType(entity.getBatchType());
        dto.setFeedName(entity.getFeedName());
        dto.setQuantityUsed(entity.getQuantityUsed());
        dto.setNotes(entity.getNotes());
        dto.setDate(entity.getDate());

        // AUDITABLE
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Feed toEntity(FeedRequestDto dto){
        Feed entity = new Feed();
        entity.setBatchId(dto.getBatchId());
        entity.setBatchType(dto.getBatchType());
        entity.setFeedName(dto.getFeedName());
        entity.setQuantityUsed(dto.getQuantityUsed());
        entity.setNotes(dto.getNotes());
        entity.setDate(dto.getDate());
        return entity;
    }
}
