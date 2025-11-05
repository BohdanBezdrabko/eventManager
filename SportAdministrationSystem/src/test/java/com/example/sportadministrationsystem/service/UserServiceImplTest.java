package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.dto.UserInfoDTO;
import com.example.sportadministrationsystem.model.Role;
import com.example.sportadministrationsystem.model.User;
import com.example.sportadministrationsystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private SecurityContext original;

    @BeforeEach
    void setUp() {
        // Preserve original context
        original = SecurityContextHolder.getContext();
    }

    @AfterEach
    void tearDown() {
        // Clear context so tests are isolated
        SecurityContextHolder.clearContext();
        if (original != null) {
            SecurityContextHolder.setContext(original);
        }
    }

    @Test
    @DisplayName("Повертає інформацію про поточного юзера з SecurityContextHolder")
    void getCurrentUserInfo_ok() {
        // given
        var auth = new UsernamePasswordAuthenticationToken("alice", "N/A");
        SecurityContextHolder.getContext().setAuthentication(auth);

        var user = User.builder()
                .id(10L)
                .username("alice")
                .password("hash")
                .roles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER))
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        // when
        UserInfoDTO dto = userService.getCurrentUserInfo();

        // then
        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals("alice", dto.username());
        assertEquals(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER), dto.roles());

        verify(userRepository).findByUsername("alice");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Кидає RuntimeException, якщо користувача не знайдено в репозиторії")
    void getCurrentUserInfo_userNotFound() {
        // given
        var auth = new UsernamePasswordAuthenticationToken("ghost", "N/A");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // when / then
        assertThrows(RuntimeException.class, () -> userService.getCurrentUserInfo());
        verify(userRepository).findByUsername("ghost");
        verifyNoMoreInteractions(userRepository);
    }
}
