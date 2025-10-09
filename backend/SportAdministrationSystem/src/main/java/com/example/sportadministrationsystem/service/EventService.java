package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import com.example.sportadministrationsystem.model.Tag;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final PostGenerationService postGenerationService;
    /* ===================== Public API ===================== */

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getAllEvents() {
        return eventRepository.findAllWithTags()
                .stream().map(this::toDto).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getAllEvents(String category, String tag) {
        EventCategory cat = parseCategoryOrNull(category);
        String tagName = normalizeOrNull(tag);
        if (cat == null && tagName == null) {
            return getAllEvents();
        }
        return eventRepository.findAllByCategoryAndTag(cat, tagName)
                .stream().map(this::toDto).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EventDto getEventById(Long id) {
        Event e = eventRepository.findByIdWithTags(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return toDto(e);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getEventsByName(String name) {
        String q = Objects.requireNonNullElse(name, "").trim();
        return eventRepository.findByNameContainingIgnoreCase(q)
                .stream().map(this::toDto).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> getEventsByLocation(String location) {
        String q = Objects.requireNonNullElse(location, "").trim();
        return eventRepository.findByLocationContainingIgnoreCase(q)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public EventDto create(EventPayload payload) {
        Event e = apply(new Event(), payload);
        Event saved = eventRepository.save(e);
        postGenerationService.ensureEventScheduledPosts(saved);
        return toDto(saved);
    }

    @Transactional
    public EventDto update(Long id, EventPayload payload) {
        Event e = eventRepository.findByIdWithTags(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        apply(e, payload);
        Event saved = eventRepository.save(e);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        eventRepository.delete(e);
    }

    /* ===================== Helpers ===================== */

    private Event apply(Event e, EventPayload p) {
        e.setName(p.getName());
        e.setStartAt(p.getStartAt());
        e.setLocation(p.getLocation());
        e.setCapacity(p.getCapacity());
        e.setDescription(p.getDescription());
        e.setCoverUrl(p.getCoverUrl());

        if (p.getCategory() == null || p.getCategory().isBlank()) {
            e.setCategory(null);
        } else {
            e.setCategory(parseCategory(p.getCategory()));
        }

        e.setTags(resolveTags(p.getTags()));
        return e;
    }

    private Set<Tag> resolveTags(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        return names.stream()
                .map(this::normalizeOrNull)
                .filter(Objects::nonNull)
                .distinct()
                .map(n -> tagRepository.findByName(n)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(n).build())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private EventCategory parseCategory(String s) {
        try {
            return EventCategory.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown category: " + s);
        }
    }

    private EventCategory parseCategoryOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return parseCategory(s);
    }

    private String normalizeOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private EventDto toDto(Event e) {
        List<String> tagNames = e.getTags() == null ? List.of()
                : e.getTags().stream()
                .map(Tag::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String category = e.getCategory() == null ? null : e.getCategory().name();

        return new EventDto(
                e.getId(),
                e.getName(),
                e.getStartAt(),
                e.getLocation(),
                e.getCapacity(),
                e.getDescription(),
                e.getCoverUrl(),
                category,
                tagNames
        );
    }
    public Event loadEntity(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }
}
