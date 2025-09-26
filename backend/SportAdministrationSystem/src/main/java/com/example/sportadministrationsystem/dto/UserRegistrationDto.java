package com.example.sportadministrationsystem.dto;

import java.time.LocalDate;

public record UserRegistrationDto(
        Long id,
        Long userId,
        String username,
        Long eventId,
        String eventTitle,
        LocalDate registrationDate
) {}