package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // ---------- Базові вибірки ----------
    Page<Event> findAll(Pageable pageable);

    // Категорія
    Page<Event> findByCategory(EventCategory category, Pageable pageable);

    // Автор (createdBy.id) — обидва варіанти неймінгу, щоб сумісно з сервісом:
    // CamelCase без підкреслення:
    Page<Event> findByCreatedById(Long createdById, Pageable pageable);
    List<Event> findByCreatedById(Long createdById);
    // Явний шлях з підкресленням:
    Page<Event> findByCreatedBy_Id(Long createdById, Pageable pageable);
    List<Event> findByCreatedBy_Id(Long createdById);

    // Майбутні події (для генерації постів)
    List<Event> findByStartAtAfter(LocalDateTime instant);

    // ---------- Пошук за тегом / категорією + тегом ----------
    @Query("""
           select e
           from Event e
             join e.tags t
           where lower(t.name) = lower(:tag)
           """)
    Page<Event> findByTagName(@Param("tag") String tag, Pageable pageable);

    @Query("""
           select e
           from Event e
             join e.tags t
           where e.category = :category and lower(t.name) = lower(:tag)
           """)
    Page<Event> findByCategoryAndTag(@Param("category") EventCategory category,
                                     @Param("tag") String tag,
                                     Pageable pageable);

    // ---------- Пошук за назвою / локацією ----------
    @Query("""
           select e
           from Event e
           where lower(e.name) like lower(concat('%', :name, '%'))
           """)
    List<Event> searchByNameLike(@Param("name") String name);

    @Query("""
           select e
           from Event e
           where lower(e.location) like lower(concat('%', :location, '%'))
           """)
    List<Event> searchByLocationLike(@Param("location") String location);

    // ---------- Fetch-теги для дерево-подання / детального перегляду ----------
    @Query("""
           select e
           from Event e
           left join fetch e.tags
           where e.id = :id
           """)
    Optional<Event> findWithTagsById(@Param("id") Long id);

    // ---------- Песимістичне блокування для безпечних оновлень ----------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
