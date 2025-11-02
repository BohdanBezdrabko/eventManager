package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.PostDto;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventRepository;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Повертає шаблон нового поста для форми створення на фронті.
 * Шлях співпадає з тим, що викликає фронт: GET /api/v1/events/{eventId}/posts/new
 *
 * НІЧОГО не створює в БД — лише віддає DTO з дефолтами.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/{eventId:\\d+}")
public class PostTemplateController {

    private final EventRepository eventRepository;

    // Може бути заданий у application.yml: telegram.bot.chat-id: "..."
    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    @GetMapping("/posts/new")
    public ResponseEntity<PostDto> getNewPostTemplate(@PathVariable @Min(1) Long eventId) {
        Event ev = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        LocalDateTime publishAt = computeDefaultPublishAt(ev);

        // Формуємо "порожній" драфт під форму
        PostDto dto = new PostDto(
                null,                          // id (новий)
                ev.getId(),                    // eventId
                "",                            // title
                "",                            // body
                publishAt,                     // publishAt (див. нижче)
                PostStatus.DRAFT.name(),       // status
                Audience.PUBLIC.name(),        // audience
                Channel.TELEGRAM.name(),       // channel
                null,                          // externalId
                null,                          // error
                false,                         // generated
                null,                          // createdAt
                null,                          // updatedAt
                (defaultChatId != null && !defaultChatId.isBlank()) ? defaultChatId : null // telegramChatId
        );

        return ResponseEntity.ok(dto);
    }

    private static LocalDateTime computeDefaultPublishAt(Event ev) {
        // Якщо у події є час початку — публікувати за годину до неї,
        // інакше через годину від "зараз" (зручно як дефолт для чернетки).
        LocalDateTime start = ev.getStartAt();
        if (start != null) {
            LocalDateTime candidate = start.minusHours(1);
            // Не віддавати минулий час — зрушимо вперед на 10 хв від поточного
            if (candidate.isBefore(LocalDateTime.now())) {
                return LocalDateTime.now().plusMinutes(10);
            }
            return candidate;
        }
        return LocalDateTime.now().plusHours(1);
    }
}
