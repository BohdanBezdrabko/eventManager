package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final EventRepository events;
    private final EventSubscriptionRepository subs;
    private final TelegramAccountProvisioner provisioner;

    /* ===================== Public API ===================== */

    /** Відправка повідомлення із (необов’язковою) інлайн-клавіатурою. */
    public Message sendMessage(String chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(text == null ? "" : text)
                .replyMarkup(keyboard)
                .build();
        return execute(sm);
    }

    /**
     * Клавіатура під ПЕРШИМ публічним постом: миттєва підписка/відписка через callback.
     * Якщо є URL івента — додається друга кнопка «Посилання».
     */
    public InlineKeyboardMarkup eventKeyboard(long eventId, boolean subscribed, String linkUrl) {
        InlineKeyboardButton subBtn = subscribed
                ? InlineKeyboardButton.builder().text("Відписатися").callbackData("EVT_UNSUB:" + eventId).build()
                : InlineKeyboardButton.builder().text("Зареєструватися").callbackData("EVT_SUB:" + eventId).build();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(subBtn));

        if (linkUrl != null && !linkUrl.isBlank()) {
            rows.add(List.of(InlineKeyboardButton.builder().text("Посилання").url(linkUrl).build()));
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    /**
     * Клавіатура для ДРУГОГО+ публічного поста: deep-link у чат з ботом, де показується персональний стан.
     * Якщо є URL івента — додається друга кнопка «Посилання».
     */
    public InlineKeyboardMarkup eventKeyboardPublicFollowup(long eventId, String linkUrl) {
        String deepLink = "https://t.me/" + botUsername + "?start=event-" + eventId;
        InlineKeyboardButton manageBtn = InlineKeyboardButton.builder()
                .text("Керувати підпискою")
                .url(deepLink)
                .build();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(manageBtn));

        if (linkUrl != null && !linkUrl.isBlank()) {
            rows.add(List.of(InlineKeyboardButton.builder().text("Посилання").url(linkUrl).build()));
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    /* ============================ LongPolling ============================ */

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update == null) return;

            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
                return;
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText().trim();

                if (text.equalsIgnoreCase("/start")) {
                    sendMessage(String.valueOf(update.getMessage().getChatId()),
                            "Вітаю! Натискайте кнопки під постами, щоб керувати підпискою на нагадування.", null);
                    return;
                }

                // Deep-link: /start event-<id>
                if (text.startsWith("/start ")) {
                    String payload = text.substring(7).trim();
                    if (payload.startsWith("event-")) {
                        String idStr = payload.substring("event-".length());
                        try {
                            long eventId = Long.parseLong(idStr);
                            handleStartWithEvent(update.getMessage().getChatId(), eventId, update);
                            return;
                        } catch (NumberFormatException ignore) { /* no-op */ }
                    }
                }
            }
        } catch (Exception e) {
            log.error("onUpdateReceived failed", e);
        }
    }

    private void handleStartWithEvent(long chatId, long eventId, Update update) throws TelegramApiException {
        // створюємо/знаходимо Telegram-акаунт користувача (ВАЖЛИВО: без chatId у сигнатурі)
        UserTelegram tgAcc = provisioner.ensure(update.getMessage().getFrom());

        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            sendMessage(String.valueOf(chatId), "Івент не знайдено.", null);
            return;
        }

        boolean isSubscribed = subs.existsByEventAndUserTelegramAndMessengerAndActiveIsTrue(
                event, tgAcc, Messenger.TELEGRAM);

        String link = resolveEventLinkUrl(event);
        InlineKeyboardMarkup kb = eventKeyboard(eventId, isSubscribed, link);

        String text = isSubscribed
                ? "Ви вже підписані на нагадування цього івента. Можете відписатися."
                : "Ви не підписані на цей івент. Можете підписатися.";
        sendMessage(String.valueOf(chatId), text, kb);
    }

    @Transactional
    protected void handleCallback(CallbackQuery cb) throws TelegramApiException {
        String data = cb.getData();
        long chatId = cb.getMessage().getChatId();
        Integer messageId = cb.getMessage() != null ? cb.getMessage().getMessageId() : null;

        // забезпечуємо існування Telegram-акаунта (ВАЖЛИВО: без chatId у сигнатурі)
        UserTelegram tgAcc = provisioner.ensure(cb.getFrom());

        if (data != null && data.startsWith("EVT_SUB:")) {
            long eventId = parseId(data, "EVT_SUB:");
            toggleSubscription(eventId, tgAcc, true);

            String link = resolveEventLinkUrl(events.findById(eventId).orElse(null));
            InlineKeyboardMarkup kb = eventKeyboard(eventId, true, link);
            updateOriginalMessageKeyboard(chatId, messageId, kb);
            ack(cb, "Ви підписані на нагадування про івент.");
            return;
        }

        if (data != null && data.startsWith("EVT_UNSUB:")) {
            long eventId = parseId(data, "EVT_UNSUB:");
            toggleSubscription(eventId, tgAcc, false);

            String link = resolveEventLinkUrl(events.findById(eventId).orElse(null));
            InlineKeyboardMarkup kb = eventKeyboard(eventId, false, link);
            updateOriginalMessageKeyboard(chatId, messageId, kb);
            ack(cb, "Ви відписалися від нагадувань.");
            return;
        }

        ack(cb, "Оновлено");
    }

    private void updateOriginalMessageKeyboard(long chatId, Integer messageId, InlineKeyboardMarkup kb) throws TelegramApiException {
        if (messageId == null) return;
        EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(kb)
                .build();
        execute(edit);
    }

    private void ack(CallbackQuery cb, String text) throws TelegramApiException {
        AnswerCallbackQuery ack = AnswerCallbackQuery.builder()
                .callbackQueryId(cb.getId())
                .text(text)
                .showAlert(false)
                .build();
        execute(ack);
    }

    @Transactional
    protected void toggleSubscription(long eventId, UserTelegram tgAcc, boolean desired) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        Optional<EventSubscription> found =
                subs.findByEventAndUserTelegramAndMessenger(event, tgAcc, Messenger.TELEGRAM);

        if (desired) {
            if (found.isEmpty()) {
                EventSubscription es = new EventSubscription();
                es.setEvent(event);
                es.setUserTelegram(tgAcc);
                es.setMessenger(Messenger.TELEGRAM);
                es.setActive(true);
                subs.save(es);
            } else if (!found.get().isActive()) { // ВАЖЛИВО: boolean ґеттер — isActive()
                found.get().setActive(true);
            }
        } else {
            found.ifPresent(es -> es.setActive(false));
        }
    }

    /** Повертає URL для кнопки «Посилання»: спочатку Event.url, інакше Event.coverUrl. */
    String resolveEventLinkUrl(Event e) {
        if (e == null) return null;

        // Спершу пробуємо getUrl()
        try {
            var m = e.getClass().getMethod("getUrl");
            Object v = m.invoke(e);
            if (v instanceof String s && s != null && !s.isBlank()) return s.trim();
        } catch (NoSuchMethodException ignore) { /* поля url може не бути — ок */ }
        catch (Exception ex) { log.warn("resolveEventLinkUrl via getUrl failed: {}", ex.toString()); }

        // fallback — coverUrl
        try {
            var m2 = e.getClass().getMethod("getCoverUrl");
            Object v2 = m2.invoke(e);
            if (v2 instanceof String s2 && s2 != null && !s2.isBlank()) return s2.trim();
        } catch (Exception ex) { log.warn("resolveEventLinkUrl via getCoverUrl failed: {}", ex.toString()); }

        return null;
    }

    private long parseId(String data, String prefix) {
        return Long.parseLong(data.substring(prefix.length()));
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }
}
