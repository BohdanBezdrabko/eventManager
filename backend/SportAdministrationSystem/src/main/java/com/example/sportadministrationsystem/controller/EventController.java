package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.CreatorDto;
import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    /** Список івентів з фільтрами. Підтримує ?createdBy= */
    @GetMapping
    public ResponseEntity<Page<EventDto>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Long createdBy,
            @PageableDefault(size = 20, sort = {"startAt"}) Pageable pageable
    ) {
        if (createdBy != null) {
            return ResponseEntity.ok(eventService.listByAuthor(createdBy, pageable));
        }
        return ResponseEntity.ok(eventService.list(category, tag, pageable));
    }

    /** Альтернативний шлях: /events/by-author/{userId} */
    @GetMapping("/by-author/{userId}")
    public ResponseEntity<Page<EventDto>> listByAuthorPath(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = {"startAt"}) Pageable pageable
    ) {
        return ResponseEntity.ok(eventService.listByAuthor(userId, pageable));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<EventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<EventDto>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.findByName(name));
    }

    @GetMapping("/by-location/{location}")
    public ResponseEntity<List<EventDto>> findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.findByLocation(location));
    }

    /** Автор івента */
    @GetMapping("/{id:\\d+}/creator")
    public ResponseEntity<CreatorDto> getCreator(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getCreator(id));
    }

    @PostMapping
    public ResponseEntity<EventDto> create(@Valid @RequestBody EventPayload payload) {
        EventDto saved = eventService.create(payload);
        return ResponseEntity.created(URI.create("/api/v1/events/" + saved.id())).body(saved);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<EventDto> update(@PathVariable Long id, @Valid @RequestBody EventPayload payload) {
        return ResponseEntity.ok(eventService.update(id, payload));
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
