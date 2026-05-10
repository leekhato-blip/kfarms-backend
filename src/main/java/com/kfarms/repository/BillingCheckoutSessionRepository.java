package com.kfarms.repository;

import com.kfarms.entity.BillingCheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingCheckoutSessionRepository extends JpaRepository<BillingCheckoutSession, Long> {
    Optional<BillingCheckoutSession> findByReferenceAndTenant_Id(String reference, Long tenantId);
    Optional<BillingCheckoutSession> findByReference(String reference);
}
