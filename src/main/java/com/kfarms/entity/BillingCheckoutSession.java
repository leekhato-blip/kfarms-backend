package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantPlan;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_checkout_sessions", indexes = {
        @Index(name = "idx_billing_checkout_tenant", columnList = "tenant_id"),
        @Index(name = "idx_billing_checkout_reference", columnList = "reference", unique = true)
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BillingCheckoutSession extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlan plan = TenantPlan.PRO;

    @Column(nullable = false, length = 40)
    private String provider = "KFARMS";

    @Column(nullable = false, length = 100, unique = true)
    private String reference;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(length = 160)
    private String customerEmail;

    @Column(length = 500)
    private String successUrl;

    @Column(length = 500)
    private String cancelUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCheckoutStatus status = BillingCheckoutStatus.PENDING;

    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;
}
