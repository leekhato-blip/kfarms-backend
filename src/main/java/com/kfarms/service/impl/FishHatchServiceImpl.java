package com.kfarms.service.impl;

import com.kfarms.dto.FishHatchRequestDto;
import com.kfarms.dto.FishHatchResponseDto;
import com.kfarms.entity.FishHatch;
import com.kfarms.entity.FishPond;
import com.kfarms.exceptions.ResourceNotFoundException;
import com.kfarms.mapper.FishHatchMapper;
import com.kfarms.repository.FishHatchRepository;
import com.kfarms.repository.FishPondRepository;
import com.kfarms.service.FishHatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FishHatchServiceImpl implements FishHatchService {

    private final FishHatchRepository hatchRepo;
    private final FishPondRepository pondRepo;


    // CREATE
    @Override
    public FishHatchResponseDto create(FishHatchRequestDto request){
        FishPond pond = pondRepo.findById(request.getPondId())
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", request.getPondId()));

        FishHatch entity = FishHatchMapper.toEntity(request, pond);
        FishHatch saved = hatchRepo.save(entity);
        return FishHatchMapper.toResponseDto(saved);
    }

    // READ
    @Override
    public List<FishHatchResponseDto> getAll(){
        return hatchRepo.findAll()
                .stream()
                .map(FishHatchMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // READ
    @Override
    public FishHatchResponseDto getById(Long id){
        FishHatch hatch = hatchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));
        return FishHatchMapper.toResponseDto(hatch);
    }

    // UPDATE
    @Override
    public FishHatchResponseDto update(Long id, FishHatchRequestDto request, String updatedBy) {
        FishHatch entity = hatchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FishHatch", "id", id));

        FishPond pond = pondRepo.findById(request.getPondId())
                .orElseThrow(() -> new ResourceNotFoundException("FishPond", "id", request.getPondId()));

        entity.setPond(pond);
        entity.setHatchDate(request.getHatchDate());
        entity.setMaleCount(request.getMaleCount());
        entity.setFemaleCount(request.getFemaleCount());
        entity.setQuantityHatched(request.getQuantityHatched());
        entity.setNote(request.getNote());
        entity.setUpdatedBy(updatedBy);

        // recalculate hatch rate
        int totalParents = request.getMaleCount() + request.getFemaleCount();
        entity.setHatchRate(totalParents > 0
                ? (double) request.getQuantityHatched() / totalParents * 100
                : 0.0);

        hatchRepo.save(entity);
        return FishHatchMapper.toResponseDto(entity);
    }

    // DELETE
    @Override
    public void delete(Long id){ hatchRepo.deleteById(id); }

    // SUMMARY - analysis and reports
    @Override
    public Map<String, Object> getSummary() {
        long totalRecords = hatchRepo.count();
        Map<String, Long> hatchCountByPond = pondRepo.findAll().stream()
                .collect(Collectors.toMap(FishPond::getPondName, p -> hatchRepo.countByPond(p.getId())));

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalHatchRecords", totalRecords);
        summary.put("hatchCountByPond", hatchCountByPond);
        return summary;
    }
}
