package com.kfarms.service;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;

import java.util.List;
import java.util.Map;

public interface FishHatchService {
    List<FishHatchResponseDto> getAll();
    FishHatchResponseDto getById(Long id);
    FishHatchResponseDto create(FishHatchRequestDto request);
    FishHatchResponseDto update(Long id, FishHatchRequestDto request, String updateBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();
}
