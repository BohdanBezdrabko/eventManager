package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.CreatorDto;
import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /* ===== List / Search ===== */

    /** Загальний лістинг для сторінки «Івенти» (усі івенти). */
    @GetMapping
    public ResponseEntity<Page<EventDto>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "startAt,asc") String sort,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "tag", required = false) String tag
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "startAt"));
        return ResponseEntity.ok(eventService.list(category, tag, pageable));
    }

    /** Дашборд: івенти конкретного автора (userId з шляху). */
    @GetMapping("/by-author/{userId}")
    public ResponseEntity<Page<EventDto>> listByAuthorPath(
            @PathVariable Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "startAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort, "startAt"));
        return ResponseEntity.ok(eventService.listByAuthor(userId, pageable));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<EventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<EventDto>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.findByName(name));
    }

    @GetMapping("/by-location/{location}")
    public ResponseEntity<List<EventDto>> findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.findByLocation(location));
    }

    @GetMapping("/{id:\\d+}/creator")
    public ResponseEntity<CreatorDto> getCreator(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getCreator(id));
    }

    /* ===== CRUD ===== */

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

    /* ===== Helpers ===== */

    private Sort parseSort(String sort, String defaultField) {
        try {
            if (sort == null || sort.isBlank()) {
                return Sort.by(Sort.Direction.ASC, defaultField);
            }
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction dir = (parts.length > 1)
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;
            return Sort.by(dir, field);
        } catch (Exception e) {
            return Sort.by(Sort.Direction.ASC, defaultField);
        }
    }
}
