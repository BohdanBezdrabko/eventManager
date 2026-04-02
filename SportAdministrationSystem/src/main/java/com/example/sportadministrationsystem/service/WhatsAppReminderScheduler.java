package com.example.sportadministrationsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Планувальник для відправки WhatsApp нагадувань про предстоящі события.
 * Запускає перевірку нагадувань кожні 5 хвилин.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppReminderScheduler {

    private final WhatsAppReminderService reminderService;

    /**
     * Запускається кожні 5 хвилин (300,000 мс).
     * Відправляє всі відповідні нагадування.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    @Async
    public void checkAndSendReminders() {
        try {
            log.debug("Starting WhatsApp reminder scheduler tick");

            // Відправляємо 72-годинні нагадування
            reminderService.send72HourReminders();

            // Відправляємо 24-годинні нагадування
            reminderService.send24HourReminders();

            log.debug("WhatsApp reminder scheduler tick completed");
        } catch (Exception e) {
            log.error("Error in WhatsApp reminder scheduler: {}", e.getMessage(), e);
        }
    }
}
