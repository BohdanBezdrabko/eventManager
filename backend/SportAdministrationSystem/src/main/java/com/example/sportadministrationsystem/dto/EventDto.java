package com.example.sportadministrationsystem.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EventDto(
        Long id,
        String name,
        LocalDateTime startAt,
        String location,
        Integer capacity,
        String description,
        String coverUrl,
        String category,          // ENUM name або null
        List<String> tags,        // список назв тегів
        Long createdBy,           // id автора (може бути null)
        String createdByUsername  // username автора (може бути null)
) {}
