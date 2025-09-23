package com.kfarms.service;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.InventoryCategory;

import java.time.LocalDate;
import java.util.Map;

public interface InventoryService {
    InventoryResponseDto create(InventoryRequestDto inventory);
    Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate date);
    InventoryResponseDto getById(Long id);
    InventoryResponseDto update(Long id, InventoryRequestDto request, String updatedBy);
    void delete(Long id);
    Map<String, Object> getSummary();
}
