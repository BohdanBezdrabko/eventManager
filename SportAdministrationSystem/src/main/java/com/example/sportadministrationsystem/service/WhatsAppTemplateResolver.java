package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.NotificationType;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import com.example.sportadministrationsystem.model.WhatsAppTemplate;
import com.example.sportadministrationsystem.repository.WhatsAppTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс для резолвування WhatsApp шаблонів та підготовки параметрів для відправки
 * Керує маппінгом між типами сповіщень та Meta-approved шаблонами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppTemplateResolver {

    private final WhatsAppTemplateRepository templateRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Розрезолвити шаблон за типом сповіщення
     * @param notificationType тип сповіщення
     * @return Optional з шаблоном або пусто, якщо не знайдено
     */
    public Optional<WhatsAppTemplate> resolveTemplate(NotificationType notificationType) {
        return templateRepository.findByNotificationTypeAndEnabledTrue(notificationType);
    }

    /**
     * Підготувати параметри для шаблону SUBSCRIPTION_CONFIRM
     * Параметри: {{1}} = eventName, {{2}} = eventDateTime
     */
    public List<String> getSubscriptionConfirmParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        params.add(formatDateTime(event.getStartAt()));
        return params;
    }

    /**
     * Підготувати параметри для шаблону REGISTRATION_CONFIRM
     * Параметри: {{1}} = eventName, {{2}} = eventDateTime
     */
    public List<String> getRegistrationConfirmParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        params.add(formatDateTime(event.getStartAt()));
        return params;
    }

    /**
     * Підготувати параметри для шаблону REMINDER_24H
     * Параметри: {{1}} = eventName, {{2}} = eventDateTime
     */
    public List<String> getReminder24hParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        params.add(formatDateTime(event.getStartAt()));
        return params;
    }

    /**
     * Підготувати параметри для шаблону REMINDER_72H
     * Параметри: {{1}} = eventName, {{2}} = eventDateTime
     */
    public List<String> getReminder72hParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        params.add(formatDateTime(event.getStartAt()));
        return params;
    }

    /**
     * Підготувати параметри для шаблону EVENT_UPDATED
     * Параметри: {{1}} = eventName, {{2}} = eventDateTime
     */
    public List<String> getEventUpdatedParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        params.add(formatDateTime(event.getStartAt()));
        return params;
    }

    /**
     * Підготувати параметри для шаблону EVENT_CANCELLED
     * Параметри: {{1}} = eventName
     */
    public List<String> getEventCancelledParams(Event event) {
        List<String> params = new ArrayList<>();
        params.add(event.getName() != null ? event.getName() : "Івент");
        return params;
    }

    /**
     * Отримати параметри за типом сповіщення
     */
    public List<String> getParams(NotificationType notificationType, Event event) {
        return switch (notificationType) {
            case SUBSCRIPTION_CONFIRM -> getSubscriptionConfirmParams(event);
            case REGISTRATION_CONFIRM -> getRegistrationConfirmParams(event);
            case REMINDER_24H -> getReminder24hParams(event);
            case REMINDER_72H -> getReminder72hParams(event);
            case EVENT_UPDATED -> getEventUpdatedParams(event);
            case EVENT_CANCELLED -> getEventCancelledParams(event);
        };
    }

    /**
     * Форматувати дату/час для сповіщення
     */
    private String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "Дата невідома";
        }

        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(DATE_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(DATE_FORMATTER);
            } else if (dateTime instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTime).toLocalDateTime().format(DATE_FORMATTER);
            } else if (dateTime instanceof java.util.Date) {
                return java.time.format.DateTimeFormatter
                        .ofPattern("dd.MM.yyyy HH:mm")
                        .format(java.time.Instant.ofEpochMilli(((java.util.Date) dateTime).getTime())
                                .atZone(java.time.ZoneId.systemDefault()));
            }
        } catch (Exception e) {
            log.warn("Failed to format date: {}", e.getMessage());
        }

        return dateTime.toString();
    }
}
