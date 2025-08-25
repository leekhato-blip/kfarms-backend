package com.kfarms.service;

import com.kfarms.entity.Layer;

import java.util.List;

public interface LayerService {
    List<Layer> getAll();
    Layer getById(Long id);
    Layer save(Layer layer);
    void delete(Long id);
}
