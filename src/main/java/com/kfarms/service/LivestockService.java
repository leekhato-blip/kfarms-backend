package com.kfarms.service;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface LivestockService {
    List<LivestockResponse> getAll();
    LivestockResponse getById(Long id);
    LivestockResponse create(LivestockRequest request, String createdBy);
    LivestockResponse update(Long id, LivestockRequest request, String updatedBy);
    void delete(Long id);
    Map<String, Object> getSummary();
    List<LivestockResponse> search(String batchName, String type, LocalDate arrivalDate);
}
