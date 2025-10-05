package com.kfarms.mapper;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.entity.*;
import com.kfarms.service.impl.FishPondServiceImpl;

import java.time.LocalDate;

public class FishPondMapper {
    public static FishPondResponseDto toResponseDto(FishPond entity){
        FishPondResponseDto dto = new FishPondResponseDto();
        // BASIC INFO
        dto.setId(entity.getId());
        dto.setPondName(entity.getPondName());
        dto.setPondType(entity.getPondType() != null ? entity.getPondType().name() : null);
        // STOCK INFO
        dto.setCurrentStock(entity.getCurrentStock());
        dto.setCapacity(entity.getCapacity());
        dto.setMortalityCount(entity.getMortalityCount());
        // GROWTH/FEEDING
        dto.setFeedingSchedule(entity.getFeedingSchedule() != null ? entity.getFeedingSchedule().name() : null);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        // LOCATION
        dto.setPondLocation(entity.getPondLocation() != null ? entity.getPondLocation().name() : null);
        // DATES
        dto.setLastWaterChange(entity.getLastWaterChange());
        dto.setDateStocked(entity.getDateStocked());
        dto.setNextWaterChange(entity.getNextWaterChange());
        // NOTES
        dto.setNote(entity.getNote());

        // AUDITABLE
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static FishPond toEntity(FishPondRequestDto dto) {
        FishPond entity = new FishPond();

        entity.setPondName(dto.getPondName());

        // POND TYPE
        if (dto.getPondType() != null && !dto.getPondType().isBlank()) {
            try {
                entity.setPondType(FishPondType.valueOf(dto.getPondType().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid pond type: " + dto.getPondType());
            }
        }

        // STATUS
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            entity.setStatus(FishPondStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        }

        // SELF-MANAGE LOGIC comes after setting status
        if (entity.getStatus() == FishPondStatus.EMPTY) {
            entity.setCurrentStock(0);
            entity.setMortalityCount(0);
            entity.setFeedingSchedule(null);
            entity.setLastWaterChange(null);
        } else {
            // STOCK INFO
            if (dto.getCurrentStock() != null) {
                entity.setCurrentStock(dto.getCurrentStock());
            }
            entity.setCapacity(dto.getCapacity());
            entity.setMortalityCount(dto.getMortalityCount());

            // FEEDING
            if (dto.getFeedingSchedule() != null && !dto.getFeedingSchedule().isBlank()) {
                try {
                    entity.setFeedingSchedule(FishFeedingSchedule.valueOf(dto.getFeedingSchedule().trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid feeding schedule: " + dto.getFeedingSchedule());
                }
            }

            // POND LOCATION
            if (dto.getPondLocation() != null && !dto.getPondLocation().isBlank()) {
                try {
                    entity.setPondLocation(FishPondLocation.valueOf(dto.getPondLocation().trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid pond location: " + dto.getPondLocation());
                }
            }

            // DATES
            entity.setDateStocked(dto.getDateStocked() != null ? dto.getDateStocked() : LocalDate.now());
            entity.setLastWaterChange(dto.getLastWaterChange());
        }

        // NOTES
        entity.setNote(dto.getNote());
        return entity;
    }

}
