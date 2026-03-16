/*
contains business logic for this domain and coordinates repository operations
*/
package com.tygilbert.virtualstudyroom.service;

import com.tygilbert.virtualstudyroom.dto.auth.AuthLoginRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthRegisterRequest;
import com.tygilbert.virtualstudyroom.dto.auth.AuthResponse;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.UserRepository;
import com.tygilbert.virtualstudyroom.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // registers a user account and returns jwt session data
    public AuthResponse register(AuthRegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        });

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);

        return new AuthResponse(
                jwtService.generateToken(saved.getEmail()),
                saved.getId(),
                saved.getEmail(),
                saved.getDisplayName()
        );
    }

    // validates credentials and returns jwt session data
    public AuthResponse login(AuthLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }
}

