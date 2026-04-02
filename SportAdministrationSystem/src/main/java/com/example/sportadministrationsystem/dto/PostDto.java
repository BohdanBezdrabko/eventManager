package com.example.sportadministrationsystem.dto;

import java.time.LocalDateTime;

public record PostDto(
        Long id,
        Long eventId,
        String title,
        String body,
        LocalDateTime publishAt,
        String status,
        String audience,
        String channel,
        String externalId,
        String error,
        boolean generated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
