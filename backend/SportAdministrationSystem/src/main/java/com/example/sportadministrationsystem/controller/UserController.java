package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.UserInfoDTO;
import com.example.sportadministrationsystem.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/user")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/person")
    public UserInfoDTO getCurrentUser() {
        return userService.getCurrentUserInfo();
    }
}
