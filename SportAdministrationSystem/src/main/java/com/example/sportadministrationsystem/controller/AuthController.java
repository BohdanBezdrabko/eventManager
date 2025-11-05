package com.example.sportadministrationsystem.controller;

import com.example.sportadministrationsystem.dto.RegisterRequest;
import com.example.sportadministrationsystem.model.Role;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.UserRepository;
import com.example.sportadministrationsystem.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
/**
 * REST controller responsible for authentication and user registration.
 *
 * <p>Exposes endpoints under {@code /api/v1/auth} for:
 * <ul>
 *   <li>Registering new users</li>
 *   <li>Logging in and obtaining JWT tokens</li>
 *   <li>Inspecting the currently authenticated user</li>
 * </ul>
 *
 * <p>This controller delegates persistence to {@code UserRepository}, password encoding to
 * {@code PasswordEncoder}, authentication to {@code AuthenticationManager} and JWT issuance to
 * {@code JwtTokenProvider}.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ======== DTOs ========
    public record LoginRequest(String username, String password) {}
    public record JwtResponse(String accessToken, String refreshToken, UserMeResponse user) {}
    public record UserMeResponse(Long id, String username, Set<String> roles) {}

    // ======== Helpers ========

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

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

        User domainUser = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        return new JwtResponse(accessToken, refreshToken, toUserMe(domainUser));
    }

    // ======== Endpoints ========

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

        // Одразу логінимо і повертаємо {tokens, user}
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

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(Principal principal) {
        if (principal == null || isBlank(principal.getName())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toUserMe(user));
    }
}
