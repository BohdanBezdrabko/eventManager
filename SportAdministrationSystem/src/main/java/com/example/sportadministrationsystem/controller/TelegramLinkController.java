package com.example.sportadministrationsystem.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram")
public class TelegramLinkController {

    @Value("${telegram.bot.username}")
    private String botUsername;

    /**
     * Повертає deep-link для підключення Telegram без JWT (для безпеки).
     * Користувач натискає посилання та починає чат з Telegram-ботом,
     * який розпочинається з допомоги або інструкцій.
     */
    @GetMapping("/link-url")
    public ResponseEntity<?> linkUrl(@AuthenticationPrincipal UserDetails me) {
        // Перевірка автентифікації
        if (me == null) {
            return ResponseEntity.status(401).body(
                Map.of("error", "Unauthorized", "message", "User not authenticated")
            );
        }

        // Перевірка конфігурації
        if (botUsername == null || botUsername.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "ConfigurationError", "message", "Telegram bot username not configured")
            );
        }

        try {
            // Посилаємо посилання на бота без JWT для безпеки
            // Користувач натиснув — бот покаже меню з виборами
            String url = "https://t.me/" + botUsername;

            return ResponseEntity.ok(Map.of(
                "url", url,
                "instructions", "Натисніть посилання, щоб відкрити Telegram та знайти бота. Слідуйте командам /start та інструкціям для підписки на івенти."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                Map.of("error", "InternalError", "message", "Failed to generate Telegram link: " + e.getMessage())
            );
        }
    }
}
