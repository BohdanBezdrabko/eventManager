package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Створює/оновлює запис UserTelegram та синхронізує tg_chat_id,
 * щоб кнопки/переходи з різних контекстів працювали стабільно.
 */
@Component
@RequiredArgsConstructor
public class TelegramAccountProvisioner {

    private final UserTelegramRepository userTelegramRepository;

    @Transactional
    public UserTelegram ensure(org.telegram.telegrambots.meta.api.objects.User tg) {
        Long tgUserId = tg.getId();
        Long privateChatId = tg.getId(); // у приватному чаті chatId == userId

        return userTelegramRepository.findByTgUserId(tgUserId)
                .map(existing -> {
                    // оновлюємо chatId, якщо змінився або був null
                    if (existing.getTgChatId() == null || !existing.getTgChatId().equals(privateChatId)) {
                        existing.setTgChatId(privateChatId);
                    }
                    return existing;
                })
                .orElseGet(() -> userTelegramRepository.save(
                        UserTelegram.builder()
                                .tgUserId(tgUserId)
                                .tgChatId(privateChatId)
                                .build()
                ));
    }
}
