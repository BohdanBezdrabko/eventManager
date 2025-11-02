package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.UserRegistrationDto;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final UserRegistrationService service;
    private final UserRepository users;

    private Long requireUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("UNAUTHORIZED");
        }
        User u = users.findByUsername(principal.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("UNAUTHORIZED"));
        return u.getId();
    }

    @PostMapping("/my/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registerMy(@PathVariable Long eventId, Principal principal) {
        Long userId = requireUserId(principal);
        try {
            UserRegistrationDto reg = service.register(userId, eventId);
            return ResponseEntity.status(HttpStatus.CREATED).body(reg);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @DeleteMapping("/my/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelMy(@PathVariable Long eventId, Principal principal) {
        Long userId = requireUserId(principal);
        try {
            service.cancel(userId, eventId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserRegistrationDto>> my(Principal principal) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok(service.forUser(userId));
    }

    @GetMapping("/user-id/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserRegistrationDto>> byUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.forUser(userId));
    }

    @GetMapping("/event-id/{eventId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserRegistrationDto>> byEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(service.forEvent(eventId));
    }
}
