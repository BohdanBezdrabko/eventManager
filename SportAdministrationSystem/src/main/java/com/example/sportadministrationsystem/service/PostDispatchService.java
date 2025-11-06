package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.exception.MissingTelegramChatIdException;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDispatchService {

    private final PostRepository postRepository;
    private final TelegramService telegramService;
    private final EventSubscriptionRepository eventSubscriptionRepository;

    @Value("${telegram.defaultChannelChatId:}")
    private String defaultChannelChatId;

    /**
     * Відправка одного поста, викликається планувальником або вручну.
     * Ізольована транзакція: будь-яка помилка НЕ зламає зовнішню транзакцію.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Post post) {
        try {
            if (post.getChannel() != Channel.TELEGRAM) {
                throw new IllegalStateException("Unsupported channel: " + post.getChannel());
            }

            int sentCount;
            if (post.getAudience() == Audience.PUBLIC) {
                sentCount = dispatchPublic(post);
            } else if (post.getAudience() == Audience.SUBSCRIBERS) {
                sentCount = dispatchSubscribers(post);
            } else {
                throw new IllegalStateException("Unsupported audience: " + post.getAudience());
            }

            post.setStatus(PostStatus.PUBLISHED);
            post.setError(null);
            log.info("Post #{} delivered to {} target(s).", post.getId(), sentCount);

        } catch (MissingTelegramChatIdException e) {
            post.setStatus(PostStatus.FAILED);
            // фіксований код помилки для фронта / інших сервісів
            post.setError("NO_TELEGRAM_CHAT_ID");
            log.warn("Dispatch failed for post #{}: no Telegram chatId", post.getId(), e);
            // НЕ прокидуємо далі — щоб не ставити транзакцію зовнішнього рівня у rollback-only

        } catch (Exception e) {
            post.setStatus(PostStatus.FAILED);
            post.setError(shorten(e.getMessage(), 500));
            log.error("Dispatch failed for post #{}: {}", post.getId(), e.getMessage(), e);
            // НЕ прокидуємо далі — щоб статус/помилка гарантовано закомітились

        } finally {
            postRepository.save(post);
        }
    }

    /**
     * Відправка публічного поста в канал/чат.
     */
    private int dispatchPublic(Post p) {
        String chatId = resolveTargetChatId(p);
        String text = buildPostText(p);

        Event e = p.getEvent();
        if (e == null || e.getId() == null) {
            throw new IllegalStateException("PUBLIC post must be linked to an Event");
        }

        String linkUrl = resolveEventLinkUrl(e);

        // якщо це перший вже опублікований пост по івенту — робимо "живий" callback;
        // якщо ні — ставимо deep-link у бот для персонального відображення статусу
        long publishedCount = postRepository.countByEvent_IdAndStatus(e.getId(), PostStatus.PUBLISHED);
        InlineKeyboardMarkup kb = (publishedCount == 0)
                ? telegramService.eventKeyboard(e.getId(), false, linkUrl)                 // 1-й пост
                : telegramService.eventKeyboardPublicFollowup(e.getId(), linkUrl);         // 2-й і далі

        try {
            telegramService.sendMessage(chatId, text, kb);
        } catch (TelegramApiException e1) {
            // загортаємо в RuntimeException, щоб не тягнути checked exception нагору
            throw new RuntimeException("Telegram API error while sending PUBLIC post: " + e1.getMessage(), e1);
        }

        return 1;
    }

    /**
     * Відправка поста підписникам івенту (direct повідомлення).
     */
    private int dispatchSubscribers(Post p) {
        Event e = p.getEvent();
        if (e == null || e.getId() == null) return 0;

        List<Long> chatIds = eventSubscriptionRepository
                .findSubscriberChatIds(e.getId(), Messenger.TELEGRAM);

        String text = buildPostText(p);
        String linkUrl = resolveEventLinkUrl(e);
        InlineKeyboardMarkup kb = telegramService.eventKeyboard(e.getId(), true, linkUrl);

        int sent = 0;
        for (Long chatId : chatIds) {
            try {
                telegramService.sendMessage(String.valueOf(chatId), text, kb);
                sent++;
            } catch (TelegramApiException e1) {
                // логнемо, але дамо іншим отримати свій месседж
                log.error("Telegram API error while sending to subscriber chatId={}: {}", chatId, e1.getMessage(), e1);
            }
        }
        return sent;
    }

    private String resolveTargetChatId(Post p) {
        if (p.getTelegramChatId() != null && !p.getTelegramChatId().isBlank()) {
            return p.getTelegramChatId();
        }
        if (defaultChannelChatId != null && !defaultChannelChatId.isBlank()) {
            return defaultChannelChatId;
        }
        throw new MissingTelegramChatIdException("No Telegram chatId for PUBLIC post");
    }

    private String buildPostText(Post p) {
        String t = p.getTitle() == null ? "" : p.getTitle();
        String b = p.getBody() == null ? "" : p.getBody();
        return (t + (b.isBlank() ? "" : "\n\n" + b)).trim();
    }

    /**
     * 1) Перевага за Event.getUrl() (якщо додав це поле в модель)
     * 2) Відкат на Event.getCoverUrl()
     */
    private String resolveEventLinkUrl(Event e) {
        if (e == null) return null;
        try {
            var m = e.getClass().getMethod("getUrl");
            Object v = m.invoke(e);
            if (v instanceof String s && s != null && !s.isBlank()) {
                return s.trim();
            }
        } catch (NoSuchMethodException ignore) {
            // не додано поле url — ок
        } catch (Exception ex) {
            log.warn("resolveEventLinkUrl via getUrl failed: {}", ex.toString());
        }
        try {
            var m2 = e.getClass().getMethod("getCoverUrl");
            Object v2 = m2.invoke(e);
            if (v2 instanceof String s2 && s2 != null && !s2.isBlank()) {
                return s2.trim();
            }
        } catch (Exception ex) {
            log.warn("resolveEventLinkUrl via getCoverUrl failed: {}", ex.toString());
        }
        return null;
    }

    private static String shorten(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
