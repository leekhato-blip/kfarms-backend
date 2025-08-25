package com.kfarms.repository;

import com.kfarms.entity.Hatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HatchRepository extends JpaRepository<Hatch, Long> {
}
