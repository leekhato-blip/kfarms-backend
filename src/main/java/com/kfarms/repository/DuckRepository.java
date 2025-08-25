package com.kfarms.repository;

import com.kfarms.entity.Duck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DuckRepository extends JpaRepository<Duck, Long> {
}
