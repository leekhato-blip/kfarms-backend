package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_invoices", indexes = {
        @Index(name = "idx_billing_invoice_tenant", columnList = "tenant_id"),
        @Index(name = "idx_billing_invoice_reference", columnList = "reference")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BillingInvoice extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private BillingSubscription subscription;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingInvoiceStatus status = BillingInvoiceStatus.PAID;

    @Column(nullable = false, length = 100)
    private String reference;

    private LocalDateTime paidAt;
}
