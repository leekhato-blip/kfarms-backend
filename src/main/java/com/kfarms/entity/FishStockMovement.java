package com.kfarms.entity;

import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fish_stock_movement")
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FishStockMovement extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which pond changed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pond_id", nullable = false)
    private FishPond pond;

    // +ve or -ve
    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private Integer stockAfter;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime movementAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}
