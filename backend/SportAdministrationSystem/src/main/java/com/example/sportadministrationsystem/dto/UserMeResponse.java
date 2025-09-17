package com.example.sportadministrationsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMeResponse {
    private Long id;
    private String username;
    private List<String> roles;
}
