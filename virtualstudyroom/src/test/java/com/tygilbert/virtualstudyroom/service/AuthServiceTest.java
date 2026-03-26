/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.auth.AuthLoginRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthRegisterRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthResponse;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.UserRepository;
import com.tygilbert.virtualstudyroom.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_normalizesEmailAndReturnsToken() {
        AuthRegisterRequest request = new AuthRegisterRequest("Student@Example.com", "password123", "Ty");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtService.generateToken("student@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("student@example.com", userCaptor.getValue().getEmail());
        assertEquals("Ty", userCaptor.getValue().getDisplayName());
        assertEquals("hashed", userCaptor.getValue().getPasswordHash());

        assertEquals("jwt-token", response.token());
        assertEquals(42L, response.userId());
        assertEquals("student@example.com", response.email());
        assertEquals("Ty", response.displayName());
    }

    @Test
    void register_throwsConflictWhenEmailExists() {
        AuthRegisterRequest request = new AuthRegisterRequest("student@example.com", "password123", "Ty");
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(new User()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void login_throwsUnauthorizedOnInvalidPassword() {
        User user = new User();
        user.setId(5L);
        user.setEmail("student@example.com");
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new AuthLoginRequest("student@example.com", "wrong-password"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_returnsTokenForValidCredentials() {
        User user = new User();
        user.setId(6L);
        user.setEmail("student@example.com");
        user.setDisplayName("Student");
        user.setPasswordHash("hashed");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("student@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(new AuthLoginRequest("Student@Example.com", "password123"));

        assertEquals("jwt-token", response.token());
        assertEquals(6L, response.userId());
        assertEquals("student@example.com", response.email());
        assertEquals("Student", response.displayName());
    }

    @Test
    void login_throwsUnauthorizedWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(new AuthLoginRequest("missing@example.com", "password123"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}


