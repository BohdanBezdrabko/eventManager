package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.PostDelivery;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDeliveryRepository extends JpaRepository<PostDelivery, Long> {

    /**
     * Бере FAILED-спроби, у яких настав час наступної спроби (експонента 1,2,4,8,16 хв),
     * та локує їх SKIP LOCKED. Обмежуємо за maxAttempts (наприклад, 5).
     *
     * now() >= created_at + (interval '1 minute' * (1 << (attempt_no - 1)))
     */
    @Query(value = """
        SELECT *
          FROM post_delivery
         WHERE status = 'FAILED'
           AND attempt_no < :maxAttempts
           AND (created_at + (interval '1 minute' * (1 << GREATEST(attempt_no - 1,0)))) <= NOW()
         ORDER BY created_at ASC, id ASC
         LIMIT :batchSize
         FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PostDelivery> lockDueRetries(@Param("batchSize") int batchSize,
                                      @Param("maxAttempts") int maxAttempts);

    /**
     * Ідемпотентний запис спроби: якщо (post_id, target, attempt_no) вже є —
     * оновлюємо status/error (без updated_at, бо такої колонки у вас немає).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO post_delivery (post_id, target, attempt_no, status, error, created_at)
        VALUES (:postId, :target, :attemptNo, :status, :error, NOW())
        ON CONFLICT (post_id, target, attempt_no)
        DO UPDATE SET status = EXCLUDED.status,
                      error  = EXCLUDED.error
        """, nativeQuery = true)
    int upsertAttempt(@Param("postId") Long postId,
                      @Param("target") String target,
                      @Param("attemptNo") int attemptNo,
                      @Param("status") String status,
                      @Param("error") String error);
}
