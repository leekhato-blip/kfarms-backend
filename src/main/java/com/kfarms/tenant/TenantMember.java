package com.kfarms.tenant;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.context.TenantIdentifierMismatchException;

@Entity
@Data
@NoArgsConstructor
@Table(name = "tenant_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_user", columnNames = {"tenant_id", "user_id"})
}, indexes = {
        @Index(name = "idx_member_tenant", columnList = "tenant_id"),
        @Index(name = "idx_member_user", columnList = "user_id")
})
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

    @Column(nullable = false)
    private Boolean active = true;

}
