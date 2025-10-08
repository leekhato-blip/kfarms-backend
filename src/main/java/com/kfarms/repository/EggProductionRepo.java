package com.kfarms.repository;

import com.kfarms.entity.EggProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EggProductionRepo extends JpaRepository<EggProduction, Long>, JpaSpecificationExecutor<EggProduction> {

}
