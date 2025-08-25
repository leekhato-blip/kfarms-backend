package com.kfarms.service.impl;

import com.kfarms.entity.Duck;
import com.kfarms.repository.DuckRepository;
import com.kfarms.service.DuckService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DuckServiceImpl implements DuckService {
    private final DuckRepository repo;
    public DuckServiceImpl(DuckRepository repo){
        this.repo = repo;
    }
    public List<Duck> getAll(){
        return repo.findAll();
    }
    public Duck getById(Long id){
        return repo.findById(id).orElse(null);
    }
    public Duck save(Duck duck){
        return repo.save(duck);
    }
    public void delete(Long id){
        repo.deleteById(id);
    }

}
