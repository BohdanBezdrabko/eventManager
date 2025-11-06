package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService extends TelegramLongPollingBot {

    private final EventRepository events;
    private final EventSubscriptionRepository subs;
    private final TelegramAccountProvisioner provisioner;

    // Транзакційний сервіс підписок (виносить write-логіку, щоб не було No EntityManager ...)
    private final EventSubscriptionService eventSubscriptionService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    /* ============================ ПУБЛІЧНИЙ API для інших сервісів ============================ */

    /**
     * Публічний метод, який можна викликати з інших сервісів (PostDispatchService тощо).
     * Кидає TelegramApiException для коректного хендлінгу на рівні викликача.
     */
    public void sendMessage(String chatId, String text, InlineKeyboardMarkup kb) throws TelegramApiException {
        SendMessage msg = new SendMessage(chatId, text);
        if (kb != null) msg.setReplyMarkup(kb);
        execute(msg);
    }

    /**
     * Клавіатура для карточки івенту з можливістю безпосередньо підписатися/відписатися (callback_data).
     * Використовується коли очікуємо callback від користувача (наприклад, при першому пості чи в приваті).
     */
    public InlineKeyboardMarkup eventKeyboard(long eventId, boolean subscribed, String linkUrl) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton toggle = new InlineKeyboardButton();
        toggle.setText(subscribed ? "Відписатися" : "Підписатися");
        toggle.setCallbackData(subscribed ? ("EVT_UNSUB:" + eventId) : ("EVT_SUB:" + eventId));
        rows.add(List.of(toggle));

        if (linkUrl != null && !linkUrl.isBlank()) {
            InlineKeyboardButton link = new InlineKeyboardButton();
            link.setText("Посилання");
            link.setUrl(linkUrl);
            rows.add(List.of(link));
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    /**
     * Клавіатура для наступних PUBLIC-постів у канал: у каналах callback-кнопки часто не те, що нам треба.
     * Тому даємо URL-кнопку з deep-link у бота `/start <eventId>`, щоб користувач відкрив бота
     * і вже там натиснув підписку. Додаємо опційне посилання на івент.
     */
    public InlineKeyboardMarkup eventKeyboardPublicFollowup(long eventId, String linkUrl) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton manage = new InlineKeyboardButton();
        manage.setText("Керувати підпискою");
        manage.setUrl(buildStartDeepLink(eventId));
        rows.add(List.of(manage));

        if (linkUrl != null && !linkUrl.isBlank()) {
            InlineKeyboardButton link = new InlineKeyboardButton();
            link.setText("Посилання");
            link.setUrl(linkUrl);
            rows.add(List.of(link));
        }

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    /* ============================ Bot lifecycle ============================ */

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String txt = update.getMessage().getText();
                if ("/start".equalsIgnoreCase(txt) || txt.startsWith("/start ")) {
                    long chatId = update.getMessage().getChatId();

                    if (txt.startsWith("/start ") && txt.length() > 7) {
                        String idStr = txt.substring(7).trim();
                        try {
                            long eventId = Long.parseLong(idStr);
                            handleStartWithEvent(chatId, eventId, update);
                            return;
                        } catch (NumberFormatException ignore) { /* no-op */ }
                    }

                    // Базове привітання
                    safeSend(String.valueOf(chatId),
                            "Привіт! Надішліть /start <eventId>, щоб керувати підпискою на івент.",
                            null);
                }
            } else if (update.hasCallbackQuery()) {
                CallbackQuery cb = update.getCallbackQuery();
                String data = cb.getData();
                long chatId = cb.getMessage().getChatId();

                // Синхронізація акаунта користувача
                UserTelegram tgAcc = provisioner.ensure(cb.getFrom());

                if (data != null && data.startsWith("EVT_SUB:")) {
                    long eventId = parseId(data, "EVT_SUB:");
                    boolean nowActive = eventSubscriptionService.toggleSubscription(eventId, tgAcc, true);

                    Event event = events.findById(eventId).orElse(null);
                    InlineKeyboardMarkup kb = eventKeyboard(eventId, nowActive, resolveEventLinkUrl(event));
                    safeSend(String.valueOf(chatId), "Підписка активована ✅", kb);
                    ack(cb, "Підписка активована");

                } else if (data != null && data.startsWith("EVT_UNSUB:")) {
                    long eventId = parseId(data, "EVT_UNSUB:");
                    boolean nowActive = eventSubscriptionService.toggleSubscription(eventId, tgAcc, false);

                    Event event = events.findById(eventId).orElse(null);
                    InlineKeyboardMarkup kb = eventKeyboard(eventId, nowActive, resolveEventLinkUrl(event));
                    safeSend(String.valueOf(chatId), "Підписка вимкнена ❌", kb);
                    ack(cb, "Відписка виконана");

                } else {
                    ack(cb, "Невідома дія");
                }
            }
        } catch (Exception e) {
            log.error("onUpdateReceived failed", e);
        }
    }

    /* ============================ Helpers ============================ */

    private void safeSend(String chatId, String text, InlineKeyboardMarkup kb) {
        try {
            sendMessage(chatId, text, kb);
        } catch (TelegramApiException e) {
            log.error("sendMessage failed: {}", e.getMessage(), e);
        }
    }

    private void handleStartWithEvent(long chatId, long eventId, Update update) {
        UserTelegram tgAcc = provisioner.ensure(update.getMessage().getFrom());
        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            safeSend(String.valueOf(chatId), "Івент не знайдено.", null);
            return;
        }
        boolean isSubscribed = subs.existsByEventAndUserTelegramAndMessengerAndActiveIsTrue(
                event, tgAcc, Messenger.TELEGRAM);

        String link = resolveEventLinkUrl(event);
        InlineKeyboardMarkup kb = eventKeyboard(eventId, isSubscribed, link);
        String text = isSubscribed
                ? "Ви вже підписані на нагадування про цей івент."
                : "Ви не підписані на цей івент. Натисніть кнопку нижче, щоб підписатися.";
        safeSend(String.valueOf(chatId), text, kb);
    }

    private void ack(CallbackQuery cb, String text) throws TelegramApiException {
        AnswerCallbackQuery ack = AnswerCallbackQuery.builder()
                .callbackQueryId(cb.getId())
                .text(text)
                .showAlert(false)
                .build();
        execute(ack);
    }

    private String buildStartDeepLink(long eventId) {
        // напр.: https://t.me/<botUsername>?start=<eventId>
        return "https://t.me/" + botUsername + "?start=" + eventId;
    }

    /** URL для кнопки "Посилання": спочатку Event.getUrl(), якщо нема — Event.getCoverUrl(). */
    String resolveEventLinkUrl(Event e) {
        if (e == null) return null;

        try {
            var m1 = e.getClass().getMethod("getUrl");
            Object v1 = m1.invoke(e);
            if (v1 instanceof String s1 && s1 != null && !s1.isBlank()) return s1.trim();
        } catch (Exception ex) { /* ок, пробуємо coverUrl */ }

        try {
            var m2 = e.getClass().getMethod("getCoverUrl");
            Object v2 = m2.invoke(e);
            if (v2 instanceof String s2 && s2 != null && !s2.isBlank()) return s2.trim();
        } catch (Exception ex) { /* no-op */ }

        return null;
    }

    private long parseId(String data, String prefix) {
        return Long.parseLong(data.substring(prefix.length()));
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }
}
