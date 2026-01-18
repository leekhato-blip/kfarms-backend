package com.kfarms.service;

import com.kfarms.dto.FishPondRequestDto;
import com.kfarms.dto.FishPondResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;

import java.time.LocalDate;
import java.util.Map;

public interface FishPondService {
    FishPondResponseDto create(FishPondRequestDto fishPond);
    Map<String, Object> getAll(int page, int size, String pondName, String pondType, String status, LocalDate lastWaterChange);
    FishPondResponseDto getById(Long id);
    FishPondResponseDto update(Long id, FishPondRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
    FishPondResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy);
}
