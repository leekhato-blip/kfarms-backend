package com.kfarms.service;


import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.dto.StockAdjustmentRequestDto;

import java.time.LocalDate;
import java.util.Map;

public interface LivestockService {
    Map<String, Object>  getAll(int page, int size, String batchName, String type, LocalDate arrivalDate);
    LivestockResponseDto getById(Long id);
    LivestockResponseDto create(LivestockRequestDto request, String createdBy);
    LivestockResponseDto update(Long id, LivestockRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
    LivestockResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy);
}
