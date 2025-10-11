package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, Long> {

    /**
     * Пошук підписки за (event, user, messenger).
     * Використовується у EventSubscriptionController:
     *   - subscribe(): findByEventAndUserAndMessenger(...)
     *   - unsubscribe(): findByEventAndUserAndMessenger(...)
     */
    Optional<EventSubscription> findByEventAndUserAndMessenger(Event event,
                                                               User user,
                                                               Messenger messenger);

    /**
     * Перевірка активної підписки (для /my).
     * Використовується у EventSubscriptionController#myStatus(...)
     */
    boolean existsByEventAndUserAndMessengerAndActiveIsTrue(Event event,
                                                            User user,
                                                            Messenger messenger);

    /**
     * Повертає tg_chat_id усіх (АКТИВНИХ) підписників івенту для конкретного месенджера.
     *
     * У запиті є CAST(:messenger AS messenger) — це потрібно, якщо у БД колонка
     * event_subscriptions.messenger має тип PostgreSQL enum "messenger"
     * (створений у Flyway-міграції).
     *
     * Якщо ж у БД це VARCHAR, заміни рядок
     *   "and es.messenger = CAST(:messenger AS messenger)"
     * на
     *   "and es.messenger = :messenger"
     */
    @Query(value = """
        select ut.tg_chat_id
        from event_subscriptions es
        join users u  on u.id = es.user_id
        join user_telegram ut on ut.user_id = u.id
        where es.event_id = :eventId
          and es.messenger = CAST(:messenger AS messenger)
          and es.active = true
          and ut.tg_chat_id is not null
        """, nativeQuery = true)
    List<Long> findSubscriberChatIds(@Param("eventId") Long eventId,
                                     @Param("messenger") String messenger);
}
