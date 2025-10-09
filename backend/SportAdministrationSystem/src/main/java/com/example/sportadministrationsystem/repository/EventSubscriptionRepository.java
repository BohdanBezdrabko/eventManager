package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, Long> {
    Optional<EventSubscription> findByEventAndUserAndMessenger(Event event, User user, Messenger messenger);

    boolean existsByEventAndUserAndMessengerAndActiveIsTrue(Event event, User user, Messenger messenger);

    @Query("""
        select ut.tgChatId
        from EventSubscription es
          join es.user u
          join UserTelegram ut on ut.user = u
        where es.event = :event
          and es.messenger = :messenger
          and es.active = true
        """)
    List<Long> findSubscriberChatIds(@Param("event") Event event, @Param("messenger") Messenger messenger);
}
