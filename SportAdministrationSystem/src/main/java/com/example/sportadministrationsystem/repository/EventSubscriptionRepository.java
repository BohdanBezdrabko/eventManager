package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, Long> {

    Optional<EventSubscription> findByEventAndUserTelegramAndMessenger(
            com.example.sportadministrationsystem.model.Event event,
            com.example.sportadministrationsystem.model.UserTelegram userTelegram,
            Messenger messenger
    );

    boolean existsByEventAndUserTelegramAndMessengerAndActiveIsTrue(
            com.example.sportadministrationsystem.model.Event event,
            com.example.sportadministrationsystem.model.UserTelegram userTelegram,
            Messenger messenger
    );

    @Query(value = """
        select ut.tg_chat_id
        from event_subscriptions es
        join user_telegram ut on ut.id = es.user_telegram_id
        where es.event_id = :eventId
          and es.messenger = :messenger
          and es.active = true
          and ut.tg_chat_id is not null
    """, nativeQuery = true)
    List<Long> findSubscriberChatIds(@Param("eventId") Long eventId,
                                     @Param("messenger") String messenger);

    default List<Long> findSubscriberChatIds(Long eventId, Messenger messenger) {
        return findSubscriberChatIds(eventId, messenger.name());
    }

    // === НОВЕ: підрахунок кількості активних Telegram-підписок з валідним chat_id ===
    @Query(value = """
        select count(*) 
        from event_subscriptions es
        join user_telegram ut on ut.id = es.user_telegram_id
        where es.event_id = :eventId
          and es.messenger = :messenger
          and es.active = true
          and ut.tg_chat_id is not null
    """, nativeQuery = true)
    long countActiveByEventAndMessengerWithChat(
            @Param("eventId") Long eventId,
            @Param("messenger") String messenger
    );

    default long countActiveTelegram(Long eventId) {
        return countActiveByEventAndMessengerWithChat(eventId, Messenger.TELEGRAM.name());
    }
}
