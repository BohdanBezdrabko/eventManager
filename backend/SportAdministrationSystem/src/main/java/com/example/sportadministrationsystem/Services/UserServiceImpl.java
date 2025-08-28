package com.example.sportadministrationsystem.Services;

import com.example.sportadministrationsystem.Dtos.UserInfoDTO;
import com.example.sportadministrationsystem.Models.User;
import com.example.sportadministrationsystem.Repositories.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserInfoDTO getCurrentUserInfo() {
        // Отримуємо username із JWT через SecurityContextHolder
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // Шукаємо користувача в базі
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Повертаємо DTO
        return new UserInfoDTO(user.getId(), user.getUsername(), user.getRoles());
    }
}
