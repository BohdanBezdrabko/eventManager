package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<EventDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<EventDto>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.getEventsByName(name));
    }

    @GetMapping("/by-location/{location}")
    public ResponseEntity<List<EventDto>> findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.getEventsByLocation(location));
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
