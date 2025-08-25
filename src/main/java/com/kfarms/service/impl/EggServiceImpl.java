package com.kfarms.service.impl;

import com.kfarms.dto.EggDto;
import com.kfarms.entity.Egg;
import com.kfarms.entity.Layer;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.EggMapper;
import com.kfarms.repository.EggRepository;
import com.kfarms.repository.LayerRepository;
import com.kfarms.service.EggService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EggServiceImpl implements EggService {
    private final EggRepository repo;
    private final LayerRepository layerRepo;

    public EggServiceImpl(EggRepository repo, LayerRepository layerRepo){
        this.repo = repo;
        this.layerRepo = layerRepo;
    }

    public List<Egg> getAll(Long layerId) {
        if (layerId != null){
            return repo.findByLayerId(layerId);
        } else {
            return repo.findAll();
        }
    }
    public Egg getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public Egg save(EggDto dto) {
        Egg egg;
        boolean isNew = false;
        if(dto.getId() != null && repo.existsById(dto.getId())){
            egg = repo.findById(dto.getId()).orElse(new Egg());
        } else {
            egg = new Egg();
            isNew = true;
        }
        egg.setQuantity(dto.getQuantity());
        egg.setCollectionDate(dto.getCollectionDate());
        egg.setNotes(dto.getNotes());

        if (dto.getLayerId() != null){
            Layer layer = layerRepo.findById(dto.getLayerId()).orElse(null);
            egg.setLayer(layer); // if layer is null, it will set null
        }
        // 🌙 Set auditing fields
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "system";
        if(isNew){
            egg.setCreatedBy(username);
        }
        egg.setUpdatedBy(username);
        return repo.save(egg);
    }
    public List<Egg> getFilteredEggs(Long layerId, LocalDate date){
        if (layerId != null && date != null){
            return repo.findByLayerIdAndCollectionDate(layerId, date);
        } else if (layerId != null){
            return repo.findByLayerId(layerId);
        } else if (date != null){
            return repo.findByCollectionDate(date);
        } else {
            return repo.findAll();
        }
    }
    public void delete(Long id) {
        Egg egg = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Eggs", "id", id));
        repo.delete(egg);
    }
}

