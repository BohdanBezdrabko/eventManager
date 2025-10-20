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
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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

    /** Автопровізія “тіньового” акаунта при першому кліку */
    private final TelegramAccountProvisioner accountProvisioner;

    @Value("${telegram.bot.username}")
    private String username;

    @Value("${telegram.bot.token}")
    private String token;

    private static final String CB_REG_PREFIX   = "EVT_REG";
    private static final String CB_UNREG_PREFIX = "EVT_UNREG";

    /* ===================== TelegramLongPollingBot ===================== */

    @Override
    public String getBotUsername() { return username; }

    @Override
    public String getBotToken() { return token; }

    @Override
    public void onRegister() {
        try {
            SetMyCommands set = new SetMyCommands();
            set.setCommands(List.of(
                    new BotCommand("start", "Почати")
            ));
            // Якщо у твоїй версії підтримується scope — залиш; якщо ні, просто прибери наступний рядок:
            set.setScope(new BotCommandScopeDefault());

            execute(set);
        } catch (TelegramApiException ignore) {
            // no-op
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleText(update);
                return;
            }
            if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            log.error("Update handling error", e);
        }
    }

    /* =========================== Messages ============================ */

    private void handleText(Update update) throws TelegramApiException {
        var msg = update.getMessage();
        var chatId = String.valueOf(msg.getChatId());
        String text = msg.getText();

        if (text != null && text.startsWith("/start")) {
            // тут можна розпарсити deep-link, якщо захочеш ("/start <jwt>")
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("👋 Привіт! Я допомагаю реєструватися на події. " +
                            "Просто тисни кнопку під постом у каналі.")
                    .build());
        }
    }

    /* ========================= Callback query ======================== */

    @Transactional
    protected void handleCallback(Update update) {
        var cq = update.getCallbackQuery();
        String data = cq.getData() == null ? "" : cq.getData();

        if (data.startsWith(CB_REG_PREFIX + ":")) {
            Long eventId = parseEventId(data);
            if (eventId == null) {
                answerCallback(cq.getId(), "Невалідні дані кнопки.");
                return;
            }
            registerForEvent(update, eventId);
            answerCallback(cq.getId(), "Вас зареєстровано ✅");
            tryToggleKeyboard(update, eventId, true);
            return;
        }

        if (data.startsWith(CB_UNREG_PREFIX + ":")) {
            Long eventId = parseEventId(data);
            if (eventId == null) {
                answerCallback(cq.getId(), "Невалідні дані кнопки.");
                return;
            }
            unregisterFromEvent(update, eventId);
            answerCallback(cq.getId(), "Підписку вимкнено ❌");
            tryToggleKeyboard(update, eventId, false);
            return;
        }

        answerCallback(cq.getId(), "Невідома дія.");
    }

    private void answerCallback(String callbackId, String text) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback: {}", e.getMessage());
        }
    }

    private void tryToggleKeyboard(Update update, Long eventId, boolean registered) {
        try {
            var cq = update.getCallbackQuery();
            var msg = cq.getMessage();
            execute(EditMessageReplyMarkup.builder()
                    .chatId(String.valueOf(msg.getChatId()))
                    .messageId(msg.getMessageId())
                    .replyMarkup(eventKeyboard(eventId, registered))
                    .build());
        } catch (Exception e) {
            // не критично
            log.debug("Keyboard toggle failed: {}", e.getMessage());
        }
    }

    private Long parseEventId(String data) {
        // формат: EVT_REG:123 або EVT_UNREG:123
        String[] parts = data.split(":");
        if (parts.length != 2) return null;
        try { return Long.valueOf(parts[1]); } catch (NumberFormatException e) { return null; }
    }

    /* ====================== Domain actions (TX) ====================== */

    @Transactional
    protected void registerForEvent(Update update, Long eventId) {
        var cq = update.getCallbackQuery();
        var from = cq.getFrom();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new DomainException("Подію не знайдено."));

        // 1) Якщо юзер ще не зв’язаний — створюємо “тіньовий”
        User user = resolveLinkedUser(from.getId())
                .orElseGet(() -> accountProvisioner.provisionShadow(
                        from.getId(), from.getUserName(), from.getFirstName(), from.getLastName()
                ));

        // 2) Підписка в event_subscriptions (active=true)
        Optional<EventSubscription> existing =
                subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM);

        if (existing.isPresent()) {
            EventSubscription es = existing.get();
            if (!es.isActive()) {
                es.setActive(true);
                subscriptionRepository.save(es);
            }
        } else {
            EventSubscription es = new EventSubscription();
            es.setEvent(event);
            es.setUser(user);
            es.setMessenger(Messenger.TELEGRAM);
            es.setActive(true);
            subscriptionRepository.save(es);
        }

        // 3) Реєстрація в user_events (щоб тригери підняли registered_count)
        if (!registrationRepository.existsByEventAndUser(event, user)) {
            registrationRepository.create(event, user);
        }
    }

    @Transactional
    protected void unregisterFromEvent(Update update, Long eventId) {
        var cq = update.getCallbackQuery();
        var from = cq.getFrom();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new DomainException("Подію не знайдено."));

        User user = resolveLinkedUser(from.getId())
                .orElseThrow(() -> new DomainException("Спочатку зареєструйтесь на подію."));

        // 1) Деактивуємо підписку
        subscriptionRepository.findByEventAndUserAndMessenger(event, user, Messenger.TELEGRAM)
                .ifPresent(es -> {
                    if (es.isActive()) {
                        es.setActive(false);
                        subscriptionRepository.save(es);
                    }
                });

        // 2) Видаляємо рядок з user_events (тригери зменшать registered_count)
        registrationRepository.deleteByEventAndUser(event, user);
    }

    private Optional<User> resolveLinkedUser(Long tgUserId) {
        return userTelegramRepository.findByTgUserId(tgUserId)
                .map(UserTelegram::getUser);
    }

    /* =========================== Keyboard ============================ */

    public InlineKeyboardMarkup eventKeyboard(Long eventId, boolean registered) {
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

    /* ===================== Facade for other services ================= */

    public void sendMessage(String chatId, String text) throws TelegramApiException {
        execute(SendMessage.builder().chatId(chatId).text(text).build());
    }

    public void sendMessage(String chatId, String text, InlineKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        if (keyboard != null) msg.setReplyMarkup(keyboard);
        execute(msg);
    }

    /* ============================== Misc ============================= */

    private static class DomainException extends RuntimeException {
        public DomainException(String message) { super(message); }
    }
}
