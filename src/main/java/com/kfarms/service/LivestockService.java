package com.kfarms.service;


import com.kfarms.dto.LivestockRequestDto;
import com.kfarms.dto.LivestockResponseDto;
import com.kfarms.dto.MortalityRecordRequestDto;
import com.kfarms.dto.StockAdjustmentRequestDto;

import java.time.LocalDate;
import java.util.Map;

public interface LivestockService {

    // READ - get all Livestock (Pagination and Filtering)
    Map<String, Object> getAll(int page, int size, String batchName, String type, LocalDate arrivalDate, Boolean deleted);

    LivestockResponseDto getById(Long id);
    LivestockResponseDto create(LivestockRequestDto request, String createdBy);
    LivestockResponseDto update(Long id, LivestockRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void permanentDelete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
    Map<String, Object> getOverview(int rangeDays);

    LivestockResponseDto adjustStock(Long id, StockAdjustmentRequestDto request, String updatedBy);
    LivestockResponseDto recordMortality(Long id, MortalityRecordRequestDto request, String updatedBy);
}
