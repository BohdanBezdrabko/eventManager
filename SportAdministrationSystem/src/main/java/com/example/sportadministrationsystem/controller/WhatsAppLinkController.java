package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppLinkController {

    @Value("${whatsapp.business-phone-e164:}")
    private String businessPhoneE164;

    /**
     * Повертає deep-link для підключення WhatsApp без JWT (для безпеки).
     * Користувач натискає посилання та починає чат з WhatsApp-ботом,
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

        if (businessPhoneE164 == null || businessPhoneE164.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "ConfigurationError", "message", "WhatsApp integration not configured")
            );
        }

        try {
            String phone = businessPhoneE164.trim();
            if (phone.startsWith("+")) {
                phone = phone.substring(1);
            }

            // Посилаємо простий текст без JWT для безпеки
            String text = "Привіт! Хочу отримувати оновлення про івенти.";
            String url = "https://wa.me/" + phone + "?text=" +
                        java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);

            return ResponseEntity.ok(Map.of(
                "url", url,
                "instructions", "Натисніть посилання, щоб відкрити WhatsApp та начати чат з ботом. Слідуйте командам в чаті для підписки на івенти."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                Map.of("error", "InternalError", "message", "Failed to generate WhatsApp link: " + e.getMessage())
            );
        }
    }
}
