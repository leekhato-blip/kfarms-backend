package com.kfarms.enterprise.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.entity.Auditable;
import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@Table(
        name = "enterprise_sites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_enterprise_site_code", columnNames = {"tenant_id", "code"})
        },
        indexes = {
                @Index(name = "idx_enterprise_site_tenant", columnList = "tenant_id")
        }
)
public class EnterpriseSite extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 48)
    private String code;

    @Column(length = 180)
    private String location;

    @Column(name = "manager_name", length = 120)
    private String managerName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "poultry_enabled", nullable = false)
    private Boolean poultryEnabled = true;

    @Column(name = "fish_enabled", nullable = false)
    private Boolean fishEnabled = false;

    @Column(name = "poultry_house_count")
    private Integer poultryHouseCount = 0;

    @Column(name = "pond_count")
    private Integer pondCount = 0;

    @Column(name = "active_bird_count")
    private Integer activeBirdCount = 0;

    @Column(name = "fish_stock_count")
    private Integer fishStockCount = 0;

    @Column(name = "current_month_revenue", precision = 14, scale = 2)
    private BigDecimal currentMonthRevenue = BigDecimal.ZERO;

    @Column(name = "current_month_expenses", precision = 14, scale = 2)
    private BigDecimal currentMonthExpenses = BigDecimal.ZERO;

    @Column(name = "current_feed_usage_kg", precision = 12, scale = 2)
    private BigDecimal currentFeedUsageKg = BigDecimal.ZERO;

    @Column(name = "projected_egg_output_30d")
    private Integer projectedEggOutput30d = 0;

    @Column(name = "projected_fish_harvest_kg", precision = 12, scale = 2)
    private BigDecimal projectedFishHarvestKg = BigDecimal.ZERO;

    @Column(name = "current_mortality_rate", precision = 6, scale = 2)
    private BigDecimal currentMortalityRate = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;
}
