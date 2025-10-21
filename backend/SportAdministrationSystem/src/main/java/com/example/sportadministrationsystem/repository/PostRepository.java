package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Audience;
import com.example.sportadministrationsystem.model.Channel;
import com.example.sportadministrationsystem.model.Post;
import com.example.sportadministrationsystem.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Використовується для побудови дерева "івент → пости" з фільтрами.
     */
    @Query("""
            select p from Post p
            where p.event.id = :eventId
              and (:status is null or p.status = :status)
              and (:audience is null or p.audience = :audience)
              and (:channel is null or p.channel = :channel)
            order by p.publishAt asc, p.id asc
            """)
    List<Post> findByEventIdAndFilters(@Param("eventId") Long eventId,
                                       @Param("status") PostStatus status,
                                       @Param("audience") Audience audience,
                                       @Param("channel") Channel channel);

    /**
     * Backward-compat alias (той самий запит). Можеш прибрати, якщо ніде явно не викликається.
     */
    @Query("""
            select p from Post p
            where p.event.id = :eventId
              and (:status is null or p.status = :status)
              and (:audience is null or p.audience = :audience)
              and (:channel is null or p.channel = :channel)
            order by p.publishAt asc, p.id asc
            """)
    List<Post> findByEventAndFilters(@Param("eventId") Long eventId,
                                     @Param("status") PostStatus status,
                                     @Param("audience") Audience audience,
                                     @Param("channel") Channel channel);

    /**
     * Батч на розсилку з блокуванням рядків (PostgreSQL):
     * забираємо N найраніших SCHEDULED із дедлайном <= now і блокуємо їх, щоб інші воркери не взяли.
     */
    @Query(value = """
            select * from posts
            where status = 'SCHEDULED' and publish_at <= :now
            order by publish_at asc, id asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<Post> lockBatchDueForDispatch(@Param("now") LocalDateTime now,
                                       @Param("limit") int limit);

    /**
     * Fallback без блокувань (для один-в-один інстансу або локальних запусків).
     */
    @Query("""
            select p from Post p
            where p.status = :status and p.publishAt <= :now
            order by p.publishAt asc, p.id asc
            """)
    List<Post> findPostsToDispatch(@Param("status") PostStatus status,
                                   @Param("now") LocalDateTime now);

    /**
     * Використовується генератором постів, щоб не дублювати автосгенеровані вікна.
     */
    boolean existsByEvent_IdAndGeneratedIsTrueAndPublishAtAndChannelAndAudience(
            Long eventId,
            LocalDateTime publishAt,
            Channel channel,
            Audience audience
    );

    /**
     * Корисно для швидких лічильників по статусу (опційно для UI).
     */
    long countByEvent_IdAndStatus(Long eventId, PostStatus status);
    @Query(value = """
            SELECT *
            FROM posts
            WHERE status = 'SCHEDULED'
              AND publish_at <= NOW()
            ORDER BY publish_at ASC, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    java.util.List<com.example.sportadministrationsystem.model.Post> lockNextDue(@Param("batchSize") int batchSize);

}
