package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    private final EventService eventService;

    @GetMapping("/{id}")
    public ResponseEntity<Event> findById(@PathVariable int id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<Optional<Event>> findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.getEventByName(name));
    }

    @GetMapping("/by-location/{location}")
    public ResponseEntity<Optional<Event>> findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.getEventByLocation(location));
    }

    @PostMapping
    public ResponseEntity<Event> addEvent(@RequestBody Event event) {
        Event savedEvent = eventService.addEvent(event);
        return ResponseEntity.ok(savedEvent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable int id, @RequestBody Event event) {
        if (id != event.getId()) {
            return ResponseEntity.badRequest().build();
        }
        Event updatedEvent = eventService.updateEvent(event);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable int id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
}
