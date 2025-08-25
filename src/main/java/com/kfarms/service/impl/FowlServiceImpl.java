package com.kfarms.service.impl;

import com.kfarms.entity.Fowl;
import com.kfarms.repository.FowlRepository;
import com.kfarms.service.FowlService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FowlServiceImpl implements FowlService {
    private final FowlRepository repo;
    public FowlServiceImpl(FowlRepository repo){this.repo = repo;}
    public List<Fowl> getAll(){return repo.findAll();}
    public Fowl getById(Long id){return repo.findById(id).orElse(null);}
    public Fowl save(Fowl fowl){
        return repo.save(fowl);
    }
    public void delete(Long id){repo.deleteById(id);}
}
