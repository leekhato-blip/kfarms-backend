package com.kfarms.service;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;

import java.util.Map;

public interface SuppliesService {
    Map<String, Object> getAll(int page, int size, String itemName, String Supplier);
    SuppliesResponseDto getById(Long id);
    SuppliesResponseDto save(SuppliesRequestDto dto);
    SuppliesResponseDto update(Long id, SuppliesRequestDto request, String updatedBy);
    void delete(Long id);
    Map<String, Object> getSummary();
}
