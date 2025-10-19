package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Role;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramAccountProvisioner {

    private final UserRepository users;
    private final UserTelegramRepository userTg;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User provisionShadow(Long tgUserId, String tgUsername, String firstName, String lastName) {
        Optional<UserTelegram> existing = userTg.findByTgUserId(tgUserId);
        if (existing.isPresent()) return existing.get().getUser();

        String base = (tgUsername != null && !tgUsername.isBlank())
                ? ("tg_" + tgUsername.toLowerCase())
                : ("tg_" + tgUserId);
        String candidate = base;
        int i = 0;
        while (users.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + (++i);
        }

        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User u = User.builder()
                .username(candidate)
                .password(randomPassword)
                .roles(Set.of(Role.ROLE_USER))
                .build();
        u = users.save(u);

        UserTelegram map = UserTelegram.builder()
                .user(u)
                .tgUserId(tgUserId)
                .tgChatId(tgUserId) // приватний чат користувача = його tg user id
                .linkedAt(LocalDateTime.now())
                .build();
        userTg.save(map);

        return u;
    }
}
