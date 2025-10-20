package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
public interface RegistrationRepository extends JpaRepository<UserRegistration, Long> {

    boolean existsByEventAndUser(Event event, User user);

    @Modifying
    @Transactional
    @Query("delete from UserRegistration r where r.event = :event and r.user = :user")
    int deleteByEventAndUser(@Param("event") Event event, @Param("user") User user);

    @Query("select count(r.id) from UserRegistration r where r.event.id = :eventId")
    int countByEventId(@Param("eventId") Long eventId);

    default UserRegistration create(Event event, User user) {
        UserRegistration r = UserRegistration.builder()
                .event(event)
                .user(user)
                .registrationDate(LocalDate.now())
                .build();
        return save(r);
    }
}
