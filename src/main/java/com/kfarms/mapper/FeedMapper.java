package com.kfarms.mapper;

import com.kfarms.entity.Feed;
import com.kfarms.dto.FeedDto;

public class FeedMapper {
    public static FeedDto toDto(Feed entity){
        FeedDto dto = new FeedDto();
        dto.setId(entity.getId());
        dto.setBatchId(entity.getBatchId());
        dto.setBatchType(entity.getBatchType());
        dto.setFeedName(entity.getFeedName());
        dto.setQuantityUsed(entity.getQuantityUsed());
        dto.setDate(entity.getDate());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static Feed toEntity(FeedDto dto){
        Feed entity = new Feed();
        entity.setId(dto.getId());
        entity.setBatchId(dto.getBatchId());
        entity.setBatchType(dto.getBatchType());
        entity.setFeedName(dto.getFeedName());
        entity.setQuantityUsed(dto.getQuantityUsed());
        entity.setDate(entity.getDate());
        return entity;
    }
}
