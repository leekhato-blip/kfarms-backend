package com.kfarms.service;

import com.kfarms.dto.FeedResponseDto;
import com.kfarms.dto.FeedRequestDto;


import javax.management.ObjectName;
import java.util.List;
import java.util.Map;

public interface FeedService {
    FeedResponseDto save(FeedRequestDto dto);
    Map<String, Object> getAll(int page, int size, String batchType);
    FeedResponseDto getById(Long id);
    FeedResponseDto update(Long id, FeedRequestDto request, String updatedBy);
    void delete(Long id);
    Map<String, Object> getSummary();
}

