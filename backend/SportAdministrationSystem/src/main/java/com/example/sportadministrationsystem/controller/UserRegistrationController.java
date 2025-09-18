package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.model.Event;
import com.example.sportadministrationsystem.model.UserRegistration;
import com.example.sportadministrationsystem.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/registrations")
public class UserRegistrationController {

    private final UserRegistrationService service;

    // Отримати всі реєстрації
    @GetMapping
    public ResponseEntity<List<UserRegistration>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // Отримати всі події користувача
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Event>> getEventsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getEventsByUserId(userId));
    }

    // Створити нову реєстрацію
    @PostMapping
    public ResponseEntity<UserRegistration> create(@RequestBody UserRegistration userRegistration) {
        return ResponseEntity.ok(service.create(userRegistration));
    }

    // Оновити реєстрацію
    @PutMapping("/{id}")
    public ResponseEntity<UserRegistration> update(@PathVariable Long id,
                                                   @RequestBody UserRegistration updated) {
        try {
            return ResponseEntity.ok(service.update(id, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Видалити реєстрацію
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
