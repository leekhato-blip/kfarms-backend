package com.kfarms.service.impl;

import com.kfarms.entity.Layer;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.repository.LayerRepository;
import com.kfarms.service.LayerService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ServiceConfigurationError;

@Service
public class LayerServiceImpl implements LayerService {
    private final LayerRepository repo;
    public LayerServiceImpl(LayerRepository repo){this.repo = repo;}
    public List<Layer> getAll(){return repo.findAll();}
    public Layer getById(Long id){return repo.findById(id).orElse(null);}
    public Layer save(Layer layer){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "system";

        // TODO: Add owner or role check before allowing update
        // e.g., Only allow if layer.getCreatedBy().equals(username) or user has ADMIN role

        if(layer.getId() == null){
            layer.setCreatedBy(username);
        }
        layer.setUpdatedBy(username);

        return repo.save(layer);
    }
    public void delete(Long id){
        Layer layer = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Layers", "id", id));
        repo.delete(layer);
    }
}

