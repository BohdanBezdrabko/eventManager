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

    Optional<EventSubscription> findByEventAndUserAndMessenger(Event event, User user, Messenger messenger);

    boolean existsByEventAndUserAndMessengerAndActiveIsTrue(Event event, User user, Messenger messenger);

    // Потрібно для PostDispatchService — отримати chatId активних підписників на подію
    @Query(value = """
            select ut.tg_chat_id
            from event_subscriptions es
            join users u on u.id = es.user_id
            join user_telegram ut on ut.user_id = u.id
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
}
