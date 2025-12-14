package com.kfarms.service;

import com.kfarms.dto.InventoryRequestDto;
import com.kfarms.dto.InventoryResponseDto;
import com.kfarms.entity.InventoryCategory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface InventoryService {
    InventoryResponseDto create(InventoryRequestDto inventory);
    Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate lastUpdated);
    InventoryResponseDto getById(Long id);
    InventoryResponseDto update(Long id, InventoryRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
    List<Map<String, Object>> getLowFeedItems();
    void adjustStock(String itemName, InventoryCategory category, int quantityChange, String unit, String note);
}
