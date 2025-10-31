package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TelegramAccountProvisioner {

    private final UserTelegramRepository userTelegramRepository;

    /**
     * Забезпечує існування запису в user_telegram і повертає його.
     * НІЯКИХ створень записів у таблиці users.
     */
    @Transactional
    public UserTelegram ensure(org.telegram.telegrambots.meta.api.objects.User tg) {
        Long tgUserId = tg.getId();
        Long chatIdAsPrivate = tg.getId(); // для приватних чатів під час натискання кнопки

        return userTelegramRepository.findByTgUserId(tgUserId)
                .orElseGet(() -> userTelegramRepository.save(
                        UserTelegram.builder()
                                .tgUserId(tgUserId)
                                .tgChatId(chatIdAsPrivate)
                                .build()
                ));
    }
}
