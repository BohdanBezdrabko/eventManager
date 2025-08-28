package com.example.sportadministrationsystem.Controllers;

import com.example.sportadministrationsystem.Dtos.UserInfoDTO;
import com.example.sportadministrationsystem.Services.UserService;
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
