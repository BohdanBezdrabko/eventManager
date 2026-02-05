package com.example.sportadministrationsystem.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_whatsapp")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wa_id", nullable = false, unique = true, length = 32)
    private String waId;

    @Column(name = "profile_name")
    private String profileName;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    void prePersist() {
        if (linkedAt == null) linkedAt = LocalDateTime.now();
    }
}
