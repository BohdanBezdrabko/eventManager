package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/telegram")
public class TelegramLinkController {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${telegram.bot.username}")
    private String botUsername;

    /** Повертає deep-link для підключення Telegram: https://t.me/<bot>?start=<jwt> */
    @GetMapping("/link-url")
    public ResponseEntity<String> linkUrl(@AuthenticationPrincipal UserDetails me) {
        String token = jwtTokenProvider.generateAccessToken(me); // TTL у конфізі (access-exp-min)
        String url = "https://t.me/" + botUsername + "?start=" + token;
        return ResponseEntity.ok(url);
    }
}
