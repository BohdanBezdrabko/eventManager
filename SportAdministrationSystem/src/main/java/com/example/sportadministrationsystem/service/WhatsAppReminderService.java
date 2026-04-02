package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscriptionWhatsapp;
import com.example.sportadministrationsystem.model.NotificationType;
import com.example.sportadministrationsystem.model.WhatsAppTemplate;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppReminderService {

    private final EventSubscriptionWhatsappRepository subscriptionRepository;
    private final WhatsAppGraphClient graphClient;
    private final WhatsAppTemplateResolver templateResolver;

    /**
     * Надсилає 72-годинні нагадування про предстоящі события.
     * Шукає события у діапазоні: now + 71h50m до now + 72h10m
     * Надсилає нагадування тільки активним підписникам, які ще не отримали це нагадування.
     */
    @Transactional
    public void send72HourReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            // 72 години = 259200 секунд
            LocalDateTime minTime = now.plusSeconds(259200 - 600);  // -10 хвилин
            LocalDateTime maxTime = now.plusSeconds(259200 + 600);  // +10 хвилин

            log.info("Searching for 72h reminders between {} and {}", minTime, maxTime);

            List<EventSubscriptionWhatsapp> subscriptions = subscriptionRepository.findForReminder72h(minTime, maxTime);
            log.info("Found {} subscriptions for 72h reminders", subscriptions.size());

            int successful = 0;
            int failed = 0;

            for (EventSubscriptionWhatsapp sub : subscriptions) {
                try {
                    // ВАЖЛИВО: Безпечно отримуємо waId у контексті завантаженої підписки
                    String waId = getWaIdSafely(sub);
                    if (waId == null) {
                        log.warn("Skip reminder: waId is null for subscription {}", sub.getId());
                        failed++;
                        continue;
                    }

                    sendReminder72h(sub, waId);
                    sub.setReminder72hSent(true);
                    subscriptionRepository.save(sub);
                    successful++;
                    log.debug("72h reminder sent to {} for event {}", waId, sub.getEvent().getId());
                } catch (Exception e) {
                    failed++;
                    log.warn("Failed to send 72h reminder for subscription {}: {}", sub.getId(), e.getMessage());
                }
            }

            log.info("72h reminders: {} successful, {} failed", successful, failed);
        } catch (Exception e) {
            log.error("Error in send72HourReminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Надсилає 24-годинні нагадування про предстоящі события.
     * Шукає события у діапазоні: now + 23h50m до now + 24h10m
     * Надсилає нагадування тільки активним підписникам, які ще не отримали це нагадування.
     */
    @Transactional
    public void send24HourReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            // 24 години = 86400 секунд
            LocalDateTime minTime = now.plusSeconds(86400 - 600);   // -10 хвилин
            LocalDateTime maxTime = now.plusSeconds(86400 + 600);   // +10 хвилин

            log.info("Searching for 24h reminders between {} and {}", minTime, maxTime);

            List<EventSubscriptionWhatsapp> subscriptions = subscriptionRepository.findForReminder24h(minTime, maxTime);
            log.info("Found {} subscriptions for 24h reminders", subscriptions.size());

            int successful = 0;
            int failed = 0;

            for (EventSubscriptionWhatsapp sub : subscriptions) {
                try {
                    // ВАЖЛИВО: Безпечно отримуємо waId у контексті завантаженої підписки
                    String waId = getWaIdSafely(sub);
                    if (waId == null) {
                        log.warn("Skip reminder: waId is null for subscription {}", sub.getId());
                        failed++;
                        continue;
                    }

                    sendReminder24h(sub, waId);
                    sub.setReminder24hSent(true);
                    subscriptionRepository.save(sub);
                    successful++;
                    log.debug("24h reminder sent to {} for event {}", waId, sub.getEvent().getId());
                } catch (Exception e) {
                    failed++;
                    log.warn("Failed to send 24h reminder for subscription {}: {}", sub.getId(), e.getMessage());
                }
            }

            log.info("24h reminders: {} successful, {} failed", successful, failed);
        } catch (Exception e) {
            log.error("Error in send24HourReminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Скидає прапорці нагадування для всіх підписок на заданий івент.
     * Викликається, коли час початку івенту змінюється.
     */
    @Transactional
    public void resetRemindersForEvent(Long eventId) {
        try {
            int updated = subscriptionRepository.resetRemindersForEvent(eventId);
            log.info("Reset reminders for event {}: {} subscriptions updated", eventId, updated);
        } catch (Exception e) {
            log.error("Error resetting reminders for event {}: {}", eventId, e.getMessage(), e);
        }
    }

    // ---------- Private helper methods ----------

    /**
     * Надсилає 72-годинне нагадування користувачу через template message
     * Спроба використати шаблон, fallback на простий текст
     */
    private void sendReminder72h(EventSubscriptionWhatsapp subscription, String waId) {
        Event event = subscription.getEvent();

        // Спробуємо отримати шаблон
        Optional<WhatsAppTemplate> template = templateResolver.resolveTemplate(NotificationType.REMINDER_72H);

        if (template.isPresent()) {
            try {
                // Розрезолвимо параметри для шаблону
                var params = templateResolver.getReminder72hParams(event);
                graphClient.sendTemplate(waId, template.get().getTemplateName(), params);
                log.debug("Sent 72h reminder via template to {}", waId);
                return;
            } catch (Exception e) {
                log.warn("Failed to send 72h reminder via template: {}, falling back to plain text", e.getMessage());
            }
        }

        // Fallback: простий текст, якщо шаблон недоступний
        sendReminder72hFallback(event, waId);
    }

    /**
     * Надсилає 24-годинне нагадування користувачу через template message
     * Спроба використати шаблон, fallback на простий текст
     */
    private void sendReminder24h(EventSubscriptionWhatsapp subscription, String waId) {
        Event event = subscription.getEvent();

        // Спробуємо отримати шаблон
        Optional<WhatsAppTemplate> template = templateResolver.resolveTemplate(NotificationType.REMINDER_24H);

        if (template.isPresent()) {
            try {
                // Розрезолвимо параметри для шаблону
                var params = templateResolver.getReminder24hParams(event);
                graphClient.sendTemplate(waId, template.get().getTemplateName(), params);
                log.debug("Sent 24h reminder via template to {}", waId);
                return;
            } catch (Exception e) {
                log.warn("Failed to send 24h reminder via template: {}, falling back to plain text", e.getMessage());
            }
        }

        // Fallback: простий текст, якщо шаблон недоступний
        sendReminder24hFallback(event, waId);
    }

    /**
     * Fallback: надсилає 72-годинне нагадування звичайним текстом
     */
    private void sendReminder72hFallback(Event event, String waId) {
        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();
        String eventTime = formatEventTime(event.getStartAt());

        String message = String.format(
            "⏰ *Нагадування:* Подія '*%s*' почнеться через *3 дні*\n" +
            "🕐 Час: *%s*\n" +
            "📍 Місце: %s\n\n" +
            "Будьте готові! 💪",
            eventName,
            eventTime,
            event.getLocation() != null ? event.getLocation() : "уточніть на сайті"
        );

        graphClient.sendText(waId, message);
    }

    /**
     * Fallback: надсилає 24-годинне нагадування звичайним текстом
     */
    private void sendReminder24hFallback(Event event, String waId) {
        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();
        String eventTime = formatEventTime(event.getStartAt());

        String message = String.format(
            "🔔 *Важливо:* Подія '*%s*' почнеться *ЗАВТРА*\n" +
            "🕐 Час: *%s*\n" +
            "📍 Місце: %s\n\n" +
            "Не забудьте прийти! ⏰",
            eventName,
            eventTime,
            event.getLocation() != null ? event.getLocation() : "уточніть на сайті"
        );

        graphClient.sendText(waId, message);
    }

    /**
     * Безпечно отримує waId з підписки.
     * Захищає від LazyInitializationException в асинхронному контексті.
     */
    private String getWaIdSafely(EventSubscriptionWhatsapp subscription) {
        try {
            if (subscription == null || subscription.getUserWhatsapp() == null) {
                return null;
            }
            return subscription.getUserWhatsapp().getWaId();
        } catch (Exception e) {
            log.error("Failed to extract waId from subscription {}: {}", subscription.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Форматувати дату/час для сповіщення
     */
    private String formatEventTime(Object dateTime) {
        if (dateTime == null) {
            return "невідомий час";
        }
        try {
            if (dateTime instanceof java.time.LocalDateTime) {
                return ((java.time.LocalDateTime) dateTime)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            } else if (dateTime instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTime).toLocalDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            }
        } catch (Exception e) {
            log.warn("Failed to format date: {}", e.getMessage());
        }
        return dateTime.toString();
    }
}

