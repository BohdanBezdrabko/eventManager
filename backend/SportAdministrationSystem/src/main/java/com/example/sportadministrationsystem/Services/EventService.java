package com.example.sportadministrationsystem.Services;

import com.example.sportadministrationsystem.Models.Event;
import com.example.sportadministrationsystem.Repositories.EventRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
public class EventService {
    @Autowired
    private final EventRepository eventRepository;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(int id) {
        Optional<Event> event = eventRepository.findById((long) id);
        if (event.isPresent()) {
            return event.get();
        } else {
            throw new RuntimeException("Event not found with id: " + id);
        }
    }

    public Event addEvent(Event event) {
        // Перевірка, чи вже існує подія з таким же ім'ям
        Optional<Event> existingEvent = eventRepository.findByName(event.getName());
        if (existingEvent.isPresent()) {
            throw new RuntimeException("Event with the same name already exists");
        }
        return eventRepository.save(event);
    }

    public Event updateEvent(Event event) {
        // Перевірка на наявність події для оновлення
        if (!eventRepository.existsById(event.getId())) {
            throw new RuntimeException("Event not found with id: " + event.getId());
        }
        return eventRepository.save(event);
    }

    public void deleteEvent(int id) {
        // Перевірка на існування події перед видаленням
        if (!eventRepository.existsById((long) id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById((long) id);
    }

    public Optional<Event> getEventByName(String name) {
        Optional<Event> event = eventRepository.findByName(name);
        if (event.isEmpty()) {
            throw new RuntimeException("Event not found with name: " + name);
        }
        return event;
    }

    public Optional<Event> getEventByLocation(String location) {
        Optional<Event> event = eventRepository.findByLocation(location);
        if (event.isEmpty()) {
            throw new RuntimeException("Event not found at location: " + location);
        }
        return event;
    }
}
