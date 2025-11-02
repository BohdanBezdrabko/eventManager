package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PostPayload(
        @NotBlank @Size(max = 256) String title,
        @NotBlank String body,
        @NotNull LocalDateTime publishAt,
        @NotBlank String audience,
        @NotBlank String channel,
        String status,                    // optional
        @Size(max = 255) String telegramChatId // optional per-post override
) {
}
