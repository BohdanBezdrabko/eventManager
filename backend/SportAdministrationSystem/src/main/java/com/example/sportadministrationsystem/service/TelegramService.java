package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Role;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.repository.UserTelegramRepository;
import com.example.sportadministrationsystem.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramService extends TelegramLongPollingBot {

    // === Config ===
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    // === Deps ===
    private final UserRepository users;
    private final UserTelegramRepository userTelegrams;
    private final UserRegistrationService registrations;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // === Bot identity ===
    @Override public String getBotUsername() { return botUsername; }
    @Override public String getBotToken() { return botToken; }

    // === Public helpers (used by PostDispatchService) ===
    /** Звичайне повідомлення без клавіатури. */
    public void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        execute(msg);
    }

    /** Повідомлення з inline-клавіатурою. */
    public void sendMessage(String chatId, String text, InlineKeyboardMarkup kb) throws TelegramApiException {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setReplyMarkup(kb);
        execute(msg);
    }

    /** Клавіатура для події. */
    public InlineKeyboardMarkup eventKeyboard(Long eventId, boolean registered) {
        InlineKeyboardButton toggle = new InlineKeyboardButton();
        toggle.setText(registered ? "Скасувати реєстрацію" : "Записатись");
        toggle.setCallbackData((registered ? "unreg:" : "reg:") + eventId);

        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(java.util.List.of(java.util.List.of(toggle)));
        return kb;
    }

    /** Після успіху реєстрації/скасування міняємо кнопку. */
    private void swapKeyboard(Long chatId, Integer messageId, Long eventId, boolean registered) {
        try {
            EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setReplyMarkup(eventKeyboard(eventId, registered));
            execute(edit);
        } catch (TelegramApiException e) {
            log.warn("swapKeyboard failed: {}", e.getMessage());
        }
    }

    private void answerToast(String callbackId, String text) {
        try {
            AnswerCallbackQuery a = new AnswerCallbackQuery();
            a.setCallbackQueryId(callbackId);
            if (text != null && !text.isBlank()) a.setText(text);
            a.setShowAlert(false);
            execute(a);
        } catch (TelegramApiException e) {
            log.warn("answerToast failed: {}", e.getMessage());
        }
    }

    private void safeReply(Long chatId, String text) {
        try { sendMessage(String.valueOf(chatId), text); } catch (TelegramApiException ignored) {}
    }

    // === Update dispatcher ===
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update == null) return;

            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
                return;
            }

            if (update.hasMessage() && update.getMessage().hasText()) {
                handleText(update.getMessage());
            }
        } catch (Exception e) {
            log.error("onUpdateReceived error", e);
        }
    }

    // === Text handlers ===
    private void handleText(Message msg) {
        String text = msg.getText().trim();
        Long tgUserId = msg.getFrom().getId();
        Long tgChatId = msg.getChatId();

        if (text.startsWith("/start")) {
            // /start  OR  /start <jwt>
            String[] parts = text.split("\\s+", 2);
            String token = parts.length > 1 ? parts[1] : null;
            if (token != null && !token.isBlank()) {
                // Лінкуємо до ВЖЕ існуючого юзера з веб-кабінету
                try {
                    String username = jwtTokenProvider.getUsername(token);
                    User u = users.findByUsername(username)
                            .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено"));
                    linkTelegramIfMissing(u, tgUserId, tgChatId);
                    safeReply(tgChatId, "Telegram підключено до акаунта " + username + " ✅");
                } catch (Exception e) {
                    safeReply(tgChatId, "Не вдалось підключити: " + e.getMessage());
                }
            } else {
                // Нема токена — просто створюємо Telegram-юзера (тіньовий акаунт)
                Long userId = ensureUserForTelegram(msg.getFrom(), tgChatId);
                safeReply(tgChatId, "Вітаю! Ваш Telegram прив’язано до акаунта #" + userId + " ✅");
            }
            return;
        }

        if (text.startsWith("/unlink")) {
            userTelegrams.findByTgUserId(tgUserId).ifPresent(ut -> userTelegrams.deleteById(ut.getId()));
            safeReply(tgChatId, "Зв’язок Telegram відключено.");
            return;
        }

        // (Опційно) ручні команди
        if (text.startsWith("/reg ")) {
            Long eventId = parseId(text.substring(5));
            Long userId = ensureUserForTelegram(msg.getFrom(), tgChatId);
            try {
                registrations.register(userId, eventId);
                safeReply(tgChatId, "Готово! Ви зареєстровані ✅");
            } catch (Exception e) {
                safeReply(tgChatId, e.getMessage());
            }
        } else if (text.startsWith("/unreg ")) {
            Long eventId = parseId(text.substring(7));
            Long userId = ensureUserForTelegram(msg.getFrom(), tgChatId);
            try {
                registrations.cancel(userId, eventId);
                safeReply(tgChatId, "Скасовано ❌");
            } catch (Exception e) {
                safeReply(tgChatId, e.getMessage());
            }
        }
    }

    // === Callback handler (кнопки) ===
    private void handleCallback(CallbackQuery cq) {
        String data = cq.getData();
        Long tgUserId = cq.getFrom().getId();
        Long chatId = cq.getMessage().getChatId();
        Integer messageId = cq.getMessage().getMessageId();

        if (data == null) { answerToast(cq.getId(), "Невірні дані"); return; }

        boolean wantReg = data.startsWith("reg:");
        boolean wantUnreg = data.startsWith("unreg:");
        if (!wantReg && !wantUnreg) { answerToast(cq.getId(), "Невідома дія"); return; }

        Long eventId = parseId(data.substring(data.indexOf(':') + 1));

        // Гарантовано маємо локального User (створимо, якщо треба)
        Long userId = userTelegrams.findByTgUserId(tgUserId)
                .map(ut -> ut.getUser().getId())
                .orElseGet(() -> ensureUserForTelegram(cq.getFrom(), chatId));

        try {
            if (wantReg) {
                registrations.register(userId, eventId);
                answerToast(cq.getId(), "Готово! Ви зареєстровані ✅");
                swapKeyboard(chatId, messageId, eventId, true);
            } else {
                registrations.cancel(userId, eventId);
                answerToast(cq.getId(), "Скасовано ❌");
                swapKeyboard(chatId, messageId, eventId, false);
            }
        } catch (IllegalArgumentException ex) {
            answerToast(cq.getId(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Callback error", ex);
            answerToast(cq.getId(), "Сталася помилка. Спробуйте пізніше.");
        }
    }

    // === Linking & provisioning ===
    /** Якщо для tgUser ще немає локального User — створюємо ROLE_USER і лінкуємо у user_telegram. */
    private Long ensureUserForTelegram(org.telegram.telegrambots.meta.api.objects.User from, Long tgChatId) {
        Long tgUserId = from.getId();
        return userTelegrams.findByTgUserId(tgUserId)
                .map(ut -> ut.getUser().getId())
                .orElseGet(() -> {
                    // 1) Створюємо нового User
                    String baseUsername = proposeUsername(from);
                    String unique = ensureUniqueUsername(baseUsername);
                    String randomPass = randomPassword();
                    User u = User.builder()
                            .username(unique)
                            .password(passwordEncoder.encode(randomPass))
                            .roles(Set.of(Role.ROLE_USER))
                            .build();
                    u = users.save(u);

                    // 2) Лінкуємо у user_telegram
                    UserTelegram ut = UserTelegram.builder()
                            .user(u)
                            .tgUserId(tgUserId)
                            .tgChatId(tgChatId)
                            .linkedAt(LocalDateTime.now())
                            .build();
                    userTelegrams.save(ut);
                    return u.getId();
                });
    }

    /** Лінкує існуючого User, якщо запису в user_telegram ще немає. */
    private void linkTelegramIfMissing(User u, Long tgUserId, Long tgChatId) {
        userTelegrams.findByUser(u).ifPresentOrElse(
                existing -> { /* вже лінковано — нічого */ },
                () -> userTelegrams.save(UserTelegram.builder()
                        .user(u)
                        .tgUserId(tgUserId)
                        .tgChatId(tgChatId)
                        .linkedAt(LocalDateTime.now())
                        .build())
        );
    }

    private String proposeUsername(org.telegram.telegrambots.meta.api.objects.User tgu) {
        // У TelegramBots API метод називається getUserName() (з великою N)
        if (tgu.getUserName() != null && !tgu.getUserName().isBlank()) {
            return tgu.getUserName().trim();
        }
        String fn = Optional.ofNullable(tgu.getFirstName()).orElse("tg");
        return ("tg_" + fn + "_" + tgu.getId()).toLowerCase();
    }

    private String ensureUniqueUsername(String base) {
        String u = base;
        int i = 1;
        while (users.findByUsername(u).isPresent()) {
            u = base + "_" + i++;
        }
        return u;
    }

    private static final SecureRandom RNG = new SecureRandom();
    private String randomPassword() {
        // 24 символи [A-Za-z0-9]
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    private Long parseId(String s) {
        return Long.parseLong(s.replaceAll("[^0-9]", ""));
    }
}
