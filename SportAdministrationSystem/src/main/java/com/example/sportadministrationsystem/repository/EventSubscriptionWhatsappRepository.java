package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscriptionWhatsapp;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
