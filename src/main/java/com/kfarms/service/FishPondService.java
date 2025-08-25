package com.kfarms.service;

import com.kfarms.entity.FishPond;

import java.util.List;

public interface FishPondService {
    List<FishPond> getAll();
    FishPond getById(Long id);
    FishPond save(FishPond fishPond);
    void delete(Long id);
}
