package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.PostDto;
import com.example.sportadministrationsystem.dto.PostPayload;
import com.example.sportadministrationsystem.exception.NotFoundException;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final PostDispatchService postDispatchService;

    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    @Value("${dispatcher.batch-size:50}")
    private int batchSize;

    /* ===================== CRUD ===================== */

    @Transactional
    public PostDto create(Long eventId, PostPayload payload) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        PostStatus status = Optional.ofNullable(payload.status())
                .filter(s -> !s.isBlank())
                .map(this::parseStatus)
                .orElse(PostStatus.DRAFT);

        Audience audience = parseAudience(payload.audience());
        Channel channel = parseChannel(payload.channel());

        // Валідація: для PUBLIC posts потрібна підтримка (поки зараз тільки SUBSCRIBERS)
        if (audience == Audience.PUBLIC && channel == Channel.WHATSAPP) {
            throw new IllegalArgumentException("WHATSAPP PUBLIC posts not yet supported");
        }

        Post p = Post.builder()
                .event(event)
                .title(payload.title())
                .body(payload.body())
                .publishAt(payload.publishAt())
                .status(status)
                .audience(audience)
                .channel(channel)
                .externalId(null)
                .error(null)
                .generated(false)
                .build();

        return toDto(postRepository.save(p));
    }

    @Transactional
    public PostDto update(Long eventId, Long postId, PostPayload payload) {
        Post p = getChecked(eventId, postId);

        if (payload.title() != null && !payload.title().isBlank()) {
            p.setTitle(payload.title());
        }
        if (payload.body() != null && !payload.body().isBlank()) {
            p.setBody(payload.body());
        }
        if (payload.publishAt() != null) {
            p.setPublishAt(payload.publishAt());
        }
        if (payload.audience() != null && !payload.audience().isBlank()) {
            p.setAudience(parseAudience(payload.audience()));
        }
        if (payload.channel() != null && !payload.channel().isBlank()) {
            p.setChannel(parseChannel(payload.channel()));
        }
        if (payload.status() != null && !payload.status().isBlank()) {
            PostStatus next = parseStatus(payload.status());
            validateTransition(p.getStatus(), next);
            p.setStatus(next);
        }

        return toDto(postRepository.save(p));
    }

    @Transactional
    public void delete(Long eventId, Long postId) {
        Post p = getChecked(eventId, postId);
        postRepository.delete(p);
    }

    @Transactional
    public PostDto changeStatus(Long eventId, Long postId, String statusStr, String error) {
        Post p = getChecked(eventId, postId);
        PostStatus next = parseStatus(statusStr);
        validateTransition(p.getStatus(), next);
        p.setStatus(next);
        p.setError(error);
        return toDto(postRepository.save(p));
    }

    /* ===================== Queries ===================== */

    @Transactional
    public PostDto get(Long eventId, Long postId) {
        return toDto(getChecked(eventId, postId));
    }

    @Transactional
    public List<PostDto> list(Long eventId, String status, String audience, String channel) {
        PostStatus s = (status == null || status.isBlank()) ? null : parseStatus(status);
        Audience a = (audience == null || audience.isBlank()) ? null : parseAudience(audience);
        Channel c = (channel == null || channel.isBlank()) ? null : parseChannel(channel);
        return postRepository.findByEventIdAndFilters(eventId, s, a, c)
                .stream().map(this::toDto).toList();
    }

    /* ===================== Dispatch ===================== */

    /** Публікувати негайно (делегуємо всю логіку в PostDispatchService). */
    @Transactional
    public PostDto publishNow(Long eventId, Long postId) {
        Post p = getChecked(eventId, postId);

        // ВАЖЛИВО: Завантажуємо Event явно перед асинхронним dispatch
        // Замість простого "touch", потрібно переконатися що Event повністю завантажений
        Event event = p.getEvent();
        if (event != null) {
            // Примусово завантажуємо всі потрібні поля Event в поточній сесії
            event.getId();
            event.getName();
            event.getLocation();
            event.getStartAt();
        }

        postDispatchService.dispatch(p);
        return toDto(postRepository.save(p));
    }

    /**
     * Відправити всі SCHEDULED пости з publishAt <= now.
     * Використовує lockNextDueWithEvent() з FETCH JOIN для явного завантаження Event
     * перед асинхронною обробкою, що запобігає LazyInitializationException.
     */
    @Transactional
    public int dispatchDue() {
        LocalDateTime now = LocalDateTime.now();
        // Використовуємо JPQL запит з FETCH JOIN для явного завантаження Event
        List<Post> due = postRepository.lockNextDueWithEvent(now, batchSize);

        int processed = 0;
        for (Post p : due) {
            try {
                postDispatchService.dispatch(p);
                processed++;
            } catch (Exception ex) {
                log.error("Failed to dispatch post {}: {}", p.getId(), ex.getMessage());
            }
        }
        postRepository.saveAll(due);
        return processed;
    }

    /* ===================== Helpers ===================== */

    private void validateTransition(PostStatus current, PostStatus next) {
        if (Objects.equals(current, next)) return;

        // Дозволені переходи
        // DRAFT -> SCHEDULED | PUBLISHED | CANCELLED
        // SCHEDULED -> PUBLISHED | FAILED | CANCELLED
        // FAILED -> SCHEDULED | CANCELLED
        // PUBLISHED -> CANCELLED
        // CANCELLED -> (заборонено)
        switch (current) {
            case DRAFT -> {
                if (next != PostStatus.SCHEDULED && next != PostStatus.PUBLISHED && next != PostStatus.CANCELLED) {
                    throw new IllegalStateException("Illegal transition: " + current + " -> " + next);
                }
            }
            case SCHEDULED -> {
                if (next != PostStatus.PUBLISHED && next != PostStatus.FAILED && next != PostStatus.CANCELLED) {
                    throw new IllegalStateException("Illegal transition: " + current + " -> " + next);
                }
            }
            case FAILED -> {
                if (next != PostStatus.SCHEDULED && next != PostStatus.CANCELLED) {
                    throw new IllegalStateException("Illegal transition: " + current + " -> " + next);
                }
            }
            case PUBLISHED -> {
                if (next != PostStatus.CANCELLED) {
                    throw new IllegalStateException("Illegal transition: " + current + " -> " + next);
                }
            }
            case CANCELLED -> {
                throw new IllegalStateException("Illegal transition: " + current + " -> " + next);
            }
        }
    }

    private Post getChecked(Long eventId, Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found: " + postId));
        if (p.getEvent() == null || !Objects.equals(p.getEvent().getId(), eventId)) {
            throw new NotFoundException("Post " + postId + " does not belong to event " + eventId);
        }
        return p;
    }

    private PostDto toDto(Post p) {
        return new PostDto(
                p.getId(),
                p.getEvent() != null ? p.getEvent().getId() : null,
                p.getTitle(),
                p.getBody(),
                p.getPublishAt(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getAudience() != null ? p.getAudience().name() : null,
                p.getChannel() != null ? p.getChannel().name() : null,
                p.getExternalId(),
                p.getError(),
                p.isGenerated(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private PostStatus parseStatus(String s) {
        try {
            return PostStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown status: " + s);
        }
    }

    private Audience parseAudience(String s) {
        try {
            return Audience.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown audience: " + s);
        }
    }

    private Channel parseChannel(String s) {
        try {
            return Channel.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown channel: " + s);
        }
    }
}
