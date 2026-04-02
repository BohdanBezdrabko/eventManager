package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.UserTelegram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторій керування підписками на івенти.
 *
 * ВАЖЛИВО: методи, позначені @Modifying, мають виконуватись в активній транзакції.
 * Транзакцію відкриває сервісний шар (див. EventSubscriptionService.toggleSubscription).
 */
public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, Long> {

    /**
     * Знаходить підписку за ключем (event + userTelegram).
     */
    Optional<EventSubscription> findByEventAndUserTelegram(
            Event event,
            UserTelegram userTelegram
    );

    /**
     * Перевіряє, чи існує активна підписка.
     */
    boolean existsByEventAndUserTelegramAndActiveIsTrue(
            Event event,
            UserTelegram userTelegram
    );

    /**
     * Усі активні підписки для івенту.
     */
    List<EventSubscription> findAllByEvent_IdAndActiveIsTrue(Long eventId);

    /**
     * Повертає кількість активних підписників з НЕПУСТИМ tg_chat_id для конкретного івенту.
     */
    @Query("""
        select count(distinct ut.tgChatId)
          from EventSubscription es
          join es.userTelegram ut
         where es.event.id = :eventId
           and es.active = true
           and ut.tgChatId is not null
        """)
    long countActiveTelegram(@Param("eventId") Long eventId);

    /**
     * Унікальні chatId підписників для події.
     * Повертаються лише активні підписки із наявним tgChatId.
     */
    @Query("""
        select distinct es.userTelegram.tgChatId
          from EventSubscription es
         where es.event.id = :eventId
           and es.active = true
           and es.userTelegram.tgChatId is not null
        """)
    List<Long> findSubscriberChatIds(@Param("eventId") Long eventId);

    /**
     * Реактивація існуючої (неактивної) підписки.
     * Повертає кількість змінених рядків.
     *
     * УВАГА: викликати лише в межах відкритої транзакції.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update EventSubscription es
           set es.active = true
         where es.event = :event
           and es.userTelegram = :user
           and es.active = false
        """)
    int reactivate(@Param("event") Event event,
                   @Param("user") UserTelegram user);

    /**
     * Деактивація активної підписки.
     * Повертає кількість змінених рядків.
     *
     * УВАГА: викликати лише в межах відкритої транзакції.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update EventSubscription es
           set es.active = false
         where es.event = :event
           and es.userTelegram = :user
           and es.active = true
        """)
    int deactivate(@Param("event") Event event,
                   @Param("user") UserTelegram user);
}
