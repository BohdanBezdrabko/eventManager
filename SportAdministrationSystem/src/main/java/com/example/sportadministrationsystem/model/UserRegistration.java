package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_events") // V3
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // V3: BIGSERIAL
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) // V3: user_id NOT NULL
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) // V3: event_id NOT NULL
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // V3: registration_date DATE NOT NULL DEFAULT CURRENT_DATE
    @Column(nullable = false)
    private LocalDate registrationDate;

    @PrePersist
    void prePersist() {
        if (registrationDate == null) {
            registrationDate = LocalDate.now();
        }
    }
}
