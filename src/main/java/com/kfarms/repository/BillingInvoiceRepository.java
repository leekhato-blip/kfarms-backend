package com.kfarms.repository;

import com.kfarms.entity.BillingInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, Long> {
    Page<BillingInvoice> findByTenant_Id(Long tenantId, Pageable pageable);
    Optional<BillingInvoice> findByIdAndTenant_Id(Long id, Long tenantId);
    Optional<BillingInvoice> findFirstByTenant_IdAndReferenceOrderByIdDesc(Long tenantId, String reference);
}
