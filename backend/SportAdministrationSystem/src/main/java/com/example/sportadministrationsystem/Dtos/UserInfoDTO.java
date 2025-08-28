package com.example.sportadministrationsystem.Dtos;

import com.example.sportadministrationsystem.Models.Role;
import java.util.Set;

public record UserInfoDTO(
        Long id,
        String username,
        Set<Role> roles
) {}
