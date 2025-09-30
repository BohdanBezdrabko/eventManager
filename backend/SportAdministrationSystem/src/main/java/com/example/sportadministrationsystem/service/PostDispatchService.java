package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import com.example.sportadministrationsystem.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostDispatchService {

    private final PostRepository postRepository;

    @Value("${app.dispatch.batch-size:20}")
    private int batchSize;

    @Value("${app.dispatch.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${app.dispatch.delay-ms:60000}")
    public void tick() {
        if (!enabled) return;
        int processed = 0;
        while (true) {
            int n = dispatchBatch();
            processed += n;
            if (n < batchSize) break;
        }
        if (processed > 0) {
            log.info("PostDispatchService: processed {} posts", processed);
        }
    }

    @Transactional
    public int dispatchBatch() {
        List<Post> due = postRepository.lockDueForDispatch(LocalDateTime.now(), batchSize);
        for (Post p : due) {
            try {
                switch (p.getChannel()) {
                    case INTERNAL -> handleInternal(p);
                    case TELEGRAM -> handleTelegramStub(p); // реалізуємо на кроці 5
                    default -> throw new IllegalStateException("Unknown channel: " + p.getChannel());
                }
                p.setStatus(PostStatus.PUBLISHED);
                p.setError(null);
            } catch (Exception ex) {
                p.setStatus(PostStatus.FAILED);
                p.setError(ex.getMessage());
                log.warn("Dispatch failed for post id={} : {}", p.getId(), ex.getMessage());
            }
            postRepository.save(p);
        }
        return due.size();
    }

    private void handleInternal(Post p) {
        String externalId = "internal:" + p.getId() + ":" + System.currentTimeMillis();
        log.info("INTERNAL DISPATCH [{}] eventId={} title='{}' at {}",
                externalId, p.getEvent().getId(), safe(p.getTitle(), 120), LocalDateTime.now());
        p.setExternalId(externalId);
    }

    private void handleTelegramStub(Post p) {
        throw new UnsupportedOperationException("TELEGRAM channel is not implemented at this step");
    }

    private static String safe(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
