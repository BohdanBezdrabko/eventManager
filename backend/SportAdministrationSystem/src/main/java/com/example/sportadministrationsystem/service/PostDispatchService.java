package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostDispatchService {

    private final PostRepository posts;
    private final EventSubscriptionRepository subscriptions;
    private final TelegramService telegram;

    /** Дефолтний chatId (наприклад, канал), якщо не задано у пості. */
    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    /** Розмір батчу на один тік */
    @Value("${dispatch.batch-size:50}")
    private int batchSize;

    /** Головний тікер розсилки — бере SCHEDULED, де publishAt <= now. */
    @Scheduled(fixedDelayString = "${dispatch.tick-ms:5000}")
    public void scheduledTick() {
        int sent = dispatchTick();
        if (sent > 0) {
            log.info("PostDispatchService: sent {}", sent);
        }
    }

    /** Виконує один тік розсилки та повертає кількість успішно опублікованих постів. */
    public int dispatchTick() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> due = posts.findPostsToDispatch(PostStatus.SCHEDULED, now);
        int published = 0;
        for (Post p : due.stream().limit(batchSize).toList()) {
            try {
                switch (p.getAudience()) {
                    case PUBLIC -> published += dispatchPublic(p);
                    case SUBSCRIBERS -> published += dispatchSubscribers(p);
                    default -> {
                        p.setStatus(PostStatus.FAILED);
                        p.setError("Unsupported audience: " + p.getAudience());
                        posts.save(p);
                    }
                }
            } catch (Exception e) {
                p.setStatus(PostStatus.FAILED);
                p.setError(e.getMessage());
                posts.save(p);
                log.warn("Dispatch failed for post #{}: {}", p.getId(), e.getMessage());
            }
        }
        return published;
    }

    private int dispatchPublic(Post p) throws TelegramApiException {
        String chatId = (p.getTelegramChatId() != null && !p.getTelegramChatId().isBlank())
                ? p.getTelegramChatId()
                : defaultChatId;
        if (chatId == null || chatId.isBlank()) {
            p.setStatus(PostStatus.FAILED);
            p.setError("No Telegram chat configured for PUBLIC post (post.telegramChatId or telegram.bot.chat-id).");
            posts.save(p);
            return 0;
        }
        Long eventId = p.getEvent() != null ? p.getEvent().getId() : null;
        if (eventId != null) {
            telegram.sendMessage(chatId, p.getBody(), telegram.eventKeyboard(eventId, false));
        } else {
            telegram.sendMessage(chatId, p.getBody());
        }
        p.setStatus(PostStatus.PUBLISHED);
        p.setError(null);
        posts.save(p);
        return 1;
    }

    private int dispatchSubscribers(Post p) {
        Long eventId = p.getEvent() != null ? p.getEvent().getId() : null;
        if (eventId == null) {
            p.setStatus(PostStatus.FAILED);
            p.setError("SUBSCRIBERS post must be linked to an Event.");
            posts.save(p);
            return 0;
        }
        List<Long> chatIds = subscriptions.findSubscriberChatIds(eventId, Messenger.TELEGRAM.name());
        int ok = 0, fail = 0;
        for (Long chatId : chatIds) {
            try {
                telegram.sendMessage(String.valueOf(chatId), p.getBody(), telegram.eventKeyboard(eventId, true));
                ok++;
            } catch (TelegramApiException e) {
                fail++;
                log.warn("Dispatch to {} failed for post #{}: {}", chatId, p.getId(), e.getMessage());
            }
        }
        if (fail == 0) {
            p.setStatus(PostStatus.PUBLISHED);
            p.setError(null);
        } else if (ok > 0) {
            p.setStatus(PostStatus.PUBLISHED); // частковий успіх
            p.setError("Delivered to some subscribers; failures: " + fail);
        } else {
            p.setStatus(PostStatus.FAILED);
            p.setError("No deliveries succeeded to subscribers.");
        }
        posts.save(p);
        return ok > 0 ? 1 : 0;
    }
}
