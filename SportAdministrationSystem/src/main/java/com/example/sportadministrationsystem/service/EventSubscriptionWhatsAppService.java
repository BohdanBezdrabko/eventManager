package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscriptionWhatsapp;
import com.example.sportadministrationsystem.model.UserWhatsapp;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionWhatsappRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSubscriptionWhatsAppService {

    private final EventRepository events;
    private final EventSubscriptionWhatsappRepository subs;

    @Transactional
    public boolean toggleSubscription(long eventId, UserWhatsapp waAcc, boolean desired) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        Optional<EventSubscriptionWhatsapp> found = subs.findByEventAndUserWhatsapp(event, waAcc);

        if (desired) {
            if (found.isEmpty()) {
                subs.saveAndFlush(EventSubscriptionWhatsapp.builder()
                        .event(event)
                        .userWhatsapp(waAcc)
                        .active(true)
                        .build());
                return true;
            }
            if (!found.get().isActive()) {
                EventSubscriptionWhatsapp es = found.get();
                es.setActive(true);
                subs.saveAndFlush(es);
            }
            return true;
        } else {
            if (found.isPresent() && found.get().isActive()) {
                EventSubscriptionWhatsapp es = found.get();
                es.setActive(false);
                subs.saveAndFlush(es);
            }
            return false;
        }
    }
}
