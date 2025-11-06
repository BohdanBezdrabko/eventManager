package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
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
     * Знаходить підписку за ключем (event + userTelegram + messenger).
     */
    Optional<EventSubscription> findByEventAndUserTelegramAndMessenger(
            Event event,
            UserTelegram userTelegram,
            Messenger messenger
    );

    /**
     * Перевіряє, чи існує активна підписка.
     */
    boolean existsByEventAndUserTelegramAndMessengerAndActiveIsTrue(
            Event event,
            UserTelegram userTelegram,
            Messenger messenger
    );

    /**
     * Усі активні підписки для івенту.
     */
    List<EventSubscription> findAllByEvent_IdAndActiveIsTrue(Long eventId);

    /**
     * Повертає кількість активних підписників з НЕПУСТИМ tg_chat_id
     * для конкретного івенту та месенджера.
     *
     * ВАЖЛИВО: переведено на JPQL, щоб коректно біндити Enum Messenger і уникнути 500.
     * Додаємо DISTINCT по tgChatId, щоб не рахувати дублікати.
     */
    @Query("""
        select count(distinct ut.tgChatId)
          from EventSubscription es
          join es.userTelegram ut
         where es.event.id = :eventId
           and es.messenger = :messenger
           and es.active = true
           and ut.tgChatId is not null
        """)
    long countActiveByEventAndMessengerWithChat(
            @Param("eventId") Long eventId,
            @Param("messenger") Messenger messenger
    );

    /**
     * Зручний шорткат для TELEGRAM.
     */
    default long countActiveTelegram(Long eventId) {
        return countActiveByEventAndMessengerWithChat(eventId, Messenger.TELEGRAM);
    }

    /**
     * Унікальні chatId підписників для події та конкретного месенджера.
     * Повертаються лише активні підписки із наявним tgChatId.
     */
    @Query("""
        select distinct es.userTelegram.tgChatId
          from EventSubscription es
         where es.event.id = :eventId
           and es.messenger = :messenger
           and es.active = true
           and es.userTelegram.tgChatId is not null
        """)
    List<Long> findSubscriberChatIds(@Param("eventId") Long eventId,
                                     @Param("messenger") Messenger messenger);

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
           and es.messenger = :messenger
           and es.active = false
        """)
    int reactivate(@Param("event") Event event,
                   @Param("user") UserTelegram user,
                   @Param("messenger") Messenger messenger);

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
           and es.messenger = :messenger
           and es.active = true
        """)
    int deactivate(@Param("event") Event event,
                   @Param("user") UserTelegram user,
                   @Param("messenger") Messenger messenger);
}
