package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.AnnouncementTemplate;
import com.example.sportadministrationsystem.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервіс для рендерингу AnnouncementTemplate з реальними даними івенту
 * Замінює плейсхолдери на реальні значення
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementTemplateRenderer {

    private final WhatsAppInviteService whatsAppInviteService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Рендерити шаблон оголошення з даними івенту
     * Замінює плейсхолдери:
     * - {{event_name}} → назва івенту
     * - {{event_date}} → дата у форматі dd.MM.yyyy
     * - {{event_time}} → час у форматі HH:mm
     * - {{location}} → місцезнаходження
     * - {{description}} → опис івенту
     * - {{wa_link}} → wa.me лінк для підписки
     * - {{event_page_url}} → URL сторінки івенту на сайті
     *
     * @param template шаблон оголошення
     * @param event дані івенту
     * @param eventPageUrl URL сторінки івенту на фронтенді (опційно)
     * @return відрендерений текст
     */
    public String render(AnnouncementTemplate template, Event event, String eventPageUrl) {
        if (template == null || template.getTemplateBody() == null) {
            return "";
        }

        Map<String, String> placeholders = buildPlaceholders(event, eventPageUrl);
        String result = template.getTemplateBody();

        // Замінити всі плейсхолдери
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }

        return result;
    }

    /**
     * Побудувати маппінг плейсхолдерів на їх значення
     */
    private Map<String, String> buildPlaceholders(Event event, String eventPageUrl) {
        Map<String, String> placeholders = new HashMap<>();

        // Основні дані івенту
        placeholders.put("{{event_name}}", event.getName() != null ? event.getName() : "");
        placeholders.put("{{location}}", event.getLocation() != null ? event.getLocation() : "");
        placeholders.put("{{description}}", event.getDescription() != null ? event.getDescription() : "");

        // Дата та час
        if (event.getStartAt() != null) {
            String eventDate = formatDate(event.getStartAt());
            String eventTime = formatTime(event.getStartAt());
            String eventDateTime = formatDateTime(event.getStartAt());

            placeholders.put("{{event_date}}", eventDate);
            placeholders.put("{{event_time}}", eventTime);
            placeholders.put("{{event_datetime}}", eventDateTime);
        } else {
            placeholders.put("{{event_date}}", "");
            placeholders.put("{{event_time}}", "");
            placeholders.put("{{event_datetime}}", "");
        }

        // WhatsApp лінк для підписки
        String waLink = generateWhatsAppLink(event);
        placeholders.put("{{wa_link}}", waLink);

        // URL сторінки івенту
        String pageUrl = eventPageUrl != null ? eventPageUrl : "";
        placeholders.put("{{event_page_url}}", pageUrl);

        return placeholders;
    }

    /**
     * Згенерувати wa.me лінк для івенту
     */
    private String generateWhatsAppLink(Event event) {
        try {
            return whatsAppInviteService.buildWaMeLink(event.getId());
        } catch (Exception e) {
            log.warn("Failed to generate WhatsApp invite link: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Форматувати дату
     */
    private String formatDate(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(DATE_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(DATE_FORMATTER);
            } else if (dateTime instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTime).toLocalDateTime().format(DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format date: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Форматувати час
     */
    private String formatTime(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(TIME_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(TIME_FORMATTER);
            } else if (dateTime instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTime).toLocalDateTime().format(TIME_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format time: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Форматувати дату та час разом
     */
    private String formatDateTime(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(DATE_TIME_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(DATE_TIME_FORMATTER);
            } else if (dateTime instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateTime).toLocalDateTime().format(DATE_TIME_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format date-time: {}", e.getMessage());
        }
        return "";
    }
}
