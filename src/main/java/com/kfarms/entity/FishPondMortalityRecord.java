package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "fish_pond_mortality_record")
@Data
@NoArgsConstructor
public class FishPondMortalityRecord extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "pond_id", nullable = false)
    private FishPond pond;

    @Column(nullable = false)
    private Integer count;

    @Column(name = "mortality_date", nullable = false)
    private LocalDate mortalityDate;

    @Column(length = 255)
    private String note;
}
