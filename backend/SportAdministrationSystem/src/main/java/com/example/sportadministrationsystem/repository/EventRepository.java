package com.example.sportadministrationsystem.repository;

import com.example.sportadministrationsystem.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByName(String name);
    Optional<Event> findByLocation(String Location);
    List<Event> findAllByLocationIgnoreCase(String location);
    List<Event> findAllByNameIgnoreCase(String name);
}
