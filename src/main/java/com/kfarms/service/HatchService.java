package com.kfarms.service;

import com.kfarms.entity.Hatch;

import java.util.List;

public interface HatchService {
    List<Hatch> getAll();
    Hatch getById(Long id);
    Hatch save(Hatch hatch);
    void delete(Long id);
}
