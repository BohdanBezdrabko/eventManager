package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.*;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import com.example.sportadministrationsystem.repository.PostDeliveryRepository;
import com.example.sportadministrationsystem.repository.PostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostDispatchService {

    private static final long LOCK_DISPATCH = DbLockService.key("post_dispatcher");
    private static final long LOCK_RETRY    = DbLockService.key("post_delivery_retry");

    private final PostRepository postRepository;
    private final PostDeliveryRepository deliveryRepository;
    private final EventSubscriptionRepository subscriptionRepository;
    private final TelegramService telegramService;
    private final DbLockService dbLockService;

    /** Менеджер транзакцій і шаблон для REQUIRES_NEW */
    private final PlatformTransactionManager txManager;
    private TransactionTemplate txRequiresNew;

    /** чат для PUBLIC, якщо в пості не вказано override (telegramChatId) */
    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    /** Розмір батчу постів на один «тік» */
    @Value("${dispatcher.batch-size:50}")
    private int dispatchBatch;

    /** Розмір батчу ретраїв */
    @Value("${dispatcher.retry-batch-size:200}")
    private int retryBatch;

    /** Ліміт ретраїв (включно з першою спробою це 5 записів у post_delivery) */
    @Value("${dispatcher.max-attempts:5}")
    private int maxAttempts;

    @PostConstruct
    void init() {
        this.txRequiresNew = new TransactionTemplate(txManager);
        this.txRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        log.info("PostDispatchService: batch={}, retryBatch={}, maxAttempts={}", dispatchBatch, retryBatch, maxAttempts);
    }

    /* ========== 1) Основний тікер публікацій (кожні 60с) ========== */

    @Scheduled(fixedDelay = 60_000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // критично: без загальної транзакції
    public void tick() {
        if (!dbLockService.tryLock(LOCK_DISPATCH)) {
            return; // інший інстанс уже працює
        }
        try {
            while (true) {
                List<Post> batch = lockBatchDueForDispatch(dispatchBatch);
                if (batch.isEmpty()) break;

                for (Post post : batch) {
                    try {
                        // кожен пост — у своїй окремій транзакції
                        txRequiresNew.executeWithoutResult(status -> dispatchOneTransactional(post));
                    } catch (Exception e) {
                        log.warn("Dispatch error for post #{}: {}", post.getId(), e.toString());
                    }
                }
            }
        } finally {
            dbLockService.unlock(LOCK_DISPATCH);
        }
    }

    /**
     * Отримання батчу до публікації.
     * Тут може бути SELECT ... FOR UPDATE SKIP LOCKED — виконуємо у короткій транзакції.
     */
    @Transactional // короткоживуча транзакція лише на вибірку батчу
    protected List<Post> lockBatchDueForDispatch(int limit) {
        return postRepository.lockNextDue(limit);
    }

    /**
     * Повна обробка ОДНОГО поста всередині REQUIRES_NEW.
     */
    protected void dispatchOneTransactional(Post post) {
        List<String> errors = new ArrayList<>();

        try {
            if (post.getChannel() == Channel.TELEGRAM) {
                dispatchTelegram(post, errors);
            } else if (post.getChannel() == Channel.INTERNAL) {
                log.info("[INTERNAL] #{} {}", post.getId(), composeText(post));
            } else {
                errors.add("Unsupported channel: " + post.getChannel());
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
            log.warn("Dispatch error for post #{}: {}", post.getId(), e.toString());
        }

        // Вважаємо пост опублікованим; ретраї покриють індивідуальні провали
        post.setStatus(PostStatus.PUBLISHED);
        post.setError(errors.isEmpty() ? null : "Partial failures: " + String.join(" | ", errors));
        postRepository.save(post);
    }

    /* -------- Телеграм канал/підписники -------- */

    private void dispatchTelegram(Post post, List<String> errors) {
        String text = composeText(post);

        if (post.getAudience() == Audience.PUBLIC) {
            String targetChat = (post.getTelegramChatId() != null && !post.getTelegramChatId().isBlank())
                    ? post.getTelegramChatId()
                    : defaultChatId;

            if (targetChat == null || targetChat.isBlank()) {
                throw new IllegalStateException("No Telegram chat configured for PUBLIC post.");
            }

            try {
                telegramService.sendMessage(targetChat, text);
                saveDeliveryIdempotent(post, targetChat, 1, DeliveryStatus.SENT, null);
            } catch (TelegramApiException e) {
                saveDeliveryIdempotent(post, targetChat, 1, DeliveryStatus.FAILED, e.getMessage());
                errors.add("PUBLIC send failed: " + e.getMessage());
            }

        } else if (post.getAudience() == Audience.SUBSCRIBERS) {
            Event eventRef = post.getEvent(); // LAZY, але ми всередині транзакції REQUIRES_NEW
            List<Long> chatIds = subscriptionRepository.findSubscriberChatIds(eventRef, Messenger.TELEGRAM);

            for (Long chatId : chatIds) {
                String target = String.valueOf(chatId);
                try {
                    telegramService.sendMessage(target, text);
                    saveDeliveryIdempotent(post, target, 1, DeliveryStatus.SENT, null);
                } catch (TelegramApiException e) {
                    saveDeliveryIdempotent(post, target, 1, DeliveryStatus.FAILED, e.getMessage());
                    // не перериваємо цикл — залишаємо під ретрай
                }
            }
        } else {
            throw new IllegalStateException("Unsupported audience: " + post.getAudience());
        }
    }

    /**
     * Ідемпотентне збереження спроби доставки — уникає Duplicate Key на (post_id, target, attempt_no).
     * Працює в поточній транзакції (не відкриває власної).
     */
    protected void saveDeliveryIdempotent(Post post, String target, int attemptNo, DeliveryStatus status, String error) {
        deliveryRepository.upsertAttempt(
                post.getId(),
                target,
                attemptNo,
                status.name(),
                error
        );
    }

    private String composeText(Post post) {
        if (post.getTitle() != null && !post.getTitle().isBlank()) {
            return post.getTitle() + "\n\n" + post.getBody();
        }
        return post.getBody();
    }

    /* ========== 2) Ретраї доставки (кожні 60с, зсув 15с) ========== */

    // Без транзакції — щоб будь-який фейл усередині не «завалив» увесь цикл і не завадив unlock()
    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void retryTick() {
        if (!dbLockService.tryLock(LOCK_RETRY)) {
            return;
        }
        try {
            while (true) {
                List<PostDelivery> due = lockDueRetries(retryBatch, maxAttempts);
                if (due.isEmpty()) break;

                for (PostDelivery failed : due) {
                    try {
                        // кожен ретрай — у власній REQUIRES_NEW транзакції
                        txRequiresNew.executeWithoutResult(status -> retryOneTransactional(failed.getId()));
                    } catch (Exception ex) {
                        log.warn("Retry error for post #{}, target {}: {}",
                                safePostId(failed), failed.getTarget(), ex.toString(), ex);
                    }
                }
            }
        } finally {
            dbLockService.unlock(LOCK_RETRY);
        }
    }

    // Обробка ОДНІЄЇ невдалої доставки всередині REQUIRES_NEW (викликається з txRequiresNew)
    protected void retryOneTransactional(Long failedId) {
        PostDelivery failed = deliveryRepository.findById(failedId).orElseThrow();
        Post post = failed.getPost();
        String text = composeText(post);
        int nextAttempt = failed.getAttemptNo() + 1;

        try {
            telegramService.sendMessage(failed.getTarget(), text);
            saveDeliveryIdempotent(post, failed.getTarget(), nextAttempt, DeliveryStatus.SENT, null);
        } catch (TelegramApiException e) {
            saveDeliveryIdempotent(post, failed.getTarget(), nextAttempt, DeliveryStatus.FAILED, e.getMessage());
        }
    }

    /** Вибірка ретраїв під блокування (FOR UPDATE SKIP LOCKED у native-запиті) */
    @Transactional
    protected List<PostDelivery> lockDueRetries(int batch, int maxAttempts) {
        return deliveryRepository.lockDueRetries(batch, maxAttempts);
    }

    private Object safePostId(PostDelivery d) {
        try { return d.getPost() != null ? d.getPost().getId() : null; }
        catch (Exception ignore) { return null; }
    }
}
