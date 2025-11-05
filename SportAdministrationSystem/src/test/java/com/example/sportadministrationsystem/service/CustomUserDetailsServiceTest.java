package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.model.Role;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService makeService() {
        CustomUserDetailsService s = new CustomUserDetailsService();
        // у вихідному коді залежність анотована @Autowired, тому сетнемо через ReflectionTestUtils
        ReflectionTestUtils.setField(s, "userRepository", userRepository);
        return s;
    }

    @Test
    @DisplayName("loadUserByUsername мапить ролі у GrantedAuthority і повертає UserDetails")
    void loadUser_ok() {
        var user = User.builder()
                .id(1L)
                .username("kate")
                .password("secret")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                .build();

        when(userRepository.findByUsername("kate")).thenReturn(Optional.of(user));

        UserDetails details = makeService().loadUserByUsername("kate");

        assertEquals("kate", details.getUsername());
        assertEquals("secret", details.getPassword());

        var authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"), authorities);

        verify(userRepository).findByUsername("kate");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Кидає UsernameNotFoundException, якщо користувача немає")
    void loadUser_notFound() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> makeService().loadUserByUsername("nope"));
        verify(userRepository).findByUsername("nope");
        verifyNoMoreInteractions(userRepository);
    }
}
