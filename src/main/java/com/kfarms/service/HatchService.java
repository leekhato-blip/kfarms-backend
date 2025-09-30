package com.kfarms.service;

import com.kfarms.entity.FishHatch;

import java.util.List;

public interface HatchService {
    List<FishHatch> getAll();
    FishHatch getById(Long id);
    FishHatch save(FishHatch fishHatch);
    void delete(Long id);
}
