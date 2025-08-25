package com.kfarms.service;

import com.kfarms.dto.EggDto;
import com.kfarms.entity.Egg;

import java.time.LocalDate;
import java.util.List;

public interface EggService {
    List<Egg> getAll(Long layerId);
    Egg getById(Long id);
    Egg save(EggDto dto);
    List<Egg> getFilteredEggs(Long layerId, LocalDate date);
    void delete(Long id);
}
