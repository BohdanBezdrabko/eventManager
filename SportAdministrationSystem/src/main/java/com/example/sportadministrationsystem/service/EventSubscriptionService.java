package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.EventSubscription;
import com.example.sportadministrationsystem.model.Messenger;
import com.example.sportadministrationsystem.model.UserTelegram;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.EventSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSubscriptionService {

    private final EventRepository events;
    private final EventSubscriptionRepository subs;

    /**
     * Увімкнути/вимкнути підписку для конкретного івенту/юзера в межах єдиної транзакції.
     * Повертає true, якщо після виклику підписка активна; false — якщо деактивована.
     */
    @Transactional
    public boolean toggleSubscription(long eventId, UserTelegram tgAcc, boolean desired) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        Optional<EventSubscription> found =
                subs.findByEventAndUserTelegramAndMessenger(event, tgAcc, Messenger.TELEGRAM);

        if (desired) {
            if (found.isEmpty()) {
                EventSubscription es = new EventSubscription();
                es.setEvent(event);
                es.setUserTelegram(tgAcc);
                es.setMessenger(Messenger.TELEGRAM);
                es.setActive(true);
                subs.saveAndFlush(es);
                return true;
            } else if (!found.get().isActive()) {
                int updated = subs.reactivate(event, tgAcc, Messenger.TELEGRAM);
                if (updated == 0) {
                    // fallback на entity update (якщо update-query нічого не змінила)
                    EventSubscription es = found.get();
                    es.setActive(true);
                    subs.saveAndFlush(es);
                }
                return true;
            } else {
                // уже активна
                return true;
            }
        } else {
            if (found.isPresent() && found.get().isActive()) {
                int updated = subs.deactivate(event, tgAcc, Messenger.TELEGRAM);
                if (updated == 0) {
                    EventSubscription es = found.get();
                    es.setActive(false);
                    subs.saveAndFlush(es);
                }
            }
            return false;
        }
    }
}
