package com.kfarms.repository;

import com.kfarms.entity.BillingSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, Long> {
    Optional<BillingSubscription> findByTenant_Id(Long tenantId);
    Optional<BillingSubscription> findByProviderSubscriptionCode(String providerSubscriptionCode);
    Optional<BillingSubscription> findByProviderCustomerCode(String providerCustomerCode);
}
