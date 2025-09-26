package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.UserRegistrationDto;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserRegistration;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.UserRegistrationRepository;
import com.example.sportadministrationsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class UserRegistrationService {

    private final UserRegistrationRepository registrations;
    private final EventRepository events;
    private final UserRepository users;

    public UserRegistrationService(UserRegistrationRepository registrations,
                                   EventRepository events,
                                   UserRepository users) {
        this.registrations = registrations;
        this.events = events;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<UserRegistrationDto> forUser(Long userId) {
        return registrations.findDtosByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<UserRegistrationDto> forEvent(Long eventId) {
        return registrations.findDtosByEventId(eventId);
    }

    @Transactional
    public UserRegistrationDto register(Long userId, Long eventId) {
        if (registrations.existsByUser_IdAndEvent_Id(userId, eventId)) {
            throw new IllegalStateException("Ви вже зареєстровані на цю подію");
        }

        Event event = events.findByIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Подію не знайдено"));

        if (event.getCapacity() != null && event.getCapacity() > 0) {
            int taken = event.getRegisteredCount() == null ? 0 : event.getRegisteredCount();
            if (taken >= event.getCapacity()) {
                throw new IllegalStateException("Місць більше немає");
            }
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Користувача не знайдено"));

        UserRegistration saved = registrations.save(
                UserRegistration.builder()
                        .user(user)
                        .event(event)
                        .registrationDate(LocalDate.now())
                        .build()
        );

        // Inline DTO. Entities are attached so fields are available.
        return new UserRegistrationDto(
                saved.getId(),
                user.getId(),
                user.getUsername(),
                event.getId(),
                event.getName(),
                saved.getRegistrationDate()
        );
    }

    @Transactional
    public void cancel(Long userId, Long eventId) {
        var reg = registrations.findByUser_IdAndEvent_Id(userId, eventId)
                .orElseThrow(() -> new IllegalArgumentException("Реєстрацію не знайдено"));

        events.findByIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Подію не знайдено"));

        registrations.delete(reg);
    }
}
