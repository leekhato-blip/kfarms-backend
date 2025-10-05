package com.kfarms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.catalina.users.GenericRole;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "fish_hatch_records")
@AllArgsConstructor
@NoArgsConstructor
public class FishHatch extends Auditable{
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fish_pond_id", nullable = false)
    private FishPond pond;


    private int maleCount;
    private int femaleCount;

    private LocalDate hatchDate = LocalDate.now();
    private double hatchRate;
    private int quantityHatched;
    private String note;
}
