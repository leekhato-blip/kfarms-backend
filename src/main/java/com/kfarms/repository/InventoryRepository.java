package com.kfarms.repository;

import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByItemNameAndCategory(String itemName, InventoryCategory category);
}
