package com.example.sportadministrationsystem.Models;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDate;
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String location;
    private LocalDate date;
}
