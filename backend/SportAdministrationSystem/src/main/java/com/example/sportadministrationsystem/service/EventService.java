package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.CreatorDto;
import com.example.sportadministrationsystem.dto.EventDto;
import com.example.sportadministrationsystem.dto.EventPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import com.example.sportadministrationsystem.model.Tag;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.TagRepository;
import com.example.sportadministrationsystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    // Якщо маєш генератор постів — заінжекть через конструктор; якщо ні — залиш null.
    private final PostGenerationService postGenerationService = null;

    /* ============================================================
       Списки / Пошук
       ============================================================ */

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<EventDto> list(String category, String tag, Pageable pageable) {
        EventCategory cat = parseCategoryOrNull(category);
        String tagName = normalizeOrNull(tag);

        if (cat == null && tagName == null) {
            return eventRepository.findAll(pageable).map(this::toDto);
        }
        if (cat != null && tagName == null) {
            return eventRepository.findByCategory(cat, pageable).map(this::toDto);
        }
        if (cat == null /* && tagName != null */) {
            return eventRepository.findByTagName(tagName, pageable).map(this::toDto);
        }
        return eventRepository.findByCategoryAndTag(cat, tagName, pageable).map(this::toDto);
    }
    @Transactional(Transactional.TxType.SUPPORTS)
    public Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<EventDto> listByAuthor(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: id=" + userId));
        return eventRepository.findByCreatedById(userId, pageable).map(this::toDto);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EventDto getEventById(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        return toDto(e);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> findByName(String name) {
        String needle = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return eventRepository.findAll().stream()
                .filter(e -> Objects.toString(e.getName(), "").toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> findByLocation(String location) {
        String needle = location == null ? "" : location.toLowerCase(Locale.ROOT);
        return eventRepository.findAll().stream()
                .filter(e -> Objects.toString(e.getLocation(), "").toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ============================================================
       CRUD
       ============================================================ */

    @Transactional
    public EventDto create(EventPayload payload) {
        Event e = new Event();
        applyPayload(e, payload);
        Event saved = eventRepository.save(e);
        tryInvokePostGenerator(saved);
        return toDto(saved);
    }

    @Transactional
    public EventDto update(Long id, EventPayload payload) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        applyPayload(e, payload);
        return toDto(eventRepository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        eventRepository.delete(e);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public CreatorDto getCreator(Long eventId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + eventId));
        User u = e.getCreatedBy();
        if (u == null) return new CreatorDto(null, null);
        return new CreatorDto(u.getId(), u.getUsername());
    }

    /* ============================================================
       Мапінг / Хелпери
       ============================================================ */

    private void applyPayload(Event e, EventPayload p) {
        // Прості поля
        e.setName(p.getName());
        e.setStartAt(p.getStartAt());
        e.setLocation(p.getLocation());
        e.setCapacity(p.getCapacity());
        e.setDescription(p.getDescription());
        e.setCoverUrl(p.getCoverUrl());

        // String -> enum
        e.setCategory(parseCategoryOrNull(p.getCategory()));

        // tags: List<String> -> Set<Tag> (знаходимо/створюємо теги)
        Collection<String> src = Optional.ofNullable(p.getTags()).orElseGet(Collections::emptyList);
        Set<String> names = src.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Tag> tags = new LinkedHashSet<>();
        for (String name : names) {
            Tag t = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Tag nt = new Tag();
                        nt.setName(name);
                        return tagRepository.save(nt);
                    });
            tags.add(t);
        }
        e.setTags(tags);
    }

    private EventDto toDto(Event e) {
        // Нормалізовані значення
        Long id = e.getId();
        String name = e.getName();
        LocalDateTime startAt = e.getStartAt();
        String location = e.getLocation();
        Integer capacity = e.getCapacity();
        String description = e.getDescription();
        String coverUrl = e.getCoverUrl();
        String categoryStr = (e.getCategory() != null) ? e.getCategory().name() : null;

        // Теги -> список назв (List<String>), щоб точно не було Set<Tag> -> List<String> конфлікту
        List<String> tagList = (e.getTags() == null)
                ? Collections.emptyList()
                : e.getTags().stream()
                .map(Tag::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Якщо в тебе `EventDto` — record із (category:String, tags:List<String>) — використовуємо його напряму
        return new EventDto(
                id, name, startAt, location, capacity, description, coverUrl,
                categoryStr, tagList,
                e.getCreatedBy() != null ? e.getCreatedBy().getId() : null,
                e.getCreatedBy() != null ? e.getCreatedBy().getUsername() : null
        );
    }

    /* ============================================================
       Технічні хелпери
       ============================================================ */

    private boolean assignable(Class<?> a, Class<?> b) {
        return a.isAssignableFrom(b);
    }

    private EventCategory parseCategoryOrNull(String s) {
        String t = normalizeOrNull(s);
        if (t == null) return null;
        try { return EventCategory.valueOf(t); }
        catch (IllegalArgumentException ex) { return null; }
    }

    private String normalizeOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Опціональний виклик генератора постів; якщо сервіс відсутній — тихо пропускаємо. */
    private void tryInvokePostGenerator(Event saved) {
        if (postGenerationService == null) return;
        try {
            Method m = postGenerationService.getClass().getMethod("generateInitialPostsForEvent", Event.class);
            m.invoke(postGenerationService, saved);
        } catch (Exception ignore) { /* нічого страшного */ }
    }
}
