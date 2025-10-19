package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostDispatchService {

    /** Більше НЕ інжектимо AbsSender — напряму використовуємо TelegramService. */
    private final TelegramService telegramService;

    private final PostRepository postRepository;
    private final EventSubscriptionRepository subscriptionRepository;

    /**
     * Значення за замовчуванням для публічного каналу/чату, якщо у пості не задано.
     * Може бути як "-1001234567890", так і @channelUsername — але для API краще ID.
     */
    @Value("${telegram.bot.chat-id:}")
    private String defaultChannelChatId;

    /**
     * Відправляє пост згідно з його каналом/аудиторією. Оновлює статус/помилку.
     */
    @Transactional
    public void dispatch(Post post) {
        try {
            int sentCount;
            if (post.getChannel() != Channel.TELEGRAM) {
                throw new IllegalStateException("Unsupported channel: " + post.getChannel());
            }

            if (post.getAudience() == Audience.PUBLIC) {
                sentCount = dispatchPublic(post);
            } else if (post.getAudience() == Audience.SUBSCRIBERS) {
                sentCount = dispatchSubscribers(post);
            } else {
                throw new IllegalStateException("Unsupported audience: " + post.getAudience());
            }

            log.info("Post #{} delivered to {} target(s).", post.getId(), sentCount);
            post.setStatus(PostStatus.PUBLISHED);
            post.setError(null);
        } catch (Exception e) {
            log.warn("Dispatch failed for post #{}: {}", post.getId(), e.getMessage(), e);
            post.setStatus(PostStatus.FAILED);
            post.setError(shorten(e.getMessage(), 500));
        }

        postRepository.save(post);
    }

    /** Відправка у конкретний канал/чат (PUBLIC). */
    private int dispatchPublic(Post p) throws TelegramApiException {
        String chatId = resolveTargetChatId(p);
        String text = buildText(p);
        InlineKeyboardMarkup kb = (p.getEvent() != null && p.getEvent().getId() != null)
                ? telegramService.eventKeyboard(p.getEvent().getId(), false)
                : null;

        telegramService.sendMessage(chatId, text, kb);
        return 1;
    }

    /**
     * Відправка всім Telegram-передплатникам події (SUBSCRIBERS).
     * Використовує findSubscriberChatIds(...) з репозиторію.
     */
    private int dispatchSubscribers(Post p) {
        Event event = requireEvent(p);

        List<Long> chatIds = subscriptionRepository.findSubscriberChatIds(
                event.getId(),
                Messenger.TELEGRAM.name()
        );

        int sent = 0;
        if (chatIds == null || chatIds.isEmpty()) {
            log.info("No Telegram subscribers for event #{}", event.getId());
            return 0;
        }

        String text = buildText(p);
        InlineKeyboardMarkup kb = telegramService.eventKeyboard(event.getId(), true);

        for (Long chatId : chatIds) {
            try {
                telegramService.sendMessage(String.valueOf(chatId), text, kb);
                sent++;
            } catch (TelegramApiException te) {
                log.warn("Delivery to chat {} failed: {}", chatId, te.getMessage());
            }
        }
        return sent;
    }

    /** Якщо у пості задано кастомний telegramChatId — використовуємо його, інакше беремо дефолт з application.yml. */
    private String resolveTargetChatId(Post p) {
        if (p.getTelegramChatId() != null && !p.getTelegramChatId().isBlank()) {
            return p.getTelegramChatId();
        }
        if (defaultChannelChatId != null && !defaultChannelChatId.isBlank()) {
            return defaultChannelChatId;
        }
        throw new IllegalStateException("No target Telegram chat id configured for PUBLIC post");
    }

    /** Текст повідомлення (заголовок + тіло). */
    private String buildText(Post p) {
        if (p.getTitle() != null && !p.getTitle().isBlank()) {
            return "⭐ " + p.getTitle() + "\n\n" + (p.getBody() == null ? "" : p.getBody());
        }
        return p.getBody() == null ? "" : p.getBody();
    }

    private Event requireEvent(Post p) {
        Event e = p.getEvent();
        if (e == null || e.getId() == null) {
            throw new IllegalStateException("Post must be linked to an Event for this operation");
        }
        return e;
    }

    private static String shorten(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}
