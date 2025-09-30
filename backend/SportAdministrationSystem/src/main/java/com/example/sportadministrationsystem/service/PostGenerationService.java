package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventTemplateOverride;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.model.PostTemplate;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventTemplateOverrideRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import com.example.sportadministrationsystem.repository.PostTemplateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostGenerationService {

    private final EventRepository eventRepo;
    private final PostTemplateRepository templateRepo;
    private final EventTemplateOverrideRepository overrideRepo;
    private final PostRepository postRepo;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public void ensureEventScheduledPosts(Event e) {
        if (e.getStartAt() == null) return;

        List<PostTemplate> templates = templateRepo.findActiveForCategory(e.getCategory());
        LocalDateTime now = LocalDateTime.now();

        for (PostTemplate t : templates) {
            LocalDateTime publishAt = e.getStartAt()
                    .plusDays(t.getOffsetDays())
                    .plusHours(t.getOffsetHours());

            // лише майбутні публікації
            if (publishAt.isBefore(now)) continue;

            boolean exists = postRepo.existsByEvent_IdAndGeneratedIsTrueAndPublishAtAndChannelAndAudience(
                    e.getId(), publishAt, t.getChannel(), t.getAudience());
            if (exists) continue;

            // override
            Optional<EventTemplateOverride> ovOpt =
                    overrideRepo.findByEvent_IdAndTemplate_Id(e.getId(), t.getId());

            boolean active = t.isActive();
            String titleTpl = t.getTitleTpl();
            String bodyTpl  = t.getBodyTpl();

            if (ovOpt.isPresent()) {
                EventTemplateOverride ov = ovOpt.get();
                if (ov.getActive() != null) active = ov.getActive();
                if (!active) continue;
                if (ov.getTitleTpl() != null && !ov.getTitleTpl().isBlank()) titleTpl = ov.getTitleTpl();
                if (ov.getBodyTpl()  != null && !ov.getBodyTpl().isBlank())  bodyTpl  = ov.getBodyTpl();
            }

            String title = render(titleTpl, e, publishAt);
            String body  = render(bodyTpl,  e, publishAt);

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
        }
    }

    private String render(String tpl, Event e, LocalDateTime pubAt) {
        long daysLeft = Duration.between(pubAt, e.getStartAt()).toDays();
        if (daysLeft < 0) daysLeft = 0;
        return tpl
                .replace("{event_name}", ns(e.getName()))
                .replace("{start_at}",  e.getStartAt() == null ? "" : e.getStartAt().format(DF))
                .replace("{location}",  ns(e.getLocation()))
                .replace("{days_left}", Long.toString(daysLeft));
    }

    private static String ns(String s) { return s == null ? "" : s; }

    // Добовий сканер: щогодини о 05 хв генерує відсутні пости для всіх майбутніх івентів
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
}
