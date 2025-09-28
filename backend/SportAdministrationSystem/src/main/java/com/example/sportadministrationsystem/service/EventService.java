package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.repository.EventRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EventDto getEventById(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        return toDto(e);
    }

    public EventDto create(@Valid EventPayload p) {
        eventRepository.findByName(p.getName()).ifPresent(x -> {
            throw new IllegalArgumentException("Event with the same name already exists");
        });
        Event e = new Event();
        apply(p, e);
        return toDto(eventRepository.save(e));
    }

    public EventDto update(Long id, @Valid EventPayload p) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        apply(p, e);
        return toDto(eventRepository.save(e));
    }

    public void delete(Long id) {
        if (!eventRepository.existsById(id)) throw new NotFoundException("Event not found: " + id);
        eventRepository.deleteById(id);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getEventsByLocation(String location) {
        return eventRepository.findAllByLocationIgnoreCase(location).stream().map(this::toDto).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getEventsByName(String name) {
        return eventRepository.findAllByNameIgnoreCase(name).stream().map(this::toDto).toList();
    }

    private void apply(EventPayload p, Event e) {
        e.setName(p.getName());
        e.setStartAt(p.getStartAt());
        e.setLocation(p.getLocation());
        e.setCapacity(p.getCapacity());
        e.setDescription(p.getDescription());
        e.setCoverUrl(p.getCoverUrl());
    }

    private EventDto toDto(Event e) {
        return new EventDto(
                e.getId(), e.getName(), e.getStartAt(), e.getLocation(),
                e.getCapacity(), e.getDescription(), e.getCoverUrl(),
                e.getRegisteredCount(), e.getCreatedAt()
        );
    }
}
