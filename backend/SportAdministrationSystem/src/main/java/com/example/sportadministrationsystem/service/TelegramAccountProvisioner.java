package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TelegramAccountProvisioner {

    private final UserRepository userRepository;
    private final UserTelegramRepository userTelegramRepository;

    /**
     * Створює "тіньового" користувача та зв'язок у user_telegram.
     * Враховано, що в схемі tg_chat_id NOT NULL — ставимо tgChatId = tgUserId.
     */
    @Transactional
    public User provisionShadow(Long tgUserId, String tgUsername, String firstName, String lastName) {
        // username має бути унікальним; формуємо стабільний
        String username = (tgUsername != null && !tgUsername.isBlank())
                ? tgUsername
                : ("tg_" + tgUserId);

        // Мінімально необхідні поля
        User user = User.builder()
                .username(username)
                .password("tg-shadow") // не використовується для логіну; головне — NOT NULL
                .build();
        user = userRepository.save(user);

        UserTelegram ut = UserTelegram.builder()
                .user(user)
                .tgUserId(tgUserId)
                .tgChatId(tgUserId) // важливо для NOT NULL у схемі
                .build();
        userTelegramRepository.save(ut);

        return user;
    }
}
