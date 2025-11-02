package com.example.sportadministrationsystem.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostGenerationService {

    private final EventRepository eventRepo;
    private final PostRepository postRepo;
    private final PostTemplateRepository templateRepo;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Викликається при створенні/оновленні івенту або добовим сканером
     */
    @Transactional
    public void ensureEventScheduledPosts(Event e) {
        EventCategory cat = e.getCategory();
        List<PostTemplate> templates = templateRepo.findActiveForCategory(cat);

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
                    .audience(t.getAudience())   // ← ключове для п.7
                    .channel(t.getChannel())     // ← TELEGRAM/INTERNAL береться з шаблону
                    .generated(true)
                    .build();

            postRepo.save(p);
        }
    }

    /**
     * Добовий сканер на випадок змін дат/категорій
     */
    @Transactional
    @Scheduled(cron = "0 5 * * * *")
    public void ensureForUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = eventRepo.findByStartAtAfter(now);
        for (Event e : upcoming) {
            try {
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
