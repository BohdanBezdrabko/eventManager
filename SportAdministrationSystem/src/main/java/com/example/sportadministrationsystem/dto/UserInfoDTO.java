package com.example.sportadministrationsystem.dto;

import com.example.sportadministrationsystem.model.Role;
import java.util.Set;

public record UserInfoDTO(
        Long id,
        String username,
        Set<Role> roles
) {}
