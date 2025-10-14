package com.kfarms.service;

import com.kfarms.dto.SalesRequestDto;
import com.kfarms.dto.SalesResponseDto;

import java.time.LocalDate;
import java.util.Map;

public interface SalesService {
    SalesResponseDto create(SalesRequestDto dto);
    Map<String, Object> getAll(int page, int size, String itemName, String category, LocalDate date);
    SalesResponseDto getById(Long id);
    SalesResponseDto update(Long id, SalesRequestDto dto, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
}
