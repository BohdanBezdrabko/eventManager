package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import com.example.sportadministrationsystem.service.EventService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId:\\d+}/subscriptions")
public class EventSubscriptionController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final UserTelegramRepository userTelegramRepository;
    private final EventSubscriptionRepository subscriptionRepository;

    /** Підписатися (Telegram) поточним користувачем */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@PathVariable Long eventId, Authentication auth) {
        Event event = eventService.loadEntity(eventId);
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Вимога: лінк до Telegram має існувати
        boolean linked = userTelegramRepository.findByUser(user).isPresent();
        if (!linked) {
            return ResponseEntity.badRequest().build();
        }

        EventSubscription sub = subscriptionRepository
                .findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .orElseGet(() -> EventSubscription.builder()
                        .event(event)
                        .user(user)
                        .messenger(Messenger.TELEGRAM)
                        .active(true)
                        .build());

        sub.setActive(true);
        subscriptionRepository.save(sub);

        return ResponseEntity.noContent().build();
    }

    /** Відписатися (Telegram) поточним користувачем */
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long eventId, Authentication auth) {
        Event event = eventService.loadEntity(eventId);
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .ifPresent(es -> {
                    es.setActive(false);
                    subscriptionRepository.save(es);
                });

        return ResponseEntity.noContent().build();
    }

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

    @Data
    @AllArgsConstructor
    public static class MySubscriptionStatus {
        private boolean linked;
        private boolean subscribed;
    }
}
