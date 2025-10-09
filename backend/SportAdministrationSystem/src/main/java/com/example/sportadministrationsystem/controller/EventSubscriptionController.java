package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.*;
import com.example.sportadministrationsystem.service.EventService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId:\\d+}/subscriptions")
public class EventSubscriptionController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final UserTelegramRepository userTelegramRepository;
    private final EventSubscriptionRepository subscriptionRepository;

    /** Підписатися на івент (в особисті повідомлення Telegram) */
    @PostMapping("/telegram")
    public ResponseEntity<Void> subscribe(@PathVariable Long eventId, Authentication auth) {
        Event event = eventService.loadEntity(eventId);
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        userTelegramRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Спочатку підключіть Telegram у профілі."));

        subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .ifPresentOrElse(es -> {
                    es.setActive(true);
                    subscriptionRepository.save(es);
                }, () -> {
                    subscriptionRepository.save(EventSubscription.builder()
                            .event(event).user(user)
                            .messenger(Messenger.TELEGRAM)
                            .active(true)
                            .createdAt(LocalDateTime.now())
                            .build());
                });

        return ResponseEntity.noContent().build();
    }

    /** Відписатися від івенту (перестати отримувати DM) */
    @DeleteMapping("/telegram")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long eventId, Authentication auth) {
        Event event = eventService.loadEntity(eventId);
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .ifPresent(subscriptionRepository::delete); // простіше: немає рядка = немає підписки

        return ResponseEntity.noContent().build();
    }

    public record MySubscriptionStatus(@NotNull boolean linked, @NotNull boolean subscribed) {}

    /** Статус підключення і підписки поточного користувача для конкретного івенту */
    @GetMapping("/my")
    public ResponseEntity<MySubscriptionStatus> myStatus(@PathVariable Long eventId, Authentication auth) {
        Event event = eventService.loadEntity(eventId);
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        boolean linked = userTelegramRepository.findByUser(user).isPresent();
        boolean subscribed = subscriptionRepository
                .existsByEventAndUserAndMessengerAndActiveIsTrue(event, user, Messenger.TELEGRAM);

        return ResponseEntity.ok(new MySubscriptionStatus(linked, subscribed));
    }
}
