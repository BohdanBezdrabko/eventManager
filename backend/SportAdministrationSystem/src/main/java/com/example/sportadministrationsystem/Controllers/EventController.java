package com.example.sportadministrationsystem.Controllers;

import com.example.sportadministrationsystem.Models.Event;
import com.example.sportadministrationsystem.Services.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/event")
public class EventController {
    private final EventService eventService;

    @GetMapping("/id/{id}")
    public ResponseEntity findById(@PathVariable int id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
    @GetMapping("/name/{name}")
    public ResponseEntity findByName(@PathVariable String name) {
        return ResponseEntity.ok(eventService.getEventByName(name));
    }
    @GetMapping("/location/{location}")
    public ResponseEntity findByLocation(@PathVariable String location) {
        return ResponseEntity.ok(eventService.getEventByLocation(location));
    }
    @PostMapping
    public ResponseEntity<Event> addEvent(@RequestBody Event event) {
        Event savedEvent = eventService.addEvent(event);
        return ResponseEntity.ok(savedEvent);
    }

    // Оновити подію
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable int id, @RequestBody Event event) {
        // Переконаємось, що id з URL і id з тіла збігаються
        if (id != event.getId()) {
            return ResponseEntity.badRequest().build();
        }
        Event updatedEvent = eventService.updateEvent(event);
        return ResponseEntity.ok(updatedEvent);
    }

    // Видалити подію
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable int id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/all")
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }
}
