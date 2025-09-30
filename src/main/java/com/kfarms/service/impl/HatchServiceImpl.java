package com.kfarms.service.impl;

import com.kfarms.entity.FishHatch;
import com.kfarms.repository.HatchRepository;
import com.kfarms.service.HatchService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HatchServiceImpl implements HatchService {
    private final HatchRepository repo;
    public HatchServiceImpl(HatchRepository repo){this.repo = repo;}
    public List<FishHatch> getAll(){ return repo.findAll();}
    public FishHatch getById(Long id){return repo.findById(id).orElse(null);}
    public FishHatch save(FishHatch fishHatch){
        return repo.save(fishHatch);
    }
    public void delete(Long id){ repo.deleteById(id); }
}
