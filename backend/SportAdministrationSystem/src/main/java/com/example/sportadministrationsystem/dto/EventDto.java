package com.example.sportadministrationsystem.dto;

import java.time.LocalDateTime;
public record EventDto(
        Long id,
        String name,
        LocalDateTime startAt,
        String location,
        Integer capacity,
        String description,
        String coverUrl,
        Integer registeredCount,
        LocalDateTime createdAt
) {}
