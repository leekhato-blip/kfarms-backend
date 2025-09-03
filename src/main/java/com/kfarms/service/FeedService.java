package com.kfarms.service;

import com.kfarms.dto.FeedResponseDto;
import com.kfarms.entity.Feed;
import com.kfarms.dto.FeedRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeedService {
    Page<FeedResponseDto> getAll(String batchType, String feedName, Pageable pageable);
    FeedResponseDto getById(Long id);
    FeedResponseDto save(FeedRequestDto dto);
    void delete(Long id);
}

