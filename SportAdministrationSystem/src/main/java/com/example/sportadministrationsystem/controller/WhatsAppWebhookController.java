package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.repository.ProcessedWhatsAppMessageRepository;
import com.example.sportadministrationsystem.service.WhatsAppService;
import com.example.sportadministrationsystem.service.WhatsAppSignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppSignatureVerifier signatureVerifier;
    private final ProcessedWhatsAppMessageRepository processedMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${whatsapp.webhook.verify-token}")
    private String verifyToken;

    @Value("${whatsapp.webhook.app-secret:}")
    private String appSecret;

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String body,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String sig
    ) {
        // 1) Перевіряємо підпис для безпеки
        if (!signatureVerifier.isValid(body, sig, appSecret)) {
            log.warn("Invalid X-Hub-Signature-256");
            return ResponseEntity.status(403).build();
        }

        // 2) Витягуємо message_id для ідемпотентності (дедупліку)
        String messageId = extractMessageId(body);

        if (messageId != null && processedMessageRepository.existsByMessageId(messageId)) {
            log.debug("Webhook message_id={} already processed, skipping", messageId);
            return ResponseEntity.ok().build(); // Швидко повертаємо, не обробляємо дублікат
        }

        // 3) Швидко повертаємо 200 OK (ack), потім обробляємо асинхронно
        try {
            if (messageId != null) {
                log.debug("Received WhatsApp webhook with message_id: {}", messageId);
            }

            // 4) Запускаємо асинхронну обробку (у сервісі будуть сохраненi processed messages)
            whatsAppService.handleWebhookAsync(body);
        } catch (Exception e) {
            log.error("Failed to process webhook: {}", e.getMessage(), e);
            // Все одно повертаємо 200, щоб Meta не повторював
        }

        // 5) Повертаємо успіх — обробка буде асинхронна
        return ResponseEntity.ok().build();
    }

    /**
     * Витягує message_id з webhook body для дедупліку.
     * Meta sends: entry[0].changes[0].value.messages[0].id
     */
    private String extractMessageId(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode entry = root.path("entry");
            if (!entry.isArray() || entry.size() == 0) return null;

            JsonNode entryNode = entry.get(0);
            JsonNode changes = entryNode.path("changes");
            if (!changes.isArray() || changes.size() == 0) return null;

            JsonNode changeNode = changes.get(0);
            JsonNode value = changeNode.path("value");

            JsonNode messages = value.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                JsonNode messageNode = messages.get(0);
                if (messageNode != null) {
                    String id = messageNode.path("id").asText(null);
                    if (id != null && !id.isBlank()) {
                        return id;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract message_id: {}", e.getMessage());
        }
        return null;
    }
}
