package com.example.sportadministrationsystem.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users") // V1
@Data
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // V1: BIGSERIAL
    private Long id;

    @Column(nullable = false, unique = true) // V1: username TEXT NOT NULL UNIQUE
    private String username;

    @Column(nullable = false)                // V1: password TEXT NOT NULL
    private String password;

    // V1: user_roles(user_id, role) (role зберігається як TEXT)
    @Builder.Default
    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();
}
