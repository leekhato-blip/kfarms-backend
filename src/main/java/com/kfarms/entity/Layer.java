package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Layer")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Layer extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String batchName;
    private int quantity;
    private LocalDate arrivalDate;
    private int ageInWeeks;
    private String notes;

    @OneToMany(mappedBy = "layer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Egg> eggs = new ArrayList<>();
}
