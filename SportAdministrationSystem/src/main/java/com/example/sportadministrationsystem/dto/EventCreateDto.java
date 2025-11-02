package com.example.sportadministrationsystem.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventCreateDto {
    @NotBlank
    private String name;

    @NotNull
    private LocalDateTime startAt;

    @NotBlank
    private String location;

    @Positive
    private Integer capacity;

    @Size(max = 2048)
    private String description;

    @Size(max = 2048)
    private String coverUrl;

}
