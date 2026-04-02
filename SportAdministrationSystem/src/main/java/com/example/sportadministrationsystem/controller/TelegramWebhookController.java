package com.example.sportadministrationsystem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook контролер для Telegram.
 * Однак, поточна конфігурація використовує long-polling (TelegramLongPollingBot).
 * Цей контролер залишено для майбутнього переходу на webhook-и.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram/webhook")
public class TelegramWebhookController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${telegram.bot.token:}")
    private String botToken;

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String body,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken
    ) {
        try {
            // У поточній реалізації Telegram використовує long-polling
            // Якщо буде перехід на webhook, то потрібно:
            // 1. Перевірити secretToken (якщо передається)
            // 2. Витягнути update_id і перевірити ідемпотентність
            // 3. Обробити асинхронно
            // 4. Повернути 200 OK

            JsonNode root = objectMapper.readTree(body);
            String updateId = root.path("update_id").asText(null);

            if (updateId != null) {
                log.debug("Received Telegram webhook update_id: {}", updateId);
            }

            // TODO: Реалізувати обробку webhook-и (замість long-polling)

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Telegram webhook: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }
    }
}
