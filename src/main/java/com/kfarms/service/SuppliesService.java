package com.kfarms.service;

import com.kfarms.entity.Supplies;

import java.util.List;

public interface SuppliesService {
    List<Supplies> getAll();
    Supplies getById(Long id);
    Supplies save(Supplies supplies);
    void delete(Long id);
}
