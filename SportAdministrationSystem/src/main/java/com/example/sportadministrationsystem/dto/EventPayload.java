package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class EventPayload {
    @NotBlank
    private String name;

    @NotNull
    private LocalDateTime startAt;

    private String location;

    @Positive
    private Integer capacity;

    @Size(max = 2048)
    private String description;

    @Size(max = 2048)
    private String coverUrl;

    @Size(max = 64)
    private String category;                    // приходить як String

    private List<@Size(max = 128) String> tags; // імена тегів
}
