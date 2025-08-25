package com.kfarms.service;

import com.kfarms.entity.Fowl;

import java.util.List;

public interface FowlService {
    List<Fowl> getAll();
    Fowl getById(Long id);
    Fowl save(Fowl fowl);
    void delete(Long id);
}
