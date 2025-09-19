package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.EventCreateDto;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> findById(@PathVariable int id) { // Long
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<Event>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.getEventsByName(name)); // зроби List у сервісі/репо
    }

    @GetMapping("/by-location/{location}")
    public ResponseEntity<List<Event>> findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.getEventsByLocation(location)); // List
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event event) {
        if (!id.equals(event.getId())) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(eventService.updateEvent(event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable int id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // не 'ROLE_ADMIN'
    public ResponseEntity<Event> create(@Valid @RequestBody EventCreateDto dto) {
        Event saved = eventService.create(dto);
        return ResponseEntity.created(URI.create("/api/v1/events/" + saved.getId())).body(saved);
    }
}
