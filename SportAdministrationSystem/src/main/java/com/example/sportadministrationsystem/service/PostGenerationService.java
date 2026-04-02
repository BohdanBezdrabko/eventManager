package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.model.PostTemplate;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import com.example.sportadministrationsystem.repository.PostTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostGenerationService {

    private final EventRepository eventRepo;
    private final PostRepository postRepo;
    private final PostTemplateRepository templateRepo;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Викликається при створенні/оновленні івенту або добовим сканером.
     * Генерує пости тільки для дозволених каналів.
     *
     * @param e івент
     * @param allowedChannels Набір дозволених каналів (напр. "TELEGRAM", "WHATSAPP")
     *                        Якщо null/пусто - используем всі шаблони
     */
    @Transactional
    public void ensureEventScheduledPosts(Event e, Set<Channel> allowedChannels) {
        EventCategory cat = e.getCategory();
        List<PostTemplate> templates = templateRepo.findActiveForCategory(cat);

        if (allowedChannels != null && !allowedChannels.isEmpty()) {
            // Фільтруємо шаблони тільки для дозволених каналів
            templates = templates.stream()
                    .filter(t -> allowedChannels.contains(t.getChannel()))
                    .toList();
            log.info("Filtering post templates by allowed channels: {} (found {} templates)",
                    allowedChannels, templates.size());
        } else {
            log.info("No channel filter, using all templates for event {}", e.getId());
        }

        for (PostTemplate t : templates) {
            LocalDateTime publishAt = e.getStartAt()
                    .plusDays(t.getOffsetDays())
                    .plusHours(t.getOffsetHours());

            boolean exists = postRepo.existsByEvent_IdAndGeneratedIsTrueAndPublishAtAndChannelAndAudience(
                    e.getId(), publishAt, t.getChannel(), t.getAudience());
            if (exists) continue;

            String title = render(t.getTitleTpl(), e);
            String body = render(t.getBodyTpl(), e);

            Post p = Post.builder()
                    .event(e)
                    .title(title)
                    .body(body)
                    .publishAt(publishAt)
                    .status(PostStatus.SCHEDULED)
                    .audience(t.getAudience())
                    .channel(t.getChannel())
                    .generated(true)
                    .build();

            postRepo.save(p);
            log.debug("Generated post for event {}, channel {}, publish at {}",
                    e.getId(), t.getChannel(), publishAt);
        }
    }

    /**
     * Перегружена версія без фільтру каналів (для сумісності)
     */
    @Transactional
    public void ensureEventScheduledPosts(Event e) {
        ensureEventScheduledPosts(e, null);
    }

    /**
     * Добовий сканер на випадок змін дат/категорій.
     * Генерує ДЛЯ ВСІХ КАНАЛІВ (бекдрп старої логіки)
     */
    @Transactional
    @Scheduled(cron = "0 5 * * * *")
    public void ensureForUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = eventRepo.findByStartAtAfter(now);
        for (Event e : upcoming) {
            try {
                // Добовий сканер генерує для всіх каналів
                ensureEventScheduledPosts(e);
            } catch (Exception ex) {
                log.warn("Autogen posts failed for event id={} : {}", e.getId(), ex.getMessage());
            }
        }
    }

    private String render(String tpl, Event e) {
        String startAt = e.getStartAt().format(DT);
        String location = e.getLocation() == null ? "" : e.getLocation();
        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), e.getStartAt().toLocalDate()));

        return tpl.replace("{event_name}", e.getName())
                .replace("{start_at}", startAt)
                .replace("{location}", location)
                .replace("{days_left}", String.valueOf(daysLeft));
    }
}
