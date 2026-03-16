/*
contains business logic for this domain and coordinates repository operations
*/
package com.tygilbert.virtualstudyroom.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.profile.ProfileResponse;
import com.tygilbert.virtualstudyroom.dto.profile.UpdateProfileRequest;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.UserRepository;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    // loads profile data for the authenticated user
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ProfileResponse getMyProfile(String email) {
        User user = getCurrentUser(email);
        return toResponse(user);
    }
        // updates profile fields and optionally rotates password

    public ProfileResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = getCurrentUser(email);

        String normalizedEmail = request.email().trim().toLowerCase();
        if (!normalizedEmail.equals(user.getEmail())) {
            userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            });
        }

        user.setDisplayName(request.displayName().trim());
        user.setEmail(normalizedEmail);
        user.setBio(request.bio() == null ? "" : request.bio().trim());

        String newPassword = request.newPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

    // resolves user entity by normalized email
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
    // maps user entity to profile response payload
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user context"));
    }

    private ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
            user.getBio(),
                user.getCreatedAt()
        );
    }
}

