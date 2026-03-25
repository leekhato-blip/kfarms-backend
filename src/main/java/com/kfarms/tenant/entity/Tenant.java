package com.kfarms.tenant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.entity.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table(name = "tenant", indexes = {
        @Index(name = "idx_tenant_slug", columnList = "slug", unique = true)
})
public class Tenant extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // slug will be used in URLs later: /t/kfarms/dashboard
    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlan plan = TenantPlan.FREE;

    @Column(length = 64)
    private String timezone = "Africa/Lagos";

    @Column(length = 8)
    private String currency = "NGN";

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "critical_sms_alerts_enabled")
    private Boolean criticalSmsAlertsEnabled = false;

    @Column(length = 500)
    private String address;

    @Column(name = "watermark_enabled")
    private Boolean watermarkEnabled = true;

    @Column(name = "logo_url", length = 1200)
    private String logoUrl;

    @Column(name = "brand_primary_color", length = 16)
    private String brandPrimaryColor = "#2563EB";

    @Column(name = "brand_accent_color", length = 16)
    private String brandAccentColor = "#10B981";

    @Column(name = "login_headline", length = 180)
    private String loginHeadline;

    @Column(name = "login_message", length = 360)
    private String loginMessage;

    @Column(name = "report_footer", length = 320)
    private String reportFooter;

    @Column(name = "custom_domain", length = 180)
    private String customDomain;

    @Column(name = "google_workspace_sso_enabled")
    private Boolean googleWorkspaceSsoEnabled = false;

    @Column(name = "microsoft_entra_sso_enabled")
    private Boolean microsoftEntraSsoEnabled = false;

    @Column(name = "strong_password_policy_enabled")
    private Boolean strongPasswordPolicyEnabled = false;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes = 480;

    @Column(name = "poultry_enabled")
    private Boolean poultryEnabled = false;

    @Column(name = "fish_enabled")
    private Boolean fishEnabled = false;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnore
    private List<TenantMember> members = new ArrayList<>();
}
