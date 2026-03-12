package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "billing_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_billing_subscription_tenant", columnNames = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BillingSubscription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlan plan = TenantPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingSubscriptionStatus status = BillingSubscriptionStatus.ACTIVE;

    @Column(nullable = false, length = 40)
    private String provider = "NONE";

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(name = "billing_interval", nullable = false, length = 20)
    private String billingInterval = "MONTHLY";

    private LocalDate nextBillingDate;

    @Column(nullable = false)
    private Boolean cancelAtPeriodEnd = false;

    @Column(length = 100)
    private String subscriptionReference;

    @Column(length = 100)
    private String providerCustomerCode;

    @Column(length = 100)
    private String providerPlanCode;

    @Column(length = 100)
    private String providerSubscriptionCode;

    @Column(length = 120)
    private String providerSubscriptionToken;

    @Column(length = 120)
    private String authorizationCode;

    @Column(length = 30)
    private String paymentMethodBrand;

    @Column(length = 4)
    private String paymentMethodLast4;

    @Column(length = 40)
    private String paymentChannel;
}
