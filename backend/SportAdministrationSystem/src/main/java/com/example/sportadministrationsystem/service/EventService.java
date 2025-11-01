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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    /** Конкретний сервіс генерації постів — він є у твоєму контексті. */
    @Autowired(required = false)
    private PostGenerationService postGenerationService;

    /* ======================= READ ======================= */

    /** Загальний лістинг з опційними фільтрами category/tag і пагінацією. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<EventDto> list(String category, String tag, Pageable pageable) {
        // Якщо є прості фільтри — зробимо їх в пам’яті, але дані беремо вже відсортовані.
        Page<Event> page = eventRepository.findAll(pageable);
        List<Event> content = page.getContent();

        EventCategory categoryEnum = parseCategory(category);
        String tagNorm = normalize(tag);

        if (categoryEnum != null) {
            content = content.stream().filter(e -> e.getCategory() == categoryEnum).toList();
        }
        if (tagNorm != null) {
            String tl = tagNorm.toLowerCase(Locale.ROOT);
            content = content.stream().filter(e -> eventHasTag(e, tl)).toList();
        }

        return new PageImpl<>(content, pageable, page.getTotalElements()).map(this::toDto);
    }

    /** Пагінована вибірка за автором — для дашборда. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<EventDto> listByAuthor(Long userId, Pageable pageable) {
        if (userId == null) return Page.empty(pageable);
        Page<Event> page = eventRepository.findByCreatedById(userId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public EventDto getById(Long id) {
        Event e = eventRepository.findWithTagsById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        return toDto(e);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> findByName(String name) {
        String n = normalize(name);
        if (n == null) return List.of();
        return eventRepository.findAll().stream()
                .filter(e -> containsIgnoreCase(e.getName(), n))
                .map(this::toDto)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventDto> findByLocation(String location) {
        String n = normalize(location);
        if (n == null) return List.of();
        return eventRepository.findAll().stream()
                .filter(e -> containsIgnoreCase(e.getLocation(), n))
                .map(this::toDto)
                .toList();
    }

    /** Автор івента. Якщо автора немає — повертаємо порожній об'єкт замість 404. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public CreatorDto getCreator(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        User u = e.getCreatedBy();
        // Раніше тут кидали 404. Тепер віддаємо "порожнього" кріейтора,
        // щоб клієнт міг безпечно показати івент навіть із відсутнім автором.
        if (u == null) {
            return new CreatorDto(null, null);
        }
        return new CreatorDto(u.getId(), u.getUsername());
    }

    /* ======================= WRITE ======================= */

    /** Створення івенту: виставляємо автора + піднімаємо шаблони постів. */
    @Transactional
    public EventDto create(EventPayload payload) {
        Event e = new Event();

        // ВАЖЛИВО: автор = поточний користувач (інакше дашборд /by-author не працюватиме коректно)
        User current = resolveCurrentUser()
                .orElseThrow(() -> new NotFoundException("Current user not found"));
        e.setCreatedBy(current);

        applyPayload(e, payload);
        Event saved = eventRepository.save(e);

        // Автоматична генерація постів із шаблонів під новий івент (якщо сервіс є)
        tryEnsureTemplates(saved);

        return toDto(saved);
    }

    /** Оновлення існуючого івенту. */
    @Transactional
    public EventDto update(Long id, EventPayload payload) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));

        applyPayload(e, payload);
        Event saved = eventRepository.save(e);

        // Підтягнемо шаблонні/планові пости під оновлений івент (якщо сервіс є)
        tryEnsureTemplates(saved);

        return toDto(saved);
    }

    /** Видалення івенту. */
    @Transactional
    public void delete(Long id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: id=" + id));
        eventRepository.delete(e);
    }

    /* ======================= MAPPING ======================= */

    private EventDto toDto(Event e) {
        List<String> tags = (e.getTags() == null) ? List.of() :
                e.getTags().stream()
                        .filter(Objects::nonNull)
                        .map(Tag::getName)
                        .filter(Objects::nonNull)
                        .sorted()
                        .toList();

        String category = e.getCategory() == null ? null : e.getCategory().name();

        User creator = e.getCreatedBy();
        Long createdById = (creator != null) ? creator.getId() : null;
        String createdByUsername = (creator != null) ? creator.getUsername() : null;

        return new EventDto(
                e.getId(),
                e.getName(),
                e.getStartAt(),
                e.getLocation(),
                e.getCapacity(),
                e.getDescription(),
                e.getCoverUrl(),
                category,
                tags,
                createdById,
                createdByUsername
        );
    }

    private void applyPayload(Event e, EventPayload p) {
        e.setName(trimOrNull(p.getName()));
        e.setStartAt(p.getStartAt());
        e.setLocation(trimOrNull(p.getLocation()));
        e.setCapacity(p.getCapacity());
        e.setDescription(trimOrNull(p.getDescription()));
        e.setCoverUrl(trimOrNull(p.getCoverUrl()));

        // category
        String categoryStr = trimOrNull(p.getCategory());
        if (categoryStr == null) {
            e.setCategory(null);
        } else {
            try {
                e.setCategory(EventCategory.valueOf(categoryStr.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                e.setCategory(null);
            }
        }

        // tags: імена -> сутності (id створяться/підтягнуться у відповідному сервісі/репозиторії)
        Set<Tag> newTags = new HashSet<>();
        if (p.getTags() != null) {
            for (String raw : p.getTags()) {
                String name = trimOrNull(raw);
                if (name == null) continue;
                // тут можна через tagRepository.findByNameIgnoreCase(name), якщо він у тебе є; якщо ні — залиш як є
                Tag t = tagRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                    Tag nt = new Tag();
                    nt.setName(name);
                    return tagRepository.save(nt);
                });
                newTags.add(t);
            }
        }
        e.setTags(newTags);
    }

    /* ======================= HELPERS ======================= */

    private Optional<User> resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return Optional.empty();
        return userRepository.findByUsername(auth.getName());
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toLowerCase(Locale.ROOT);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean containsIgnoreCase(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static boolean eventHasTag(Event e, String tagLower) {
        return e.getTags() != null && e.getTags().stream()
                .anyMatch(x -> x != null && x.getName() != null &&
                        x.getName().toLowerCase(Locale.ROOT).contains(tagLower));
    }

    private EventCategory parseCategory(String category) {
        String c = normalize(category);
        if (c == null) return null;
        try {
            return EventCategory.valueOf(c.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Легкий виклик генерації шаблонних/планових постів після create/update. */
    private void tryEnsureTemplates(Event saved) {
        if (postGenerationService == null || saved == null) return;
        try {
            // У твоєму PostGenerationService є саме цей метод:
            // public void ensureEventScheduledPosts(Event e)
            postGenerationService.ensureEventScheduledPosts(saved);
        } catch (Exception ignore) {
            // не блокуємо CRUD у разі збоїв генерації
        }
    }
}
