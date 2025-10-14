package com.kfarms.service;

import com.kfarms.dto.EggProductionRequestDto;
import com.kfarms.dto.EggProductionResponseDto;

import java.time.LocalDate;
import java.util.Map;

public interface EggProductionService {

    EggProductionResponseDto create(EggProductionRequestDto request);
    Map<String, Object> getAll(int page, int size, Long livestockId, LocalDate collectionDate);
    EggProductionResponseDto getById(Long id);
    EggProductionResponseDto update(Long id, EggProductionRequestDto request, String updatedBy);
    void delete(Long id, String deletedBy);
    void restore(Long id);
    Map<String, Object> getSummary();

}
