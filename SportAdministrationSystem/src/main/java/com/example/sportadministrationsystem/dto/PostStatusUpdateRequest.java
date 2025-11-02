package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.NotBlank;

public record PostStatusUpdateRequest(@NotBlank String status, String error) {
}
