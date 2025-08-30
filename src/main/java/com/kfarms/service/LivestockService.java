package com.kfarms.service;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;

import java.util.List;

public interface LivestockService {
    LivestockResponse create(LivestockRequest request, String createdBy);
    LivestockResponse update(Long id, LivestockRequest request, String updatedBy);
    LivestockResponse getById(Long id);
    List<LivestockResponse> getAll();
    void delete(Long id);
}
