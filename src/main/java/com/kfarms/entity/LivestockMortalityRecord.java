package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "livestock_mortality_record")
@Data
@NoArgsConstructor
public class LivestockMortalityRecord extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "livestock_id", nullable = false)
    private Livestock livestock;

    @Column(nullable = false)
    private Integer count;

    @Column(name = "mortality_date", nullable = false)
    private LocalDate mortalityDate;

    @Column(length = 255)
    private String note;
}
