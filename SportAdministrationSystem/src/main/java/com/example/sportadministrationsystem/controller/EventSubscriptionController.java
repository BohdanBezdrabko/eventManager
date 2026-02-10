package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
import com.example.sportadministrationsystem.service.EventService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/subscription")
@RequiredArgsConstructor
public class EventSubscriptionController {

    private final EventService eventService;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventSubscriptionWhatsappRepository eventSubscriptionWhatsappRepository;

    /**
     * Статус мого зв’язку/підписки (ваш існуючий ендпоінт, за потреби доробите логіку всередині).
     */
    @GetMapping("/my-status")
    public ResponseEntity<MySubscriptionStatus> myStatus(@PathVariable Long eventId,
                                                         Authentication auth) {
        // Перевірка існування івента (кине 404, якщо не знайдено)
        eventService.getById(eventId);
        // TODO: Повернути реальний статус користувача, коли додасте логіку
        return ResponseEntity.ok(new MySubscriptionStatus(false, false));
    }

    /**
     * Лише кількість активних Telegram-підписників на івент.
     * Рахуємо тільки тих, у кого є tg_chat_id (тобто реально зв'язані в Telegram),
     * і лише active=true.
     *
     * @param eventId ID івента
     * @return 200 OK з TelegramCountResponse, або 404 якщо івент не знайдено
     */
    @GetMapping("/telegram/count")
    public ResponseEntity<TelegramCountResponse> countTelegram(@PathVariable Long eventId) {
        // Перевірка існування івента (кине 404 автоматично)
        eventService.getById(eventId);

        long count = eventSubscriptionRepository.countActiveTelegram(eventId);
        return ResponseEntity.ok(new TelegramCountResponse(
                eventId, Messenger.TELEGRAM.name(), true, count
        ));
    }

    /**
     * Лише кількість активних WhatsApp-підписників на івент.
     * Рахуємо тільки тих, у кого є wa_id (тобто реально зв'язані у WhatsApp),
     * і лише active=true.
     *
     * @param eventId ID івента
     * @return 200 OK з WhatsAppCountResponse, або 404 якщо івент не знайдено
     */
    @GetMapping("/whatsapp/count")
    public ResponseEntity<WhatsAppCountResponse> countWhatsApp(@PathVariable Long eventId) {
        // Перевірка існування івента (кине 404 автоматично)
        eventService.getById(eventId);

        long count = eventSubscriptionWhatsappRepository.countActiveWhatsApp(eventId);
        return ResponseEntity.ok(new WhatsAppCountResponse(
                eventId, "WHATSAPP", true, count
        ));
    }

    @Data
    @AllArgsConstructor
    public static class MySubscriptionStatus {
        private boolean linked;
        private boolean subscribed;
    }

    @Data
    @AllArgsConstructor
    public static class TelegramCountResponse {
        private Long eventId;
        private String messenger; // "TELEGRAM"
        private boolean active;   // завжди true для цього ендпоінта
        private long count;       // кількість унікальних підписників з tg_chat_id
    }

    @Data
    @AllArgsConstructor
    public static class WhatsAppCountResponse {
        private Long eventId;
        private String messenger; // "WHATSAPP"
        private boolean active;   // завжди true для цього ендпоінта
        private long count;       // кількість унікальних підписників з wa_id
    }
}
