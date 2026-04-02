package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.service.WhatsAppInviteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppInviteService inviteService;
    private final EventRepository eventRepository;

    /**
     * GET /api/v1/whatsapp/events/{eventId}/invite
     * Генерує invite текст та wa.me посилання для івенту
     * @param eventId ID івенту
     * @param short_version якщо true - коротка версія для announcement group
     */
    @GetMapping("/events/{eventId}/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEventInvite(
            @PathVariable long eventId,
            @RequestParam(name = "short", required = false, defaultValue = "false") boolean short_version
    ) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }

        String inviteText = inviteService.buildInviteMessage(event, short_version);
        String waLink = inviteService.buildWaMeLink(eventId);

        Map<String, Object> response = Map.of(
                "eventId", eventId,
                "eventName", event.getName(),
                "text", inviteText,
                "waLink", waLink,
                "shortVersion", short_version
        );


        return ResponseEntity.ok(response);
    }
}
