package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscriptionWhatsapp;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSubscriptionWhatsappRepository extends JpaRepository<EventSubscriptionWhatsapp, Long> {

    Optional<EventSubscriptionWhatsapp> findByEventAndUserWhatsapp(Event event, UserWhatsapp userWhatsapp);

    boolean existsByEventAndUserWhatsappAndActiveIsTrue(Event event, UserWhatsapp userWhatsapp);

    @Query("""
        select distinct esw.userWhatsapp.waId
          from EventSubscriptionWhatsapp esw
         where esw.event.id = :eventId
           and esw.active = true
           and esw.userWhatsapp.waId is not null
    """)
    List<String> findSubscriberWaIds(@Param("eventId") Long eventId);

    /**
     * Рахує кількість активних WhatsApp-підписників на івент.
     * Рахуємо тільки тих, у кого є wa_id (тобто реально зв'язані у WhatsApp),
     * і лише active=true.
     */
    @Query("""
        select count(distinct esw.userWhatsapp.waId)
          from EventSubscriptionWhatsapp esw
         where esw.event.id = :eventId
           and esw.active = true
           and esw.userWhatsapp.waId is not null
    """)
    long countActiveWhatsApp(@Param("eventId") Long eventId);

    /**
     * Знаходить всі активні підписки на івенти у заданому діапазоні часу,
     * де 72-годинне нагадування ще не було відправлено.
     * Використовується для відправки 72-годинних нагадувань.
     *
     * ВАЖЛИВО: використовуємо LEFT JOIN FETCH для eager loading UserWhatsapp
     * щоб уникнути LazyInitializationException в асинхронному контексті
     */
    @Query("""
        select esw
          from EventSubscriptionWhatsapp esw
          left join fetch esw.userWhatsapp
          left join fetch esw.event
         where esw.event.startAt > :minTime
           and esw.event.startAt < :maxTime
           and esw.active = true
           and esw.reminder72hSent = false
           and esw.userWhatsapp.waId is not null
    """)
    List<EventSubscriptionWhatsapp> findForReminder72h(
        @Param("minTime") java.time.LocalDateTime minTime,
        @Param("maxTime") java.time.LocalDateTime maxTime
    );

    /**
     * Знаходить всі активні підписки на івенти у заданому діапазоні часу,
     * де 24-годинне нагадування ще не було відправлено.
     * Використовується для відправки 24-годинних нагадувань.
     *
     * ВАЖЛИВО: використовуємо LEFT JOIN FETCH для eager loading UserWhatsapp
     * щоб уникнути LazyInitializationException в асинхронному контексті
     */
    @Query("""
        select esw
          from EventSubscriptionWhatsapp esw
          left join fetch esw.userWhatsapp
          left join fetch esw.event
         where esw.event.startAt > :minTime
           and esw.event.startAt < :maxTime
           and esw.active = true
           and esw.reminder24hSent = false
           and esw.userWhatsapp.waId is not null
    """)
    List<EventSubscriptionWhatsapp> findForReminder24h(
        @Param("minTime") java.time.LocalDateTime minTime,
        @Param("maxTime") java.time.LocalDateTime maxTime
    );

    /**
     * Скидає прапорці нагадування для всіх підписок на заданий івент.
     * Викликається, коли час початку івенту змінюється.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update EventSubscriptionWhatsapp esw
           set esw.reminder72hSent = false,
               esw.reminder24hSent = false
         where esw.event.id = :eventId
    """)
    int resetRemindersForEvent(@Param("eventId") Long eventId);
}
