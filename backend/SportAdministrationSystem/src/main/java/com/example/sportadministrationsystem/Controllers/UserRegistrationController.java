package com.example.sportadministrationsystem.Controllers;

import com.example.sportadministrationsystem.Models.Event;
import com.example.sportadministrationsystem.Models.UserRegistration;
import com.example.sportadministrationsystem.Services.UserRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/registration")
public class UserRegistrationController {

    private final UserRegistrationService service;

    public UserRegistrationController(UserRegistrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserRegistration> getAll() {
        return service.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<Event> getEventsByUserId(@PathVariable Long userId) {
        return service.getEventsByUserId(userId);
    }

    @PostMapping
    public UserRegistration create(@RequestBody UserRegistration userRegistration) {
        return service.create(userRegistration);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserRegistration> update(@PathVariable Long id,
                                                   @RequestBody UserRegistration updated) {
        try {
            return ResponseEntity.ok(service.update(id, updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
