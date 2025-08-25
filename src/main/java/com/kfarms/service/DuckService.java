package com.kfarms.service;

import com.kfarms.entity.Duck;

import java.util.List;

public interface DuckService {
    List<Duck> getAll();
    Duck getById(Long id);
    Duck save(Duck duck);
    void delete(Long id);
}

