package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Базові пошуки
    Optional<Event> findByName(String name);
    List<Event> findAllByNameIgnoreCase(String name);
    List<Event> findAllByLocationIgnoreCase(String location);

    // "contains" + одразу підвантажуємо теги
    @EntityGraph(attributePaths = {"tags"})
    List<Event> findByNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = {"tags"})
    List<Event> findByLocationContainingIgnoreCase(String location);

    // Перевірки дублікатів
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndStartAt(String name, LocalDateTime startAt);

    // Завантаження з тегами
    @EntityGraph(attributePaths = {"tags"})
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdWithTags(@Param("id") Long id);

    // Повний список з тегами (для списку без фільтрів)
    @EntityGraph(attributePaths = {"tags"})
    @Query("select e from Event e")
    List<Event> findAllWithTags();

    // Песимістичне блокування
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    // Фільтр category/tag — enum
    @EntityGraph(attributePaths = {"tags"})
    @Query("""
        select distinct e
        from Event e
        left join e.tags t
        where (:category is null or e.category = :category)
          and (:tag is null or t.name = :tag)
        order by e.startAt asc, e.id asc
    """)
    List<Event> findAllByCategoryAndTag(@Param("category") EventCategory category,
                                        @Param("tag") String tag);

    // Альтернативний фільтр — String + cast (залишено за потреби)
    @EntityGraph(attributePaths = {"tags"})
    @Query("""
        select distinct e
        from Event e
        left join e.tags t
        where (:category is null or cast(e.category as string) = :category)
          and (:tag is null or t.name = :tag)
        order by e.startAt nulls last, e.id
    """)
    List<Event> findAllByCategoryAndTagRaw(@Param("category") String category,
                                           @Param("tag") String tag);
    java.util.List<com.example.sportadministrationsystem.model.Event>
    findByStartAtAfter(java.time.LocalDateTime time);
}
