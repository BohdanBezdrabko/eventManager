package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import com.example.sportadministrationsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TelegramService extends TelegramLongPollingBot {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserTelegramRepository userTelegramRepository;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /** Обробка апдейтів: /start <jwt> для лінкування та /unlink для відв'язки */
    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) return;

        String text = update.getMessage().getText().trim();
        Long tgUserId = update.getMessage().getFrom().getId();
        Long tgChatId = update.getMessage().getChatId();

        if (text.startsWith("/start")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length < 2) {
                safeReply(tgChatId, "Привіт! Відкрий посилання «Підключити Telegram» у веб-кабінеті ще раз.");
                return;
            }
            String jwt = parts[1];
            try {
                String username = jwtTokenProvider.getUsername(jwt);
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalStateException("User not found"));

                userTelegramRepository.findByUser(user).ifPresentOrElse(
                        ut -> {
                            ut.setTgUserId(tgUserId);
                            ut.setTgChatId(tgChatId);
                            ut.setLinkedAt(LocalDateTime.now());
                            userTelegramRepository.save(ut);
                        },
                        () -> {
                            UserTelegram ut = UserTelegram.builder()
                                    .user(user)
                                    .tgUserId(tgUserId)
                                    .tgChatId(tgChatId)
                                    .linkedAt(LocalDateTime.now())
                                    .build();
                            userTelegramRepository.save(ut);
                        }
                );

                safeReply(tgChatId, "Готово! Telegram підключено ✅ Тепер ви можете підписуватися на події.");
            } catch (Exception e) {
                safeReply(tgChatId, "Посилання недійсне або протерміноване. Спробуйте ще раз із кабінету.");
            }
            return;
        }

        if ("/unlink".equalsIgnoreCase(text)) {
            userTelegramRepository.findByTgUserId(tgUserId).ifPresent(ut -> {
                userTelegramRepository.delete(ut);
                safeReply(tgChatId, "Telegram від'єднано. Ви більше не отримуватимете приватні сповіщення.");
            });
        }
    }

    // Відправка тексту
    public void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        execute(msg);
    }

    private void safeReply(Long chatId, String text) {
        try { sendMessage(String.valueOf(chatId), text); } catch (TelegramApiException ignored) {}
    }
}
