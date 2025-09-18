package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.UserRegistration;
import com.example.sportadministrationsystem.repository.UserRegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRegistrationService {

    private final UserRegistrationRepository repository;

    public UserRegistrationService(UserRegistrationRepository repository) {
        this.repository = repository;
    }

    public List<UserRegistration> getAll() {
        return repository.findAll();
    }

    public List<Event> getEventsByUserId(Long userId) {
        return repository.findByUser_Id(userId)
                .stream()
                .map(UserRegistration::getEvent)
                .collect(Collectors.toList());
    }

    public UserRegistration create(UserRegistration userRegistration) {
        return repository.save(userRegistration);
    }

    public UserRegistration update(Long id, UserRegistration updated) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setUser(updated.getUser());
                    existing.setEvent(updated.getEvent());
                    existing.setRegistrationDate(updated.getRegistrationDate());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("UserRegistration not found with id " + id));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
