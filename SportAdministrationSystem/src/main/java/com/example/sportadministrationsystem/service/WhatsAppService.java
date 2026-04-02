package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.ProcessedWhatsAppMessage;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
import com.example.sportadministrationsystem.repository.ProcessedWhatsAppMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private static final Pattern START_PATTERN =
            Pattern.compile("^\\s*start\\s+([\\d:]+)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    // НОВІ КОМАНДИ ДЛЯ ГРУП
    private static final Pattern HELP_PATTERN =
            Pattern.compile("^\\s*(help|допомога|/help|/допомога)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern EVENTS_PATTERN =
            Pattern.compile("^\\s*(events|события|/events)\\s*$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper om = new ObjectMapper();

    private final EventRepository events;
    private final EventSubscriptionWhatsappRepository subs;
    private final ProcessedWhatsAppMessageRepository processedMessageRepository;
    private final WhatsAppAccountProvisioner provisioner;
    private final EventSubscriptionWhatsAppService subscriptionService;
    private final WhatsAppGraphClient graph;

    @Value("${whatsapp.business-phone-e164:}")
    private String businessPhoneE164;

    @Value("${whatsapp.groups.enabled:false}")
    private boolean groupsEnabled;

    public void handleWebhook(String bodyJson) {
        try {
            JsonNode root = om.readTree(bodyJson);
            JsonNode entry = root.path("entry");
            if (!entry.isArray()) return;

            for (JsonNode e : entry) {
                JsonNode changes = e.path("changes");
                if (!changes.isArray()) continue;

                for (JsonNode ch : changes) {
                    JsonNode value = ch.path("value");

                    String profileName = null;
                    JsonNode contacts = value.path("contacts");
                    if (contacts.isArray() && contacts.size() > 0) {
                        profileName = contacts.get(0).path("profile").path("name").asText(null);
                    }

                    JsonNode messages = value.path("messages");
                    if (!messages.isArray()) continue;

                    for (JsonNode msg : messages) {
                        String from = msg.path("from").asText(null);
                        if (from == null || from.isBlank()) continue;

                        // Отримуємо message_id для дедупліки та логування
                        String messageId = msg.path("id").asText(null);
                        if (messageId != null && !messageId.isBlank()) {
                            try {
                                ProcessedWhatsAppMessage processed = new ProcessedWhatsAppMessage();
                                processed.setMessageId(messageId);
                                processed.setWaId(from);
                                processed.setWebhookTimestamp(LocalDateTime.now());
                                processedMessageRepository.save(processed);
                                log.debug("Recorded processed message_id={}", messageId);
                            } catch (Exception ex) {
                                log.warn("Failed to log processed message: {}", ex.getMessage());
                            }
                        }

                        // НОВЕ: отримуємо group_id якщо це групове повідомлення
                        String groupId = msg.path("group_id").asText(null);

                        UserWhatsapp waAcc = provisioner.ensure(from, profileName);

                        String type = msg.path("type").asText("");
                        if ("text".equals(type)) {
                            String text = msg.path("text").path("body").asText("");
                            handleText(from, waAcc, text, groupId);
                        } else if ("interactive".equals(type)) {
                            String id = extractInteractiveId(msg);
                            if (id != null) handleAction(from, waAcc, id, groupId);
                        } else if ("button".equals(type)) {
                            String id = msg.path("button").path("payload").asText(null);
                            if (id != null) handleAction(from, waAcc, id, groupId);
                        }
                    }
                }
            }


        } catch (Exception e) {
            log.error("handleWebhook failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Асинхронна обробка webhook (викликається з контролера).
     * Дозволяє швидко повернути 200 OK користувачу без затримок.
     */
    @Async
    public void handleWebhookAsync(String bodyJson) {
        try {
            log.debug("Processing WhatsApp webhook asynchronously");
            handleWebhook(bodyJson);
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook asynchronously: {}", e.getMessage(), e);
        }
    }

    /**
     * Обробка текстових повідомлень з підтримкою нових команд
     */
    private void handleText(String toWaId, UserWhatsapp waAcc, String text, String groupId) {
        String displaySource = groupId != null ? "група " + groupId : "особистий чат";
        log.info("Отримане повідомлення від {} в {}: {}", toWaId, displaySource, text);

        // Перевіряємо нові команди
        if (HELP_PATTERN.matcher(text).matches()) {
            sendHelpMessage(toWaId);
            return;
        }

        if (EVENTS_PATTERN.matcher(text).matches()) {
            sendEventsList(toWaId);
            return;
        }

        // START команда
        Matcher m = START_PATTERN.matcher(text == null ? "" : text);
        if (!m.matches()) {
            graph.sendText(toWaId,
                "📌 *Команди:*\n\n" +
                "*START <номер-івенту>*\n" +
                "Приклад: START 12\n\n" +
                "*HELP* - довідка\n" +
                "*EVENTS* - список подій\n\n" +
                "⚠️ Щоб дізнатися номер, перейдіть на наш сайт і оберіть івент."
            );
            return;
        }

        String idStr = m.group(1); // "123" або "123:456"

        try {
            if (idStr.contains(":")) {
                // Формат: "123:456" (eventId:postId)
                String[] parts = idStr.split(":");
                long eventId = Long.parseLong(parts[0]);
                long postId = Long.parseLong(parts[1]);
                handleTextWithPostId(toWaId, waAcc, eventId, postId, groupId);
            } else {
                // Формат: "123" (тільки eventId)
                long eventId = Long.parseLong(idStr);
                handleTextWithEventId(toWaId, waAcc, eventId, groupId);
            }
        } catch (NumberFormatException e) {
            graph.sendText(toWaId, "❌ Невірний формат. Використовуйте: START 123 або START 123:456");
        }
    }

    /**
     * НОВЕ: Відправка довідки
     */
    private void sendHelpMessage(String toWaId) {
        String help =
            "🤖 *Доступні команди:*\n\n" +
            "*START <номер-івенту>*\n" +
            "Приклад: START 6\n" +
            "Показує інформацію про подію\n\n" +
            "*EVENTS*\n" +
            "Список доступних подій\n\n" +
            "*HELP*\n" +
            "Ця довідка\n\n" +
            "💡 *Як користуватися в групі:*\n" +
            "1. Додайте бота в групу\n" +
            "2. Напишіть START <номер>\n" +
            "3. Натисніть кнопку \"Підписатися\"\n" +
            "4. Отримуйте оновлення в групу!";

        graph.sendText(toWaId, help);
    }

    /**
     * НОВЕ: Відправка списку подій
     */
    private void sendEventsList(String toWaId) {
        try {
            // Отримуємо тільки майбутні івенти (startAt > now), відсортовані за startAt
            List<Event> eventList = events.findUpcomingEvents(LocalDateTime.now());

            if (eventList.isEmpty()) {
                graph.sendText(toWaId, "📭 На даний момент немає доступних подій");
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📅 *Доступні події:*\n\n");

            int count = 0;
            for (Event e : eventList) {
                if (count >= 10) { // Обмежуємо до 10 подій
                    sb.append("\n... та ще більше на сайті");
                    break;
                }
                sb.append(String.format("• #%d - *%s*\n", e.getId(), e.getName() != null ? e.getName() : "Без назви"));
                count++;
            }

            sb.append("\n💡 Напишіть: *START <номер>*\nНаприклад: START 6");

            graph.sendText(toWaId, sb.toString());
        } catch (Exception ex) {
            log.error("sendEventsList failed: {}", ex.getMessage(), ex);
            graph.sendText(toWaId, "⚠️ Помилка при завантаженні списку подій");
        }
    }

    private void handleTextWithEventId(String toWaId, UserWhatsapp waAcc, long eventId, String groupId) {
        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            graph.sendText(toWaId, "❌ Івент #" + eventId + " не знайдено.\n\nПеревірте номер і спробуйте ще раз.");
            return;
        }

        boolean isSubscribed = subs.existsByEventAndUserWhatsappAndActiveIsTrue(event, waAcc);

        // НОВЕ: Якщо не підписаний - автоматично підписуємо
        if (!isSubscribed) {
            subscriptionService.toggleSubscription(eventId, waAcc, true);
            isSubscribed = true;

            String source = groupId != null ? "групі" : "чаті";
            graph.sendText(toWaId, "✅ Ви підписались на *" + event.getName() + "*\n\n🎉 Тепер будете отримувати оновлення в " + source + "!");
        } else {
            graph.sendText(toWaId, "ℹ️ Ви вже підписані на *" + event.getName() + "*");
        }

        sendEventMenuForSubscribed(toWaId, event, isSubscribed, groupId);
    }

    private void handleTextWithPostId(String toWaId, UserWhatsapp waAcc, long eventId, long postId, String groupId) {
        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            graph.sendText(toWaId, "❌ Івент #" + eventId + " не знайдено.");
            return;
        }

        boolean isSubscribed = subs.existsByEventAndUserWhatsappAndActiveIsTrue(event, waAcc);

        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();

        String eventDate = "Дата невідома";
        if (event.getStartAt() != null) {
            try {
                if (event.getStartAt() instanceof java.time.LocalDateTime) {
                    eventDate = ((java.time.LocalDateTime) event.getStartAt())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } else {
                    eventDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        .format(event.getStartAt());
                }
            } catch (Exception e) {
                log.warn("Failed to format date: {}", e.getMessage());
            }
        }

        String text = "📬 *" + eventName + "*\n" +
                "🕐 " + eventDate + "\n" +
                (event.getLocation() != null && !event.getLocation().isBlank() ? "📍 " + event.getLocation() + "\n" : "") +
                "\n" +
                "🔔 Подія #" + postId + "\n" +
                "\n" +
                (isSubscribed
                        ? "✅ Ви вже підписані на оновлення цього івенту."
                        : "Натисніть кнопку, щоб отримувати оновлення.");

        List<WhatsAppGraphClient.Button> buttons = new ArrayList<>();
        if (isSubscribed) buttons.add(new WhatsAppGraphClient.Button("EVT_UNSUB:" + eventId, "Відписатися"));
        else buttons.add(new WhatsAppGraphClient.Button("EVT_SUB:" + eventId, "Підписатися"));

        String link = resolveEventLinkUrl(event);
        if (link != null && !link.isBlank()) {
            buttons.add(new WhatsAppGraphClient.Button("OPEN_LINK:" + eventId, "Посилання"));
        }

        graph.sendReplyButtons(toWaId, text, buttons);

        String startLink = buildWaMeStartLink(eventId);
        if (startLink != null) graph.sendText(toWaId, "💡 Поділіться цим посиланням:\n" + startLink);
    }

    private void handleAction(String toWaId, UserWhatsapp waAcc, String id, String groupId) {
        try {
            if (id.startsWith("EVT_SUB:")) {
                long eventId = parseId(id, "EVT_SUB:");
                boolean nowActive = subscriptionService.toggleSubscription(eventId, waAcc, true);
                Event event = events.findById(eventId).orElse(null);

                if (event != null) {
                    String source = groupId != null ? "групи" : "чату";
                    graph.sendText(toWaId, "✅ Ви успішно підписались на *" + event.getName() + "*\n\n" +
                        "🎉 Тепер будете отримувати оновлення в " + source + "!");
                    sendEventMenu(toWaId, event, nowActive, groupId);
                } else {
                    graph.sendText(toWaId, "✅ Підписка активована");
                }

            } else if (id.startsWith("EVT_UNSUB:")) {
                long eventId = parseId(id, "EVT_UNSUB:");
                boolean nowActive = subscriptionService.toggleSubscription(eventId, waAcc, false);
                Event event = events.findById(eventId).orElse(null);

                if (event != null) {
                    graph.sendText(toWaId, "❌ Ви відписались від *" + event.getName() + "*");
                } else {
                    graph.sendText(toWaId, "❌ Підписка вимкнена");
                }
                if (event != null) sendEventMenu(toWaId, event, nowActive, groupId);

            } else if (id.startsWith("OPEN_LINK:")) {
                long eventId = parseId(id, "OPEN_LINK:");
                Event event = events.findById(eventId).orElse(null);
                String link = resolveEventLinkUrl(event);
                if (link == null || link.isBlank()) {
                    graph.sendText(toWaId, "🔗 Посилання на івент ще не налаштоване.");
                } else {
                    graph.sendText(toWaId, "🔗 *" + (event != null ? event.getName() : "Івент") + "*\n\n" + link);
                }

            } else {
                graph.sendText(toWaId, "⚠️ Невідома дія. Спробуйте ще раз.");
            }
        } catch (Exception ex) {
            log.error("handleAction failed: {}", ex.getMessage(), ex);
            graph.sendText(toWaId, "⚠️ Помилка обробки дії. Спробуйте ще раз.");
        }
    }

    private void sendEventMenu(String toWaId, Event event, boolean subscribed, String groupId) {
        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();

        String eventDate = "Дата невідома";
        if (event.getStartAt() != null) {
            try {
                Object startAt = event.getStartAt();

                if (startAt instanceof java.time.LocalDateTime) {
                    eventDate = ((java.time.LocalDateTime) startAt)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } else if (startAt instanceof java.time.ZonedDateTime) {
                    eventDate = ((java.time.ZonedDateTime) startAt)
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } else if (startAt instanceof java.sql.Timestamp) {
                    java.time.LocalDateTime ldt = ((java.sql.Timestamp) startAt).toLocalDateTime();
                    eventDate = ldt.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } else if (startAt instanceof java.util.Date) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
                    eventDate = sdf.format((java.util.Date) startAt);
                } else {
                    eventDate = startAt.toString();
                }
            } catch (Exception e) {
                log.warn("Failed to format date: {}", e.getMessage());
            }
        }

        String source = groupId != null ? "групі" : "особистому чаті";
        String text = "📅 *" + eventName + "*\n" +
                "🕐 " + eventDate + "\n" +
                (event.getLocation() != null ? "📍 " + event.getLocation() + "\n" : "") +
                "\n" +
                (subscribed
                        ? "✅ Ви підписані. Оновлення прийдуть до " + source + "."
                        : "🔔 Натисніть кнопку нижче, щоб отримувати оновлення до " + source + ".");

        List<WhatsAppGraphClient.Button> buttons = new ArrayList<>();

        if (subscribed) buttons.add(new WhatsAppGraphClient.Button("EVT_UNSUB:" + event.getId(), "Відписатися"));
        else buttons.add(new WhatsAppGraphClient.Button("EVT_SUB:" + event.getId(), "Підписатися"));

        String link = resolveEventLinkUrl(event);
        if (link != null && !link.isBlank()) {
            buttons.add(new WhatsAppGraphClient.Button("OPEN_LINK:" + event.getId(), "Посилання"));
        }

        graph.sendReplyButtons(toWaId, text, buttons);

        String startLink = buildWaMeStartLink(event.getId());
        if (startLink != null) graph.sendText(toWaId, "💡 Поділіться цим посиланням:\n" + startLink);
    }

    /**
     * Спрощений меню для вже підписаних користувачів (тільки кнопка "Відписатися")
     */
    private void sendEventMenuForSubscribed(String toWaId, Event event, boolean subscribed, String groupId) {
        String eventName = event.getName() != null ? event.getName() : "Івент #" + event.getId();
        String eventDate = formatEventDate(event.getStartAt());

        String source = groupId != null ? "групи" : "особистого чату";
        String text = "📅 *" + eventName + "*\n" +
                "🕐 " + eventDate + "\n" +
                (event.getLocation() != null ? "📍 " + event.getLocation() + "\n" : "") +
                "\n" +
                "✅ Ви підписані. Оновлення прийдуть до " + source + ".";

        List<WhatsAppGraphClient.Button> buttons = new ArrayList<>();
        buttons.add(new WhatsAppGraphClient.Button("EVT_UNSUB:" + event.getId(), "Відписатися"));

        String link = resolveEventLinkUrl(event);
        if (link != null && !link.isBlank()) {
            buttons.add(new WhatsAppGraphClient.Button("OPEN_LINK:" + event.getId(), "Посилання"));
        }

        graph.sendReplyButtons(toWaId, text, buttons);

        String startLink = buildWaMeStartLink(event.getId());
        if (startLink != null) graph.sendText(toWaId, "💡 Поділіться цим посиланням:\n" + startLink);
    }

    private String extractInteractiveId(JsonNode msg) {
        JsonNode interactive = msg.path("interactive");
        String itype = interactive.path("type").asText("");
        if ("button_reply".equals(itype)) return interactive.path("button_reply").path("id").asText(null);
        if ("list_reply".equals(itype)) return interactive.path("list_reply").path("id").asText(null);
        return null;
    }

    private long parseId(String data, String prefix) {
        return Long.parseLong(data.substring(prefix.length()));
    }

    private String resolveEventLinkUrl(Event e) {
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

    private String buildWaMeStartLink(long eventId) {
        if (businessPhoneE164 == null || businessPhoneE164.isBlank()) return null;

        String phone = businessPhoneE164.trim();
        if (phone.startsWith("+")) phone = phone.substring(1);

        String text = "START " + eventId;
        String enc = URLEncoder.encode(text, StandardCharsets.UTF_8);

        return "https://wa.me/" + phone + "?text=" + enc;
    }

    private String formatEventDate(Object dateObj) {
        if (dateObj == null) return "Дата невідома";

        try {
            if (dateObj instanceof LocalDateTime ldt) {
                return ldt.format(DATE_FORMATTER);
            } else if (dateObj instanceof java.util.Date date) {
                return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(date);
            } else {
                return "Дата невідома";
            }
        } catch (Exception e) {
            log.warn("Failed to format date: {}", e.getMessage());
            return "Дата невідома";
        }
    }
}
