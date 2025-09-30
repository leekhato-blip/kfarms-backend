package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "HatchRecord")
@AllArgsConstructor
@NoArgsConstructor
public class FishHatch extends Auditable{
    @Id
    @GeneratedValue private Long id;

    @ManyToOne
    private FishPond fishPond;

    private LocalDate hatchDate;
    private int maleCount;
    private int femaleCount;
    private double hatchRate;
    private int quantityHatched;
    private String note;
}
