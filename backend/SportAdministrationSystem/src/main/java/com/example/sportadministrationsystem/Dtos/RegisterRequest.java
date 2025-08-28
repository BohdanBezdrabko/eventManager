package com.example.sportadministrationsystem.Dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegisterRequest(
        String username,
        String password,
        @JsonProperty("role") String role // "participant" або "creator"
) {}
