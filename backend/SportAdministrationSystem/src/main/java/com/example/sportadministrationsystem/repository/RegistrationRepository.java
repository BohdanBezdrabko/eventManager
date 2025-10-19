package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.model.UserRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Repository
public interface RegistrationRepository extends JpaRepository<UserRegistration, Long> {

    // Spring Data ім'я працює, бо в UserRegistration є поля `event` та `user`
    boolean existsByEventAndUser(Event event, User user);

    @Modifying
    @Transactional
    @Query("delete from UserRegistration r where r.event = :event and r.user = :user")
    int deleteByEventAndUser(Event event, User user);

    default boolean deleteIfExists(Event e, User u) {
        return deleteByEventAndUser(e, u) > 0;
    }

    @Query("select count(r.id) from UserRegistration r where r.event.id = :eventId")
    int countByEventId(Long eventId);

    default UserRegistration create(Event e, User u) {
        UserRegistration r = UserRegistration.builder()
                .event(e)
                .user(u)
                .registrationDate(LocalDate.now())
                .build();
        return save(r);
    }
}
