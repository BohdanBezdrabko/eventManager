package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.service.WhatsAppService;
import com.example.sportadministrationsystem.service.WhatsAppSignatureVerifier;
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
        if (!signatureVerifier.isValid(body, sig, appSecret)) {
            log.warn("Invalid X-Hub-Signature-256");
            return ResponseEntity.status(403).build();
        }

        whatsAppService.handleWebhook(body);
        return ResponseEntity.ok().build();
    }
}
