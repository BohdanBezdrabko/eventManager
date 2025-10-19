package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.RegistrationRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TelegramBots
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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

    private final EventRepository eventRepository;
    private final EventSubscriptionRepository subscriptionRepository;
    private final RegistrationRepository registrationRepository;
    private final UserTelegramRepository userTelegramRepository;

    /** автостворення “тіньового” акаунта при першому кліку */
    private final TelegramAccountProvisioner accountProvisioner;

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.bot.token}")
    private String token;

    private static final String CB_REG_PREFIX = "EVT_REG";
    private static final String CB_UNREG_PREFIX = "EVT_UNREG";

    /* ======== TelegramLongPollingBot ======== */

    @Override public String getBotUsername() { return username; }
    @Override public String getBotToken() { return token; }

    @Override
    public void onUpdateReceived(Update update) {
        handleUpdate(update);
    }

    /* ======== Inbound ======== */

    public void handleUpdate(Update update) {
        if (update == null) return;
        if (!update.hasCallbackQuery()) return;

        var cq = update.getCallbackQuery();
        String data = cq.getData() == null ? "" : cq.getData();

        try {
            Long eventId = parseEventId(data);
            if (eventId == null) {
                answerCallback(cq.getId(), "Невірні дані кнопки");
                return;
            }

            boolean unregister = data.startsWith(CB_UNREG_PREFIX);
            if (unregister) {
                unregisterFromEvent(update, eventId);
                answerCallback(cq.getId(), "Підписку вимкнено ❌");
            } else {
                registerForEvent(update, eventId);
                answerCallback(cq.getId(), "Вас зареєстровано ✅");
            }

            tryToggleKeyboard(update, eventId, !unregister);

        } catch (DomainException ex) {
            log.warn("Domain error: {}", ex.getMessage());
            answerCallback(cq.getId(), "⚠️ " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Callback handling failed", ex);
            answerCallback(cq.getId(), "❗ Сталася помилка. Спробуйте пізніше.");
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            AnswerCallbackQuery ans = new AnswerCallbackQuery();
            ans.setCallbackQueryId(callbackId);
            ans.setText(text);
            ans.setShowAlert(false);
            ans.setCacheTime(1);
            execute(ans);
        } catch (TelegramApiException e) {
            log.debug("answerCallback failed: {}", e.getMessage());
        }
    }

    private Long parseEventId(String data) {
        if (data == null) return null;
        String[] parts = data.split(":");
        if (parts.length != 2) return null;
        try { return Long.valueOf(parts[1]); } catch (NumberFormatException e) { return null; }
    }

    @Transactional
    protected void registerForEvent(Update update, Long eventId) {
        var cq = update.getCallbackQuery();
        var from = cq.getFrom();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new DomainException("Подію не знайдено."));

        // авто-провізія “тіньового” користувача
        User user = resolveLinkedUser(from.getId())
                .orElseGet(() -> accountProvisioner.provisionShadow(
                        from.getId(),
                        from.getUserName(),
                        from.getFirstName(),
                        from.getLastName()
                ));

        if (subscriptionRepository.existsByEventAndUserAndMessengerAndActiveIsTrue(
                event, user, Messenger.TELEGRAM)) {
            ensureUserRegistration(event, user);
            return;
        }

        EventSubscription sub = subscriptionRepository
                .findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .orElseGet(() -> EventSubscription.builder()
                        .event(event)
                        .user(user)
                        .messenger(Messenger.TELEGRAM)
                        .active(true)
                        .build());
        sub.setActive(true);
        subscriptionRepository.save(sub);

        ensureUserRegistration(event, user);
    }

    @Transactional
    protected void unregisterFromEvent(Update update, Long eventId) {
        var cq = update.getCallbackQuery();
        var from = cq.getFrom();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new DomainException("Подію не знайдено."));

        User user = resolveLinkedUser(from.getId())
                .orElseThrow(() -> new DomainException("Зв’язок Telegram з профілем не знайдено."));

        subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .ifPresent(es -> { es.setActive(false); subscriptionRepository.save(es); });
    }

    private Optional<User> resolveLinkedUser(Long tgUserId) {
        return userTelegramRepository.findByTgUserId(tgUserId)
                .map(UserTelegram::getUser);
    }

    private void ensureUserRegistration(Event e, User u) {
        // якщо у твоєму RegistrationRepository є existsByEventAndUser — використовуй його
        try {
            var m = registrationRepository.getClass().getMethod("existsByEventAndUser", Event.class, User.class);
            boolean exists = (boolean) m.invoke(registrationRepository, e, u);
            if (!exists) registrationRepository.create(e, u);
        } catch (Exception ignore) {
            // якщо метода немає — пропускаємо
        }
    }

    private void tryToggleKeyboard(Update update, Long eventId, boolean nowRegistered) {
        try {
            Message msg = update.getCallbackQuery().getMessage();
            Long chatId = (msg.getChat() != null) ? msg.getChat().getId() : null;
            if (chatId == null) return;

            InlineKeyboardMarkup markup = buildSingleActionKeyboard(eventId, nowRegistered);

            EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(msg.getMessageId());
            edit.setReplyMarkup(markup);

            execute(edit);
        } catch (TelegramApiException e) {
            log.debug("Keyboard toggle skipped: {}", e.getMessage());
        }
    }

    /* ===== клавіатура ===== */

    private InlineKeyboardMarkup buildSingleActionKeyboard(Long eventId, boolean registered) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        if (registered) {
            btn.setText("❌ Скасувати реєстрацію");
            btn.setCallbackData(CB_UNREG_PREFIX + ":" + eventId);
        } else {
            btn.setText("✅ Зареєструватися");
            btn.setCallbackData(CB_REG_PREFIX + ":" + eventId);
        }
        row.add(btn);
        rows.add(row);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /* ===== фасади для інших сервісів ===== */

    public InlineKeyboardMarkup eventKeyboard(Long eventId, boolean registered) {
        return buildSingleActionKeyboard(eventId, registered);
    }

    public void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        execute(msg);
    }

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        if (keyboard != null) msg.setReplyMarkup(keyboard);
        execute(msg);
    }

    private static class DomainException extends RuntimeException {
        public DomainException(String message) { super(message); }
    }
}
