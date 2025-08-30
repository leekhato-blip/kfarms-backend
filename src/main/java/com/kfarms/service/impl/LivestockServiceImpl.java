package com.kfarms.service.impl;

import com.kfarms.dto.LivestockRequest;
import com.kfarms.dto.LivestockResponse;
import com.kfarms.entity.Livestock;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.LivestockMapper;
import com.kfarms.repository.LivestockRepository;
import com.kfarms.service.LivestockService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LivestockServiceImpl implements LivestockService {
    private final LivestockRepository repo;

    // CREATE - save Livestock
    @Override
    public  LivestockResponse create(LivestockRequest request, String createBy){
        Livestock entity = LivestockMapper.toEntity(request);
        entity.setCreatedBy(createBy);
        repo.save(entity);
        return LivestockMapper.toResponse(entity);
    }

    // READ - get all Livestock
    @Override
    public List<LivestockResponse> getAll(){
        return repo.findAll()
                .stream()
                .map(LivestockMapper::toResponse)
                .toList();
    }

    // READ - get Livestock by ID
    @Override
    public LivestockResponse getById(Long id){
        Livestock entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));
        return LivestockMapper.toResponse(entity);
    }

    // UPDATE - update existing Livestock
    @Override
    public LivestockResponse update(Long id,  LivestockRequest request, String updatedBy){
        Livestock entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livestock", "id", id));

        // Update fields from request
        entity.setBatchName(request.getBatchName());
        entity.setQuantity(request.getQuantity());
        entity.setType(request.getType());
        entity.setArrivalDate(request.getArrivalDate());
        entity.setSourceType(request.getSourceType());
        entity.setStartingAgeInWeeks(request.getStartingAgeInWeeks() != null ? request.getStartingAgeInWeeks() : entity.getStartingAgeInWeeks());
        entity.setMortality(request.getMortality() != null ? request.getMortality() : entity.getMortality());
        entity.setNotes(request.getNotes());
        entity.setUpdatedBy(updatedBy);

        repo.save(entity);
        return LivestockMapper.toResponse(entity);
    }

    @Override
    public void delete(Long id){
        if(!repo.existsById(id)){
            throw new ResourceNotFoundException("Livestock", "id", id);
        }
        repo.deleteById(id);
    }
}
