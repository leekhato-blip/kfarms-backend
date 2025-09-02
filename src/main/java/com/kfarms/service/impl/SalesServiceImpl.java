package com.kfarms.service.impl;

import com.kfarms.entity.Sales;
import com.kfarms.repository.SalesRepository;
import com.kfarms.service.SalesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesServiceImpl implements SalesService {
    private final SalesRepository repo;
    public SalesServiceImpl(SalesRepository repo){this.repo = repo;}
    public List<Sales> getAll() { return repo.findAll(); }
    public Sales getById(Long id) { return repo.findById(id).orElse(null);}
    public Sales save(Sales sales) {
        return repo.save(sales);
    }
    public void delete(Long id) { repo.deleteById(id);}
}
