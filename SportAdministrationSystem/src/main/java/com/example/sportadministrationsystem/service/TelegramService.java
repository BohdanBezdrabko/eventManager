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
    private final EventSubscriptionService eventSubscriptionService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    /* ============================ ПУБЛІЧНИЙ API ============================ */

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup kb) throws TelegramApiException {
        SendMessage msg = new SendMessage(chatId, text);
        if (kb != null) msg.setReplyMarkup(kb);
        execute(msg);
    }

    /** Приватні (direct) кнопки з callback — підпис/відпис. */
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

    /** ПЕРШИЙ пост у каналі: URL-кнопка «Підписатися» (deep-link у бота). */
    public InlineKeyboardMarkup eventKeyboardPublicFirst(long eventId, String linkUrl) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton subscribe = new InlineKeyboardButton();
        subscribe.setText("Підписатися");
        subscribe.setUrl(buildStartDeepLink(eventId)); // лише deep-link
        rows.add(List.of(subscribe));

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

    /** Наступні пости у каналі: URL-кнопка «Керувати підпискою» (deep-link у бота). */
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
            // deep-link: /start <eventId> або /start <eventId>:<postId>
            if (update.hasMessage() && update.getMessage().hasText()) {
                String txt = update.getMessage().getText();
                if ("/start".equalsIgnoreCase(txt) || txt.startsWith("/start ")) {
                    long chatId = update.getMessage().getChatId();

                    if (txt.startsWith("/start ") && txt.length() > 7) {
                        String idStr = txt.substring(7).trim();
                        try {
                            // Формат: "123" або "123:456" (eventId:postId)
                            if (idStr.contains(":")) {
                                String[] parts = idStr.split(":");
                                long eventId = Long.parseLong(parts[0]);
                                long postId = Long.parseLong(parts[1]);
                                handleStartWithPostId(chatId, eventId, postId, update);
                            } else {
                                long eventId = Long.parseLong(idStr);
                                handleStartWithEvent(chatId, eventId, update);
                            }
                            return;
                        } catch (NumberFormatException ignore) { /* no-op */ }
                    }

                    safeSend(String.valueOf(chatId),
                            "📌 Команда: /start eventId\n\nПриклад: /start 12\n\nЩоб дізнатися номер eventi, перейдіть на наш сайт і оберіть івент.",
                            null);
                }
            }
            // callback (працює в основному у приваті; у каналі відповіді не шлемо)
            else if (update.hasCallbackQuery()) {
                CallbackQuery cb = update.getCallbackQuery();
                String data = cb.getData();
                long chatId = cb.getMessage().getChatId();
                boolean fromChannel = cb.getMessage() != null && cb.getMessage().isChannelMessage();

                UserTelegram tgAcc = provisioner.ensure(cb.getFrom());

                if (data != null && data.startsWith("EVT_SUB:")) {
                    long eventId = parseId(data, "EVT_SUB:");
                    boolean nowActive = eventSubscriptionService.toggleSubscription(eventId, tgAcc, true);

                    Event event = events.findById(eventId).orElse(null);
                    InlineKeyboardMarkup kb = eventKeyboard(eventId, nowActive, resolveEventLinkUrl(event));

                    if (!fromChannel) {
                        String eventName = event != null ? event.getName() : "Івент #" + eventId;
                        safeSend(String.valueOf(chatId), "✅ Ви успішно підписались на *" + eventName + "*\n\nЧекайте оновлення! 🎉", kb);
                    }
                    ack(cb, "✅ Підписка активована");

                } else if (data != null && data.startsWith("EVT_UNSUB:")) {
                    long eventId = parseId(data, "EVT_UNSUB:");
                    boolean nowActive = eventSubscriptionService.toggleSubscription(eventId, tgAcc, false);

                    Event event = events.findById(eventId).orElse(null);
                    InlineKeyboardMarkup kb = eventKeyboard(eventId, nowActive, resolveEventLinkUrl(event));

                    if (!fromChannel) {
                        String eventName = event != null ? event.getName() : "Івент #" + eventId;
                        safeSend(String.valueOf(chatId), "❌ Ви відписались від *" + eventName + "*", kb);
                    }
                    ack(cb, "❌ Відписка виконана");

                } else {
                    ack(cb, "⚠️ Невідома дія");
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

        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();
        String eventDate = event.getStartAt() != null ?
            new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(event.getStartAt()) :
            "Дата невідома";

        String text = "📅 *" + eventName + "*\n" +
                "🕐 " + eventDate + "\n" +
                (event.getLocation() != null && !event.getLocation().isBlank() ? "📍 " + event.getLocation() + "\n" : "") +
                "\n" +
                (isSubscribed
                        ? "✅ Ви вже підписані. Отримуватимете оновлення про цей івент."
                        : "🔔 Натисніть кнопку нижче, щоб отримувати оновлення.");
        safeSend(String.valueOf(chatId), text, kb);
    }

    private void handleStartWithPostId(long chatId, long eventId, long postId, Update update) {
        try {
            UserTelegram tgAcc = provisioner.ensure(update.getMessage().getFrom());

            Event event = events.findById(eventId).orElse(null);
            if (event == null) {
                safeSend(String.valueOf(chatId), "❌ Івент #" + eventId + " не знайдено.", null);
                return;
            }

            // Для постів - показуємо повну інформацію про пост + івент
            boolean isSubscribed = subs.existsByEventAndUserTelegramAndMessengerAndActiveIsTrue(
                    event, tgAcc, Messenger.TELEGRAM);

            String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();
            String eventDate = event.getStartAt() != null ?
                new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(event.getStartAt()) :
                "Дата невідома";

            String text = "📬 *" + eventName + "*\n" +
                    "🕐 " + eventDate + "\n" +
                    (event.getLocation() != null && !event.getLocation().isBlank() ? "📍 " + event.getLocation() + "\n" : "") +
                    "\n" +
                    "🔔 Подія #" + postId + "\n" +
                    "\n" +
                    (isSubscribed
                            ? "✅ Ви вже підписані на оновлення цього івенту."
                            : "Натисніть кнопку, щоб отримувати оновлення.");

            String link = resolveEventLinkUrl(event);
            InlineKeyboardMarkup kb = eventKeyboard(eventId, isSubscribed, link);

            safeSend(String.valueOf(chatId), text, kb);
        } catch (Exception e) {
            log.error("handleStartWithPostId failed: {}", e.getMessage(), e);
            safeSend(String.valueOf(chatId), "⚠️ Помилка обробки. Спробуйте ще раз.", null);
        }
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
        return "https://t.me/" + botUsername + "?start=" + eventId;
    }

    /** URL для кнопки "Посилання": спочатку Event.getUrl(), якщо нема — Event.getCoverUrl(). */
    String resolveEventLinkUrl(Event e) {
        if (e == null) return null;

        try {
            var m1 = e.getClass().getMethod("getUrl");
            Object v1 = m1.invoke(e);
            if (v1 instanceof String s1 && s1 != null && !s1.isBlank()) return s1.trim();
        } catch (Exception ignore) {}

        try {
            var m2 = e.getClass().getMethod("getCoverUrl");
            Object v2 = m2.invoke(e);
            if (v2 instanceof String s2 && s2 != null && !s2.isBlank()) return s2.trim();
        } catch (Exception ignore) {}

        return null;
    }

    private long parseId(String data, String prefix) {
        return Long.parseLong(data.substring(prefix.length()));
    }

    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }
}
