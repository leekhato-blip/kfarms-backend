package com.kfarms.service;

import com.kfarms.dto.FeedResponseDto;
import com.kfarms.dto.FeedRequestDto;


import java.time.LocalDate;
import java.util.Map;

public interface FeedService {
    FeedResponseDto create(FeedRequestDto dto);
    Map<String, Object> getAll(int page, int size, String batchType, LocalDate date);
    FeedResponseDto getById(Long id);
    FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
}

