package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private static final Pattern START_PATTERN =
            Pattern.compile("^\\s*start\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper om = new ObjectMapper();

    private final EventRepository events;
    private final EventSubscriptionWhatsappRepository subs;
    private final WhatsAppAccountProvisioner provisioner;
    private final EventSubscriptionWhatsAppService subscriptionService;
    private final WhatsAppGraphClient graph;

    @Value("${whatsapp.business-phone-e164:}")
    private String businessPhoneE164;

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

                        UserWhatsapp waAcc = provisioner.ensure(from, profileName);

                        String type = msg.path("type").asText("");
                        if ("text".equals(type)) {
                            String text = msg.path("text").path("body").asText("");
                            handleText(from, waAcc, text);
                        } else if ("interactive".equals(type)) {
                            String id = extractInteractiveId(msg);
                            if (id != null) handleAction(from, waAcc, id);
                        } else if ("button".equals(type)) {
                            String id = msg.path("button").path("payload").asText(null);
                            if (id != null) handleAction(from, waAcc, id);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("handleWebhook failed: {}", e.getMessage(), e);
        }
    }

    private void handleText(String toWaId, UserWhatsapp waAcc, String text) {
        Matcher m = START_PATTERN.matcher(text == null ? "" : text);
        if (!m.matches()) {
            graph.sendText(toWaId, "Напиши: START <eventId>\nНаприклад: START 12");
            return;
        }

        long eventId = Long.parseLong(m.group(1));
        Event event = events.findById(eventId).orElse(null);
        if (event == null) {
            graph.sendText(toWaId, "Івент не знайдено.");
            return;
        }

        boolean isSubscribed = subs.existsByEventAndUserWhatsappAndActiveIsTrue(event, waAcc);
        sendEventMenu(toWaId, event, isSubscribed);
    }

    private void handleAction(String toWaId, UserWhatsapp waAcc, String id) {
        try {
            if (id.startsWith("EVT_SUB:")) {
                long eventId = parseId(id, "EVT_SUB:");
                boolean nowActive = subscriptionService.toggleSubscription(eventId, waAcc, true);
                Event event = events.findById(eventId).orElse(null);

                graph.sendText(toWaId, "Підписка активована ✅");
                if (event != null) sendEventMenu(toWaId, event, nowActive);

            } else if (id.startsWith("EVT_UNSUB:")) {
                long eventId = parseId(id, "EVT_UNSUB:");
                boolean nowActive = subscriptionService.toggleSubscription(eventId, waAcc, false);
                Event event = events.findById(eventId).orElse(null);

                graph.sendText(toWaId, "Підписка вимкнена ❌");
                if (event != null) sendEventMenu(toWaId, event, nowActive);

            } else if (id.startsWith("OPEN_LINK:")) {
                long eventId = parseId(id, "OPEN_LINK:");
                Event event = events.findById(eventId).orElse(null);
                String link = resolveEventLinkUrl(event);
                if (link == null || link.isBlank()) graph.sendText(toWaId, "Для цього івента немає посилання.");
                else graph.sendText(toWaId, link);

            } else {
                graph.sendText(toWaId, "Невідома дія: " + id);
            }
        } catch (Exception ex) {
            log.error("handleAction failed: {}", ex.getMessage(), ex);
            graph.sendText(toWaId, "Помилка обробки дії.");
        }
    }

    private void sendEventMenu(String toWaId, Event event, boolean subscribed) {
        String text = subscribed
                ? "Ви вже підписані на нагадування про цей івент."
                : "Ви не підписані на цей івент. Натисніть кнопку нижче.";

        List<WhatsAppGraphClient.Button> buttons = new ArrayList<>();

        if (subscribed) buttons.add(new WhatsAppGraphClient.Button("EVT_UNSUB:" + event.getId(), "Відписатися"));
        else buttons.add(new WhatsAppGraphClient.Button("EVT_SUB:" + event.getId(), "Підписатися"));

        String link = resolveEventLinkUrl(event);
        if (link != null && !link.isBlank()) {
            buttons.add(new WhatsAppGraphClient.Button("OPEN_LINK:" + event.getId(), "Посилання"));
        }

        graph.sendReplyButtons(toWaId, text, buttons);

        String startLink = buildWaMeStartLink(event.getId());
        if (startLink != null) graph.sendText(toWaId, "Швидке посилання:\n" + startLink);
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
}
