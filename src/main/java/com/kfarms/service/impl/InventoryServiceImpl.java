package com.kfarms.service.impl;

import com.kfarms.entity.Inventory;
import com.kfarms.repository.InventoryRepository;
import com.kfarms.service.InventoryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository repo;

    public InventoryServiceImpl(InventoryRepository repo) {
        this.repo = repo;
    }
    public List<Inventory> getAll() {

        return repo.findAll();
    }
    public Inventory getById(Long id) {
        return repo.findById(id).orElse(null);
    }
    public Inventory save(Inventory inventory) {
        return repo.save(inventory);
    }
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
