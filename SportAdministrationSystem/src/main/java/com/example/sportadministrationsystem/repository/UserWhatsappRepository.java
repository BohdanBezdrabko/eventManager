package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.UserWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserWhatsappRepository extends JpaRepository<UserWhatsapp, Long> {
    Optional<UserWhatsapp> findByWaId(String waId);
}
