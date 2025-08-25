package com.kfarms.repository;

import com.kfarms.entity.Layer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LayerRepository extends JpaRepository<Layer, Long> {
}
