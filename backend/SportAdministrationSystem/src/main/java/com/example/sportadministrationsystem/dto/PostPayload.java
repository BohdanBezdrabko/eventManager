package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record PostPayload(
        @NotBlank @Size(max = 256) String title,
        @NotBlank String body,
        @NotNull LocalDateTime publishAt,
        @NotBlank String audience, // PUBLIC|SUBSCRIBERS
        @NotBlank String channel,  // TELEGRAM|INTERNAL
        String status              // optional on create: DRAFT|SCHEDULED
) {
}
