// src/main/java/com/example/sportadministrationsystem/service/PostDispatchScheduler.java
package com.example.sportadministrationsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostDispatchScheduler {
    private final PostService postService;
    private final DbLockService dbLockService;

    // Раз на dispatcher.interval-ms (за замовчуванням 60с) відправляємо всі due-пости
    @Scheduled(fixedDelayString = "${dispatcher.interval-ms:60000}")
    public void tick() {
        long lockKey = DbLockService.key("post-dispatcher");
        if (!dbLockService.tryLock(lockKey)) {
            log.debug("Could not acquire lock for post dispatcher, skipping this tick");
            return;
        }

        try {
            int sent = postService.dispatchDue();
            if (sent > 0) log.info("Dispatched {} scheduled posts", sent);
        } catch (Exception e) {
            log.error("Error during post dispatch: {}", e.getMessage(), e);
        } finally {
            try {
                dbLockService.unlock(lockKey);
            } catch (Exception e) {
                log.warn("Failed to unlock post dispatcher: {}", e.getMessage());
            }
        }
    }
}
