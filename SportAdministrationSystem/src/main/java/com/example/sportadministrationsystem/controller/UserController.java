package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.UserInfoDTO;
import com.example.sportadministrationsystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    // Отримати дані про поточного користувача
    @GetMapping("/me")
    public ResponseEntity<UserInfoDTO> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUserInfo());
    }
}
