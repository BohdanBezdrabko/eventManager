// dto/EventDto.java
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
        String category,
        List<String> tags
) {}
