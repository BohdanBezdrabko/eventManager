package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.UserRegistrationDto;
import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserRegistration;
import com.example.sportadministrationsystem.repository.EventRepository;
import com.example.sportadministrationsystem.repository.UserRegistrationRepository;
import com.example.sportadministrationsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock UserRegistrationRepository registrations;
    @Mock UserRepository users;
    @Mock EventRepository events;

    @InjectMocks UserRegistrationService service;

    @Test
    @DisplayName("forEvent делегує у репозиторій і повертає список DTO")
    void forEvent_ok() {
        when(registrations.findDtosByEventId(42L))
                .thenReturn(List.of(
                        new UserRegistrationDto(1L, 10L, "u1", 42L, "E", java.time.LocalDate.now())
                ));

        List<UserRegistrationDto> list = service.forEvent(42L);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).id());
        verify(registrations).findDtosByEventId(42L);
        verifyNoMoreInteractions(registrations, users, events);
    }

    @Nested
    class Cancel {
        @Test
        @DisplayName("cancel знаходить реєстрацію і видаляє її; також перевіряє існування події з блокуванням")
        void cancel_ok() {
            var reg = UserRegistration.builder().id(5L).build();
            when(registrations.findByUser_IdAndEvent_Id(10L, 99L)).thenReturn(Optional.of(reg));
            when(events.findByIdForUpdate(99L)).thenReturn(Optional.of(new Event()));

            service.cancel(10L, 99L);

            verify(registrations).findByUser_IdAndEvent_Id(10L, 99L);
            verify(events).findByIdForUpdate(99L);
            verify(registrations).delete(reg);
            verifyNoMoreInteractions(registrations, events);
            verifyNoInteractions(users);
        }

        @Test
        @DisplayName("cancel кидає IllegalArgumentException, якщо реєстрацію не знайдено")
        void cancel_registrationMissing() {
            when(registrations.findByUser_IdAndEvent_Id(10L, 99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.cancel(10L, 99L));

            verify(registrations).findByUser_IdAndEvent_Id(10L, 99L);
            verifyNoMoreInteractions(registrations);
            verifyNoInteractions(users, events);
        }

        @Test
        @DisplayName("cancel кидає IllegalArgumentException, якщо подію не знайдено")
        void cancel_eventMissing() {
            var reg = UserRegistration.builder().id(5L).build();
            when(registrations.findByUser_IdAndEvent_Id(10L, 99L)).thenReturn(Optional.of(reg));
            when(events.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> service.cancel(10L, 99L));

            verify(registrations).findByUser_IdAndEvent_Id(10L, 99L);
            verify(events).findByIdForUpdate(99L);
            // delete не викликається
            verify(registrations, never()).delete(any());
        }
    }
}
