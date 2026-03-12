package com.kfarms.tenant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@Table(name = "tenant_audit_logs", indexes = {
        @Index(name = "idx_tenant_audit_tenant_created", columnList = "tenant_id, created_at"),
        @Index(name = "idx_tenant_audit_action", columnList = "action")
})
public class TenantAuditLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TenantAuditAction action;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "target_name", length = 160)
    private String targetName;

    @Column(name = "target_email", length = 180)
    private String targetEmail;

    @Column(name = "previous_value", length = 120)
    private String previousValue;

    @Column(name = "next_value", length = 120)
    private String nextValue;

    @Column(nullable = false, length = 500)
    private String description;
}
