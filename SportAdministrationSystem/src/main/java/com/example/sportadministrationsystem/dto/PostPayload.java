package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PostPayload(
        @NotBlank String title,
        @NotBlank String body,
        @NotNull LocalDateTime publishAt,
        @NotBlank String audience,
        @NotBlank String channel,
        String status
) {
}
