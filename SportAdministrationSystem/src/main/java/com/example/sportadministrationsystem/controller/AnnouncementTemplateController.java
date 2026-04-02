package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.AnnouncementTemplate;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API для управління AnnouncementTemplates (оголошення для ручної публікації)
 * Ці шаблони використовуються адміністраторами для копіювання текстів у WhatsApp групи
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/events/{eventId}/announcement-templates")
@RequiredArgsConstructor
public class AnnouncementTemplateController {

    private final TemplateService templateService;
    private final EventRepository eventRepository;

    /**
     * GET /api/v1/events/{eventId}/announcement-templates
     * Отримати всі шаблони оголошень для івенту
     */
    @GetMapping
    public ResponseEntity<List<AnnouncementTemplate>> listTemplates(
            @PathVariable Long eventId
    ) {
        List<AnnouncementTemplate> templates = templateService.listAnnouncementTemplates(eventId);
        return ResponseEntity.ok(templates);
    }

    /**
     * GET /api/v1/events/{eventId}/announcement-templates/{id}
     * Отримати один шаблон
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementTemplate> getTemplate(
            @PathVariable Long eventId,
            @PathVariable Long id
    ) {
        return templateService.getAnnouncementTemplate(id)
                .filter(t -> t.getEvent().getId().equals(eventId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/events/{eventId}/announcement-templates/{id}/preview
     * Отримати preview відрендеренного шаблону
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Map<String, Object>> previewTemplate(
            @PathVariable Long eventId,
            @PathVariable Long id
    ) {
        Event event = eventRepository.findById(eventId)
                .orElse(null);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        AnnouncementTemplate template = templateService.getAnnouncementTemplate(id)
                .orElse(null);
        if (template == null || !template.getEvent().getId().equals(eventId)) {
            return ResponseEntity.notFound().build();
        }

        String renderedText = templateService.renderAnnouncementTemplate(
                template,
                event,
                buildEventPageUrl(eventId)
        );

        return ResponseEntity.ok(Map.of(
                "templateTitle", template.getTemplateTitle(),
                "templateBody", template.getTemplateBody(),
                "renderedText", renderedText,
                "channel", template.getChannel().toString()
        ));
    }

    /**
     * POST /api/v1/events/{eventId}/announcement-templates
     * Створити новий шаблон
     */
    @PostMapping
    public ResponseEntity<AnnouncementTemplate> createTemplate(
            @PathVariable Long eventId,
            @RequestBody CreateTemplateRequest req
    ) {
        Event event = eventRepository.findById(eventId)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        AnnouncementTemplate template = templateService.createAnnouncementTemplate(
                event,
                req.getTemplateTitle(),
                req.getTemplateBody(),
                req.getChannel(),
                req.getNotes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    /**
     * PUT /api/v1/events/{eventId}/announcement-templates/{id}
     * Оновити шаблон
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementTemplate> updateTemplate(
            @PathVariable Long eventId,
            @PathVariable Long id,
            @RequestBody CreateTemplateRequest req
    ) {
        AnnouncementTemplate existing = templateService.getAnnouncementTemplate(id)
                .orElse(null);
        if (existing == null || !existing.getEvent().getId().equals(eventId)) {
            return ResponseEntity.notFound().build();
        }

        AnnouncementTemplate updated = templateService.updateAnnouncementTemplate(
                id,
                req.getTemplateTitle(),
                req.getTemplateBody(),
                req.getChannel(),
                req.getNotes()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/v1/events/{eventId}/announcement-templates/{id}
     * Видалити шаблон
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long eventId,
            @PathVariable Long id
    ) {
        AnnouncementTemplate existing = templateService.getAnnouncementTemplate(id)
                .orElse(null);
        if (existing == null || !existing.getEvent().getId().equals(eventId)) {
            return ResponseEntity.notFound().build();
        }

        templateService.deleteAnnouncementTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Побудувати URL сторінки івенту для фронтенду
     */
    private String buildEventPageUrl(Long eventId) {
        // Замініть на реальну URL вашого фронтенду
        return "https://app.example.com/events/" + eventId;
    }

    /**
     * DTO для створення/редагування шаблону
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    public static class CreateTemplateRequest {
        private String templateTitle;
        private String templateBody;
        private Channel channel;
        private String notes;
    }
}
