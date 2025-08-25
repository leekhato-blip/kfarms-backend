package com.kfarms.repository;

import com.kfarms.entity.Egg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EggRepository extends JpaRepository<Egg, Long> {
    List<Egg> findByLayerId(Long layerId);
    List<Egg> findByCollectionDate(LocalDate collectionDate);
    List<Egg> findByLayerIdAndCollectionDate(Long layerId, LocalDate collectionDate);
}
