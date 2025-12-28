package com.kfarms.mapper;

import com.kfarms.dto.FeedRequestDto;
import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.entity.FeedBatchType;

import java.time.LocalDate;

public class FeedMapper {
    public static FeedResponseDto toResponseDto(Feed entity){
        FeedResponseDto dto = new FeedResponseDto();
        dto.setId(entity.getId());
        dto.setBatchId(entity.getBatchId());
        dto.setBatchType(entity.getBatchType() != null ? entity.getBatchType().name() : null);
        dto.setFeedName(entity.getFeedName());
        dto.setQuantityUsed(entity.getQuantityUsed());
        dto.setNote(entity.getNote());
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
        if (dto.getBatchType() != null && !dto.getBatchType().isBlank()) {
            entity.setBatchType(FeedBatchType.valueOf(dto.getBatchType().trim().toUpperCase()));
        }
        entity.setBatchId(dto.getBatchId());
        entity.setFeedName(dto.getFeedName());
        entity.setQuantityUsed(dto.getQuantityUsed());
        entity.setNote(dto.getNote());
        entity.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now()); // default today
        return entity;
    }
}
