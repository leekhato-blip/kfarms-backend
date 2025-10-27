package com.kfarms.repository;

import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    @Query("SELECT i FROM Inventory i WHERE i.lastUpdated BETWEEN :startDate AND :endDate")
    List<Inventory> findByLastUpdatedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

}
