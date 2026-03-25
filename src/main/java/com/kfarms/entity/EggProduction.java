package com.kfarms.entity;

import com.kfarms.mapper.LivestockMapper;
import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Table(name = "egg_production")
@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EggProduction extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation to the livestock batch (usually layer type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Livestock livestock;

    @Column(nullable = false)
    private LocalDate collectionDate = LocalDate.now();

    @Column(nullable = false)
    private int goodEggs = 0; // usable eggs

    @Column(nullable = false)
    private int damagedEggs = 0; // cracked or unusable eggs

    @Column(nullable = false)
    private int cratesProduced = 0; // optional derived field (e.g., goodEggs / 30)

    private String note;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    public void calculateCrates() {
        if (goodEggs > 0) {
            this.cratesProduced = goodEggs / 30; // integer division (floor)
        } else {
            this.cratesProduced = 0;
        }
    }
}
