package com.kfarms.repository;

import com.kfarms.entity.BillingSubscription;
import com.kfarms.entity.BillingSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, Long> {
    Optional<BillingSubscription> findByTenant_Id(Long tenantId);
    Optional<BillingSubscription> findByProviderSubscriptionCode(String providerSubscriptionCode);
    Optional<BillingSubscription> findByProviderCustomerCode(String providerCustomerCode);
    Optional<BillingSubscription> findFirstByStatusOrderByIdAsc(BillingSubscriptionStatus status);

    @Query("""
    select coalesce(sum(subscription.amount), 0)
    from BillingSubscription subscription
    where subscription.status = com.kfarms.entity.BillingSubscriptionStatus.ACTIVE
    """)
    BigDecimal sumActiveRecurringRevenue();
}
