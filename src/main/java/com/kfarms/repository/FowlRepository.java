package com.kfarms.repository;

import com.kfarms.entity.Fowl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FowlRepository extends JpaRepository<Fowl, Long> {
}
