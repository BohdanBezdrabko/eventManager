package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.UserTelegram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTelegramRepository extends JpaRepository<UserTelegram, Long> {
    Optional<UserTelegram> findByTgUserId(Long tgUserId);
    Optional<UserTelegram> findByTgChatId(Long tgChatId);

    // Для сумісності зі старими викликами у коді
    default Optional<UserTelegram> findByTelegramId(Long telegramId) {
        return findByTgUserId(telegramId);
    }
}
