package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.exception.MissingTelegramChatIdException;
import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
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
    private final WhatsAppGraphClient whatsAppGraphClient;
    private final EventSubscriptionRepository eventSubscriptionRepository;
    private final EventSubscriptionWhatsappRepository eventSubscriptionWhatsappRepository;

    @Value("${telegram.defaultChannelChatId:}")
    private String defaultChannelChatId;

    /**
     * Відправка одного поста (в новій транзакції, щоб статус поста завжди зберігався).
     * Підтримує: TELEGRAM та WHATSAPP канали
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(Post post) {
        try {
            int sentCount = switch (post.getChannel()) {
                case TELEGRAM -> dispatchTelegram(post);
                case WHATSAPP -> dispatchWhatsApp(post);
                default -> throw new IllegalStateException("Unsupported channel: " + post.getChannel());
            };

            post.setStatus(PostStatus.PUBLISHED);
            post.setError(null);
            log.info("Post #{} delivered to {} target(s) via {}.", post.getId(), sentCount, post.getChannel());

        } catch (MissingTelegramChatIdException e) {
            post.setStatus(PostStatus.FAILED);
            post.setError("NO_TELEGRAM_CHAT_ID");
            log.warn("Dispatch failed for post #{}: no Telegram chatId", post.getId(), e);

        } catch (Exception e) {
            post.setStatus(PostStatus.FAILED);
            post.setError(shorten(e.getMessage(), 500));
            log.error("Dispatch failed for post #{}: {}", post.getId(), e.getMessage(), e);

        } finally {
            postRepository.save(post);
        }
    }

    /**
     * Диспетчеризація для Telegram каналу.
     */
    private int dispatchTelegram(Post post) {
        return switch (post.getAudience()) {
            case PUBLIC -> dispatchTelegramPublic(post);
            case SUBSCRIBERS -> dispatchTelegramSubscribers(post);
            default -> throw new IllegalStateException("Unsupported audience: " + post.getAudience());
        };
    }

    /**
     * Диспетчеризація для WhatsApp каналу.
     */
    private int dispatchWhatsApp(Post post) {
        return switch (post.getAudience()) {
            case PUBLIC -> dispatchWhatsAppPublic(post);
            case SUBSCRIBERS -> dispatchWhatsAppSubscribers(post);
            default -> throw new IllegalStateException("Unsupported audience: " + post.getAudience());
        };
    }

    /**
     * Публічний пост у Telegram канал
     */
    private int dispatchTelegramPublic(Post p) {
        String chatId = resolveTargetChatId(p);
        String text = buildPostText(p);

        Event e = p.getEvent();
        if (e == null || e.getId() == null) {
            throw new IllegalStateException("PUBLIC post must be linked to an Event");
        }

        String linkUrl = resolveEventLinkUrl(e);
        long publishedCount = postRepository.countByEvent_IdAndStatus(e.getId(), PostStatus.PUBLISHED);

        InlineKeyboardMarkup kb = (publishedCount == 0)
                ? telegramService.eventKeyboardPublicFirst(e.getId(), linkUrl)
                : telegramService.eventKeyboardPublicFollowup(e.getId(), linkUrl);

        try {
            telegramService.sendMessage(chatId, text, kb);
        } catch (TelegramApiException e1) {
            throw new RuntimeException("Telegram API error while sending PUBLIC post: " + e1.getMessage(), e1);
        }

        return 1;
    }

    /**
     * Розповсюдження Telegram постів підписникам (приват)
     */
    private int dispatchTelegramSubscribers(Post p) {
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
                log.error("Telegram API error while sending to subscriber chatId={}: {}", chatId, e1.getMessage(), e1);
            }
        }
        return sent;
    }

    /**
     * Публічний пост у WhatsApp канал
     */
    private int dispatchWhatsAppPublic(Post p) {
        Event e = p.getEvent();
        if (e == null || e.getId() == null) {
            throw new IllegalStateException("PUBLIC WhatsApp post must be linked to an Event");
        }

        String text = buildPostTextWithEvent(p, e);
        String linkUrl = resolveEventLinkUrl(e);

        // Отримуємо всіх підписників для публічного поста у WhatsApp
        List<String> waIds = eventSubscriptionWhatsappRepository.findSubscriberWaIds(e.getId());

        if (waIds.isEmpty()) {
            log.info("No WhatsApp subscribers for public post #{}", p.getId());
            return 0;
        }

        int sent = 0;
        for (String waId : waIds) {
            try {
                String messageWithLink = text;
                if (linkUrl != null && !linkUrl.isBlank()) {
                    messageWithLink = text + "\n\n🔗 " + linkUrl;
                }
                whatsAppGraphClient.sendText(waId, messageWithLink);
                sent++;
            } catch (Exception ex) {
                log.error("WhatsApp API error while sending to waId={}: {}", waId, ex.getMessage(), ex);
            }
        }

        return sent;
    }

    /**
     * Розповсюдження WhatsApp постів підписникам (приват)
     */
    private int dispatchWhatsAppSubscribers(Post p) {
        Event e = p.getEvent();
        if (e == null || e.getId() == null) return 0;

        List<String> waIds = eventSubscriptionWhatsappRepository.findSubscriberWaIds(e.getId());

        if (waIds.isEmpty()) {
            log.info("No WhatsApp subscribers for private post #{}", p.getId());
            return 0;
        }

        String text = buildPostTextWithEvent(p, e);
        String linkUrl = resolveEventLinkUrl(e);

        int sent = 0;
        for (String waId : waIds) {
            try {
                String messageWithLink = text;
                if (linkUrl != null && !linkUrl.isBlank()) {
                    messageWithLink = text + "\n\n🔗 " + linkUrl;
                }
                whatsAppGraphClient.sendText(waId, messageWithLink);
                sent++;
            } catch (Exception ex) {
                log.error("WhatsApp API error while sending to subscriber waId={}: {}", waId, ex.getMessage(), ex);
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

    private String buildPostTextWithEvent(Post p, Event e) {
        String eventName = e.getName() != null ? e.getName() : "Івент #" + e.getId();
        String eventDate = e.getStartAt() != null ?
            new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(e.getStartAt()) :
            "Дата невідома";

        String title = p.getTitle() != null ? p.getTitle() : "";
        String body = p.getBody() != null ? p.getBody() : "";

        return "📬 *" + eventName + "*\n" +
               "🕐 " + eventDate + "\n" +
               (e.getLocation() != null && !e.getLocation().isBlank() ? "📍 " + e.getLocation() + "\n" : "") +
               "\n" +
               (title.isBlank() ? "" : "*" + title + "*\n") +
               (body.isBlank() ? "" : body);
    }

    private String resolveEventLinkUrl(Event e) {
        if (e == null) return null;
        try {
            var m = e.getClass().getMethod("getUrl");
            Object v = m.invoke(e);
            if (v instanceof String s && s != null && !s.isBlank()) {
                return s.trim();
            }
        } catch (NoSuchMethodException ignore) {
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
