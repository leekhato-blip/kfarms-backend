package com.kfarms.service;

import com.kfarms.dto.SuppliesRequestDto;
import com.kfarms.dto.SuppliesResponseDto;

import java.time.LocalDate;
import java.util.Map;

public interface SuppliesService {
    SuppliesResponseDto create(SuppliesRequestDto dto);
    Map<String, Object> getAll(int page, int size, String itemName, String Category, LocalDate date);
    SuppliesResponseDto getById(Long id);
    SuppliesResponseDto update(Long id, SuppliesRequestDto request, String updatedBy);
    void delete(Long id);
    Map<String, Object> getSummary();
}
