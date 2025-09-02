package com.kfarms.service.impl;

import com.kfarms.entity.Hatch;
import com.kfarms.repository.HatchRepository;
import com.kfarms.service.HatchService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HatchServiceImpl implements HatchService {
    private final HatchRepository repo;
    public HatchServiceImpl(HatchRepository repo){this.repo = repo;}
    public List<Hatch> getAll(){ return repo.findAll();}
    public Hatch getById(Long id){return repo.findById(id).orElse(null);}
    public Hatch save(Hatch hatch){
        return repo.save(hatch);
    }
    public void delete(Long id){ repo.deleteById(id); }
}
