package com.kfarms.repository;

import com.kfarms.entity.Inventory;
import com.kfarms.entity.InventoryCategory;
import org.springframework.data.domain.Pageable;
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

    @Query("""
    SELECT i FROM Inventory i
    WHERE LOWER(TRIM(i.itemName)) = LOWER(TRIM(:itemName))
      AND i.category = :category
      AND i.tenant.id = :tenantId
    """)
    Optional<Inventory> findByItemNameAndCategoryAndTenantId(
            @Param("itemName") String itemName,
            @Param("category") InventoryCategory category,
            @Param("tenantId") Long tenantId
    );

    // Find all updated between a range (refined param names for clarity)
    @Query("""
    SELECT i FROM Inventory i
    WHERE i.tenant.id = :tenantId
      AND i.lastUpdated BETWEEN :start AND :end
      AND (i.deleted IS NULL OR i.deleted = FALSE)
    """)
    List<Inventory> findByLastUpdatedBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    SELECT i FROM Inventory i
    WHERE i.tenant.id = :tenantId
      AND (i.deleted IS NULL OR i.deleted = FALSE)
      AND i.lastUpdated BETWEEN :start AND :end
    ORDER BY i.lastUpdated DESC, i.id DESC
    """)
    List<Inventory> findAllByLastUpdatedBetween(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
    select i from Inventory i
    where i.tenant.id = :tenantId
      and (i.deleted = false or i.deleted is null)
      and lower(i.itemName) like lower(concat('%', :q, '%'))
    order by i.lastUpdated desc
    """)
    List<Inventory> searchByItemName(@Param("tenantId") Long tenantId, @Param("q") String q, Pageable pageable);

//    long countByTenantId(Long tenantId);

    Optional<Inventory> findByIdAndTenant_Id(Long id, Long tenantId);

    @Query("""
    select i from Inventory i
    where i.tenant.id = :tenantId
      and (i.deleted = false or i.deleted is null)
    order by i.lastUpdated desc, i.id desc
    """)
    List<Inventory> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

}
