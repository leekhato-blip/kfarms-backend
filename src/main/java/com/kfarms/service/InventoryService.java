package com.kfarms.service;

import com.kfarms.entity.Inventory;

import java.util.List;

public interface InventoryService {
    List<Inventory> getAll();
    Inventory getById(Long id);
    Inventory save(Inventory inventory);
    void delete(Long id);
}
