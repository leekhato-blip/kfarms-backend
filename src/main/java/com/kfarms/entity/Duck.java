package com.kfarms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import javax.swing.*;

@Entity
@Table(name = "ducks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Duck extends Auditable{
    @Id
    @GeneratedValue private Long id;
    private String batchName;
    private int quantity;
    private String notes;

}
