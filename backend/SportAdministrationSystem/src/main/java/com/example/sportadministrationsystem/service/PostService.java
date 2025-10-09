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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final EventRepository eventRepository;
    private final PostRepository postRepository;
    private final TelegramService telegramService;

    /** fallback chat id з application.yml: telegram.bot.chat-id */
    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    /* -------------------- CRUD -------------------- */

    @Transactional
    public PostDto create(Long eventId, PostPayload payload) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));

        PostStatus status = payload.status() == null || payload.status().isBlank()
                ? PostStatus.DRAFT : parseStatus(payload.status());

        Post p = Post.builder()
                .event(event)
                .title(payload.title())
                .body(payload.body())
                .publishAt(payload.publishAt())
                .status(status)
                .audience(parseAudience(payload.audience()))
                .channel(parseChannel(payload.channel()))
                .externalId(null)
                .error(null)
                .generated(false)
                .telegramChatId(payload.telegramChatId()) // може бути null — ок
                .build();

        return toDto(postRepository.save(p));
    }

    public PostDto get(Long eventId, Long postId) {
        Post p = getChecked(eventId, postId);
        return toDto(p);
    }

    public List<PostDto> list(Long eventId, String status, String audience, String channel) {
        return postRepository.findByEventAndFilters(
                eventId,
                status == null || status.isBlank() ? null : parseStatus(status),
                audience == null || audience.isBlank() ? null : parseAudience(audience),
                channel == null || channel.isBlank() ? null : parseChannel(channel)
        ).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long eventId, Long postId) {
        Post p = getChecked(eventId, postId);
        postRepository.delete(p);
    }

    /* -------------------- STATUS -------------------- */

    @Transactional
    public PostDto changeStatus(Long eventId, Long postId, String statusStr, String error) {
        Post p = getChecked(eventId, postId);
        PostStatus next = parseStatus(statusStr);
        validateTransition(p.getStatus(), next);
        p.setStatus(next);
        if (next == PostStatus.FAILED) {
            p.setError(error != null && !error.isBlank() ? error : "failed");
        } else {
            p.setError(null);
        }
        return toDto(postRepository.save(p));
    }

    private void validateTransition(PostStatus current, PostStatus next) {
        boolean ok =
                (current == PostStatus.DRAFT     && (next == PostStatus.SCHEDULED || next == PostStatus.CANCELED)) ||
                        (current == PostStatus.SCHEDULED && (next == PostStatus.PUBLISHED || next == PostStatus.CANCELED || next == PostStatus.FAILED)) ||
                        (current == PostStatus.FAILED    && (next == PostStatus.SCHEDULED || next == PostStatus.CANCELED)) ||
                        (current == PostStatus.CANCELED  && (next == PostStatus.SCHEDULED)) ||
                        (current == PostStatus.PUBLISHED && (next == PostStatus.PUBLISHED));
        if (!ok) throw new IllegalStateException("Illegal status transition: " + current + " -> " + next);
    }

    /* -------------------- DISPATCH -------------------- */

    /** Ручна публікація одного поста (ігнорує publishAt) */
    @Transactional
    public PostDto publishNow(Long eventId, Long postId) {
        Post p = getChecked(eventId, postId);
        try {
            sendToChannel(p);                     // <-- ловимо checked TelegramApiException
            p.setStatus(PostStatus.PUBLISHED);
            p.setError(null);
        } catch (TelegramApiException e) {
            p.setStatus(PostStatus.FAILED);
            p.setError(e.getMessage());
        }
        return toDto(postRepository.save(p));
    }

    /** Запустити відправку всіх SCHEDULED, у яких publishAt <= now */
    @Transactional
    public int dispatchDue() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> due = postRepository.findPostsToDispatch(PostStatus.SCHEDULED, now);
        int sent = 0;
        for (Post p : due) {
            try {
                sendToChannel(p);
                p.setStatus(PostStatus.PUBLISHED);
                p.setError(null);
                sent++;
            } catch (TelegramApiException e) {
                p.setStatus(PostStatus.FAILED);
                p.setError(e.getMessage());
            }
        }
        postRepository.saveAll(due);
        return sent;
    }

    /** Один канал відправки, може кинути TelegramApiException (checked) */
    private void sendToChannel(Post p) throws TelegramApiException {
        if (p.getChannel() == Channel.TELEGRAM) {
            String chatId = (p.getTelegramChatId() != null && !p.getTelegramChatId().isBlank())
                    ? p.getTelegramChatId()
                    : defaultChatId;
            if (chatId == null || chatId.isBlank()) {
                throw new TelegramApiException("Telegram chatId is not set (post.telegramChatId and telegram.bot.chat-id are empty)");
            }
            telegramService.sendMessage(chatId, p.getBody());
        } else {
            throw new IllegalStateException("Unsupported channel: " + p.getChannel());
        }
    }

    /* -------------------- helpers -------------------- */

    private Post getChecked(Long eventId, Long postId) {
        Post p = postRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found: " + postId));
        if (!p.getEvent().getId().equals(eventId)) {
            throw new NotFoundException("Post " + postId + " doesn't belong to event " + eventId);
        }
        return p;
    }

    private PostDto toDto(Post p) {
        return new PostDto(
                p.getId(),
                p.getEvent().getId(),
                p.getTitle(),
                p.getBody(),
                p.getPublishAt(),
                p.getStatus().name(),
                p.getAudience().name(),
                p.getChannel().name(),
                p.getExternalId(),
                p.getError(),
                p.isGenerated(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getTelegramChatId()
        );
    }

    private PostStatus parseStatus(String s) {
        try { return PostStatus.valueOf(s.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Unknown status: " + s); }
    }

    private Audience parseAudience(String s) {
        try { return Audience.valueOf(s.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Unknown audience: " + s); }
    }

    private Channel parseChannel(String s) {
        try { return Channel.valueOf(s.toUpperCase()); }
        catch (Exception e) { throw new IllegalArgumentException("Unknown channel: " + s); }
    }
    @jakarta.transaction.Transactional
    public PostDto update(Long eventId, Long postId, PostPayload payload) {
        Post p = getChecked(eventId, postId);

        p.setTitle(payload.title());
        p.setBody(payload.body());
        p.setPublishAt(payload.publishAt());
        p.setAudience(parseAudience(payload.audience()));
        p.setChannel(parseChannel(payload.channel()));

        // оновлення telegramChatId (порожній рядок = очищаємо override)
        String chat = payload.telegramChatId();
        p.setTelegramChatId((chat != null && !chat.isBlank()) ? chat : null);

        // (опційно) одразу можемо змінити статус, якщо передано
        if (payload.status() != null && !payload.status().isBlank()) {
            PostStatus next = parseStatus(payload.status());
            validateTransition(p.getStatus(), next);
            p.setStatus(next);
            if (next != PostStatus.FAILED) {
                p.setError(null);
            }
        }

        return toDto(postRepository.save(p));
    }
}
