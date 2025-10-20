package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /** Завантажити всі події з тегами (fetch join) */
    @Query("select distinct e from Event e left join fetch e.tags")
    List<Event> findAllWithTags();

    /** Завантажити подію з тегами за id (fetch join) */
    @Query("select e from Event e left join fetch e.tags where e.id = :id")
    Optional<Event> findByIdWithTags(@Param("id") Long id);

    /** Те саме, але з іншою назвою, бо різні сервіси викликають різні методи */
    @Query("select e from Event e left join fetch e.tags where e.id = :id")
    Optional<Event> findWithTagsById(@Param("id") Long id);

    /** Пошук за назвою (case-insensitive) */
    List<Event> findByNameContainingIgnoreCase(String name);

    /** Пошук за локацією (case-insensitive) */
    List<Event> findByLocationContainingIgnoreCase(String location);

    /** Майбутні події (для PostGenerationService) */
    List<Event> findByStartAtAfter(LocalDateTime from);

    /**
     * Фільтр: категорія + тег (тег — частковий збіг, case-insensitive).
     * Якщо параметр tag == null або порожній, повертаємо всі з цієї категорії.
     */
    @Query("""
           select distinct e
           from Event e
             left join e.tags t
           where e.category = :category
             and ( :tag is null or :tag = '' or lower(t.name) like lower(concat('%', :tag, '%')) )
           """)
    List<Event> findAllByCategoryAndTag(@Param("category") EventCategory category,
                                        @Param("tag") String tag);

    /** "FOR UPDATE" для безпечної модифікації лічильників/місць у межах TX */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
