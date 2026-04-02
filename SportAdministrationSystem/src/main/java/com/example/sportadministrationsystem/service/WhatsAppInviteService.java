package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppInviteService {

    private final EventRepository eventRepository;

    @Value("${whatsapp.business-phone-e164:}")
    private String businessPhoneE164;

    /**
     * Формує invite повідомлення для WhatsApp
     * @param eventId ID івенту
     * @param shortVersion true для короткої версії (для Announcement group)
     * @return текст анонсу
     */
    public String buildInviteMessage(long eventId, boolean shortVersion) {
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return null;
        }

        Event event = eventOpt.get();
        return buildInviteMessage(event, shortVersion);
    }

    public String buildInviteMessage(Event event, boolean shortVersion) {
        if (event == null) return null;

        String eventName = event.getName() != null ? event.getName() : "Подія";
        String location = event.getLocation() != null ? event.getLocation() : "";
        String description = event.getDescription() != null ? event.getDescription() : "";

        // Форматування дати
        String eventDate = "";
        if (event.getStartAt() != null) {
            try {
                if (event.getStartAt() instanceof java.time.LocalDateTime) {
                    eventDate = ((java.time.LocalDateTime) event.getStartAt())
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
                } else {
                    eventDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm")
                        .format(event.getStartAt());
                }
            } catch (Exception e) {
                log.warn("Failed to format date: {}", e.getMessage());
            }
        }

        if (shortVersion) {
            // Коротка версія для Announcement group
            return String.format(
                "📢 *%s* 🎉\n" +
                "🕐 %s\n" +
                "%s\n\n" +
                "Реєструйтесь: https://wa.me/%s?text=START%%20%d",
                eventName,
                eventDate,
                location.isEmpty() ? "" : "📍 " + location + "\n",
                getPhoneNumberWithoutPlus(),
                event.getId()
            );
        } else {
            // Повна версія для особистого чату
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📅 *%s*\n", eventName));

            if (!eventDate.isEmpty()) {
                sb.append(String.format("🕐 %s\n", eventDate));
            }

            if (!location.isEmpty()) {
                sb.append(String.format("📍 %s\n", location));
            }

            if (!description.isEmpty()) {
                sb.append(String.format("\n📝 %s\n", description));
            }

            sb.append(String.format(
                "\n💬 Дізнайтеся детальніше та зареєструйтеся:\n" +
                "https://wa.me/%s?text=START%%20%d",
                getPhoneNumberWithoutPlus(),
                event.getId()
            ));

            return sb.toString();
        }
    }

    /**
     * Генерує wa.me посилання
     */
    public String buildWaMeLink(long eventId) {
        String phone = getPhoneNumberWithoutPlus();
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String text = "START " + eventId;
        String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
        return "https://wa.me/" + phone + "?text=" + encoded;
    }

    /**
     * Отримує номер бізнес-телефону без +
     */
    private String getPhoneNumberWithoutPlus() {
        if (businessPhoneE164 == null || businessPhoneE164.isBlank()) {
            return null;
        }

        String phone = businessPhoneE164.trim();
        if (phone.startsWith("+")) {
            phone = phone.substring(1);
        }

        return phone;
    }
}
