package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
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

    @Query(value = """
            select * from posts
            where status = 'SCHEDULED' and publish_at <= :now
            order by publish_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<Post> lockDueForDispatch(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
