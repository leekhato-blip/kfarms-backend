package com.kfarms.service;

import com.kfarms.dto.FeedResponseDto;
import com.kfarms.dto.FeedRequestDto;


import java.util.List;
import java.util.Map;

public interface FeedService {
    Map<String, Object> getAll(int page, int size, String batchType);
    FeedResponseDto getById(Long id);
    FeedResponseDto save(FeedRequestDto dto);
    void delete(Long id);
}

