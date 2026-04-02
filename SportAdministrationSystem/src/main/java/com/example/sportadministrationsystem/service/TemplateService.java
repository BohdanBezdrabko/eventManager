package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.AnnouncementTemplateRepository;
import com.example.sportadministrationsystem.repository.PostTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ОСНОВНИЙ сервіс для управління та рендеринга шаблонів.
 * Забезпечує єдину точку входу для всіх операцій з шаблонами.
 *
 * ЛОГІКА:
 * 1. AnnouncementTemplate — для ручної публікації в групах (адміни копіюють текст)
 * 2. PostTemplate — для автоматичної генерації постів при создании івенту
 *
 * Шаблони мають плейсхолдери:
 * - {{event_name}} — назва івенту
 * - {{event_date}} — дата (dd.MM.yyyy)
 * - {{event_time}} — час (HH:mm)
 * - {{event_datetime}} — дата й час (dd.MM.yyyy HH:mm)
 * - {{location}} — місцезнаходження
 * - {{description}} — опис
 * - {{capacity}} — вмістимість
 * - {{category}} — категорія
 * - {{wa_link}} — wa.me лінк для підписки
 * - {{event_page_url}} — URL сторінки івенту
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final AnnouncementTemplateRepository announcementTemplateRepo;
    private final PostTemplateRepository postTemplateRepo;
    private final WhatsAppInviteService whatsAppInviteService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // ============ ANNOUNCEMENT TEMPLATES ============

    /**
     * Список всіх AnnouncementTemplate для івенту
     */
    public List<AnnouncementTemplate> listAnnouncementTemplates(Long eventId) {
        return announcementTemplateRepo.findByEvent_IdOrderByCreatedAtDesc(eventId);
    }

    /**
     * Отримати один AnnouncementTemplate
     */
    public Optional<AnnouncementTemplate> getAnnouncementTemplate(Long id) {
        return announcementTemplateRepo.findById(id);
    }

    /**
     * Створити новий AnnouncementTemplate
     */
    public AnnouncementTemplate createAnnouncementTemplate(
            Event event,
            String templateTitle,
            String templateBody,
            Channel channel,
            String notes
    ) {
        AnnouncementTemplate template = AnnouncementTemplate.builder()
                .event(event)
                .templateTitle(templateTitle)
                .templateBody(templateBody)
                .channel(channel != null ? channel : Channel.WHATSAPP)
                .notes(notes)
                .enabled(true)
                .build();

        AnnouncementTemplate saved = announcementTemplateRepo.save(template);
        log.info("Created AnnouncementTemplate id={} for event={}", saved.getId(), event.getId());
        return saved;
    }

    /**
     * Оновити AnnouncementTemplate
     */
    public AnnouncementTemplate updateAnnouncementTemplate(
            Long id,
            String templateTitle,
            String templateBody,
            Channel channel,
            String notes
    ) {
        Optional<AnnouncementTemplate> opt = announcementTemplateRepo.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("AnnouncementTemplate not found: " + id);
        }

        AnnouncementTemplate template = opt.get();
        template.setTemplateTitle(templateTitle);
        template.setTemplateBody(templateBody);
        if (channel != null) {
            template.setChannel(channel);
        }
        template.setNotes(notes);

        AnnouncementTemplate saved = announcementTemplateRepo.save(template);
        log.info("Updated AnnouncementTemplate id={}", saved.getId());
        return saved;
    }

    /**
     * Видалити AnnouncementTemplate
     */
    public void deleteAnnouncementTemplate(Long id) {
        announcementTemplateRepo.deleteById(id);
        log.info("Deleted AnnouncementTemplate id={}", id);
    }

    /**
     * Рендерити AnnouncementTemplate з даними івенту
     * Замінює всі плейсхолдери на реальні значення
     */
    public String renderAnnouncementTemplate(AnnouncementTemplate template, Event event, String eventPageUrl) {
        if (template == null || template.getTemplateBody() == null) {
            return "";
        }

        Map<String, String> placeholders = buildPlaceholders(event, eventPageUrl);
        String result = template.getTemplateBody();

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(entry.getKey(), value);
        }

        return result;
    }

    // ============ POST TEMPLATES ============

    /**
     * Список всіх PostTemplate для каналу та аудиторії
     */
    public List<PostTemplate> listPostTemplates(Channel channel, Audience audience) {
        return postTemplateRepo.findByChannelAndAudienceAndActive(channel, audience, true);
    }

    /**
     * Список PostTemplate за кодом
     */
    public Optional<PostTemplate> getPostTemplateByCode(String code) {
        return postTemplateRepo.findByCodeAndActive(code, true);
    }

    /**
     * Рендерити PostTemplate для івенту
     * Генерує title та body для нового поста
     */
    public Map<String, String> renderPostTemplate(PostTemplate template, Event event, String eventPageUrl) {
        if (template == null) {
            return Map.of("title", "", "body", "");
        }

        Map<String, String> placeholders = buildPlaceholders(event, eventPageUrl);

        String title = template.getTitleTpl() != null ? template.getTitleTpl() : "";
        String body = template.getBodyTpl() != null ? template.getBodyTpl() : "";

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            title = title.replace(entry.getKey(), value);
            body = body.replace(entry.getKey(), value);
        }

        return Map.of("title", title, "body", body);
    }

    // ============ HELPER METHODS ============

    /**
     * Побудувати маппінг всіх доступних плейсхолдерів на їхні значення
     */
    private Map<String, String> buildPlaceholders(Event event, String eventPageUrl) {
        Map<String, String> placeholders = new HashMap<>();

        // Основні дані івенту
        placeholders.put("{{event_name}}", event.getName() != null ? event.getName() : "");
        placeholders.put("{{location}}", event.getLocation() != null ? event.getLocation() : "");
        placeholders.put("{{description}}", event.getDescription() != null ? event.getDescription() : "");
        placeholders.put("{{capacity}}", event.getCapacity() != null ? event.getCapacity().toString() : "");
        placeholders.put("{{category}}", event.getCategory() != null ? event.getCategory().toString() : "");

        // Дата та час
        if (event.getStartAt() != null) {
            placeholders.put("{{event_date}}", formatDate(event.getStartAt()));
            placeholders.put("{{event_time}}", formatTime(event.getStartAt()));
            placeholders.put("{{event_datetime}}", formatDateTime(event.getStartAt()));
        } else {
            placeholders.put("{{event_date}}", "");
            placeholders.put("{{event_time}}", "");
            placeholders.put("{{event_datetime}}", "");
        }

        // WhatsApp лінк
        placeholders.put("{{wa_link}}", generateWhatsAppLink(event.getId()));

        // URL сторінки івенту
        placeholders.put("{{event_page_url}}", eventPageUrl != null ? eventPageUrl : "");

        return placeholders;
    }

    /**
     * Генерувати wa.me лінк
     */
    private String generateWhatsAppLink(Long eventId) {
        try {
            return whatsAppInviteService.buildWaMeLink(eventId);
        } catch (Exception e) {
            log.warn("Failed to generate WhatsApp link for event {}: {}", eventId, e.getMessage());
            return "";
        }
    }

    /**
     * Форматувати дату (dd.MM.yyyy)
     */
    private String formatDate(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(DATE_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format date: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Форматувати час (HH:mm)
     */
    private String formatTime(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(TIME_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(TIME_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format time: {}", e.getMessage());
        }
        return "";
    }

    /**
     * Форматувати дату й час разом (dd.MM.yyyy HH:mm)
     */
    private String formatDateTime(Object dateTime) {
        try {
            if (dateTime instanceof LocalDateTime) {
                return ((LocalDateTime) dateTime).format(DATE_TIME_FORMATTER);
            } else if (dateTime instanceof java.time.ZonedDateTime) {
                return ((java.time.ZonedDateTime) dateTime).format(DATE_TIME_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Failed to format datetime: {}", e.getMessage());
        }
        return "";
    }
}
