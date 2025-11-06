package com.kfarms.repository;

import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    // Find a specific inventory item by name and category (case-insensitive, trims handled in service)
    @Query("SELECT i FROM Inventory i WHERE LOWER(TRIM(i.itemName)) = LOWER(TRIM(:itemName)) AND i.category = :category")
    Optional<Inventory> findByItemNameAndCategory(@Param("itemName") String itemName, @Param("category") InventoryCategory category);

    // Find all updated between a range (refined param names for clarity)
    @Query("SELECT i FROM Inventory i WHERE i.lastUpdated BETWEEN :start AND :end AND (i.deleted IS NULL OR i.deleted = FALSE)")
    List<Inventory> findByLastUpdatedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Inventory> findAllByLastUpdatedBetween(LocalDate start, LocalDate end);
}

