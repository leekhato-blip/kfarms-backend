package com.kfarms.service.impl;

import com.kfarms.entity.FishPond;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishPondService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FishPondServiceImpl implements FishPondService {
    private final FishPondRepository repo;
    public FishPondServiceImpl(FishPondRepository repo){this.repo = repo;}
    public List<FishPond> getAll(){return repo.findAll();}
    public FishPond getById(Long id){return repo.findById(id).orElse(null);}
    public FishPond save(FishPond fishPond){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";

        if(fishPond.getId() == null ){
            fishPond.setCreatedBy(username);
        }
        fishPond.setUpdatedBy(username);
        return repo.save(fishPond);
        //return repo.save(fishPond);
    }
    public void delete(Long id){ repo.deleteById(id);}
}
