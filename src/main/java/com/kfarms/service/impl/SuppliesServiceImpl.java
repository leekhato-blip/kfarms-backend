package com.kfarms.service.impl;

import com.kfarms.entity.Supplies;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.service.SuppliesService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuppliesServiceImpl implements SuppliesService {
    private final SuppliesRepository repo;
    public SuppliesServiceImpl(SuppliesRepository repo) {this.repo = repo;}
    public List<Supplies> getAll(){ return repo.findAll();}
    public Supplies getById(Long id){ return repo.findById(id).orElse(null);}
    public Supplies save(Supplies supplies){
        return repo.save(supplies);
    }
    public void delete(Long id) { repo.deleteById(id);}
}
