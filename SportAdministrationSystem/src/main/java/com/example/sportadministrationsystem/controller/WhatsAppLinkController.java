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

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${whatsapp.business-phone-e164:}")
    private String businessPhoneE164;

    /**
     * Повертає deep-link для підключення WhatsApp: https://wa.me/<phone>?text=START%20<jwt>
     * Користувач натискає посилання та починає чат з WhatsApp-ботом
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
            String token = jwtTokenProvider.generateAccessToken(me);

            String phone = businessPhoneE164.trim();
            if (phone.startsWith("+")) {
                phone = phone.substring(1);
            }

            String text = "START " + token;
            String url = "https://wa.me/" + phone + "?text=" +
                        java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);

            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                Map.of("error", "InternalError", "message", "Failed to generate WhatsApp link: " + e.getMessage())
            );
        }
    }
}
