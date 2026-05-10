package com.kfarms.tenant.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Where;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table(name = "tenant_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_user", columnNames = {"tenant_id", "user_id"})
}, indexes = {
        @Index(name = "idx_member_tenant", columnList = "tenant_id"),
        @Index(name = "idx_member_user", columnList = "user_id")
})
@Where(clause = "deleted = false")
public class TenantMember extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant owning this membership
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;


    // User in this tenant
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantRole role = TenantRole.STAFF;

    @Column(name = "custom_role_name", length = 80)
    private String customRoleName;

    @Column(name = "permission_overrides", length = 2000)
    private String permissionOverrides;

    @Column(name = "theme_preference", length = 16)
    private String themePreference = "SYSTEM";

    @Column(name = "landing_page", length = 64)
    private String landingPage = "/dashboard";

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;

    @Column(name = "weekly_summary")
    private Boolean weeklySummary = true;

    @Column(name = "compact_tables")
    private Boolean compactTables = false;

    @Column(nullable = false)
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
