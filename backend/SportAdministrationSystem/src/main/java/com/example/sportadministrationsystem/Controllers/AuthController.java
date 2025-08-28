package com.example.sportadministrationsystem.Controllers;

import com.example.sportadministrationsystem.Dtos.RegisterRequest;
import com.example.sportadministrationsystem.Models.Role;
import com.example.sportadministrationsystem.Models.User;
import com.example.sportadministrationsystem.Repositories.UserRepository;
import com.example.sportadministrationsystem.Configs.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ======== DTOs ========
    public record LoginRequest(String username, String password) {}
    public record JwtResponse(String token, UserMeResponse user) {}
    public record UserMeResponse(Long id, String username, Set<String> roles) {}

    // ======== Helpers ========

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Мапінг вхідного role на Role enum:
     * "creator" -> ROLE_ADMIN
     * "participant" -> ROLE_USER
     * інші/порожнє -> ROLE_USER
     */
    private Role mapRequestedRole(String raw) {
        if (raw == null) return Role.ROLE_USER;
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "creator", "admin", "event_creator", "eventmanager", "event_manager" -> Role.ROLE_ADMIN;
            case "participant", "user", "member" -> Role.ROLE_USER;
            default -> Role.ROLE_USER;
        };
    }

    private UserMeResponse toUserMe(User u) {
        Set<String> roleNames = u.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
        return new UserMeResponse(u.getId(), u.getUsername(), roleNames);
    }

    private JwtResponse buildJwtResponse(Authentication auth) {
        var principal = (org.springframework.security.core.userdetails.User) auth.getPrincipal();

        Set<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String token = jwtTokenProvider.createToken(principal.getUsername(), roles);

        User domainUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        return new JwtResponse(token, toUserMe(domainUser));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        log.info("RegisterRequest: username='{}', role='{}'", req.username(), req.role());

        if (isBlank(req.username()) || isBlank(req.password()) || isBlank(req.role())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("username, password and role are required");
        }

        if (userRepository.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username is already taken");
        }

        Role role = mapRequestedRole(req.role());

        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        // Перевірка, що роль збережена
        User saved = userRepository.findByUsername(req.username()).orElse(null);
        if (saved == null || saved.getRoles() == null || !saved.getRoles().contains(role)) {
            log.error("Persisted roles do not contain expected role {} for user {}", role, req.username());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Role was not persisted as expected");
        }

        // Одразу логінимо і повертаємо {token, user}
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        JwtResponse jwt = buildJwtResponse(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(jwt);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        return ResponseEntity.ok(buildJwtResponse(auth));
    }

    @GetMapping("/user/me")
    public ResponseEntity<UserMeResponse> me(Principal principal) {
        if (principal == null || isBlank(principal.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(principal.getName())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toUserMe(user));
    }
}
