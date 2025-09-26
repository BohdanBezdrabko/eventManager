package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.dto.UserRegistrationDto;
import com.example.sportadministrationsystem.model.UserRegistration;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRegistrationRepository extends JpaRepository<UserRegistration, Long> {

    @Query("""
           select new com.example.sportadministrationsystem.dto.UserRegistrationDto(
               ur.id, u.id, u.username, e.id, e.name, ur.registrationDate
           )
           from UserRegistration ur
             join ur.user u
             join ur.event e
           where u.id = :userId
           """)
    List<UserRegistrationDto> findDtosByUserId(@Param("userId") Long userId);

    @Query("""
           select new com.example.sportadministrationsystem.dto.UserRegistrationDto(
               ur.id, u.id, u.username, e.id, e.name, ur.registrationDate
           )
           from UserRegistration ur
             join ur.user u
             join ur.event e
           where e.id = :eventId
           """)
    List<UserRegistrationDto> findDtosByEventId(@Param("eventId") Long eventId);

    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);
    long countByEvent_Id(Long eventId);
    Optional<UserRegistration> findByUser_IdAndEvent_Id(Long userId, Long eventId);
}
