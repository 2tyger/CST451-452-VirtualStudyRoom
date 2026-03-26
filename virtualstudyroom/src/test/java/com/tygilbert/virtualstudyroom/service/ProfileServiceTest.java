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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.profile.ProfileResponse;
import com.tygilbert.virtualstudyroom.dto.profile.UpdateProfileRequest;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void updateMyProfile_rotatesPasswordWhenProvided() {
        User user = new User();
        user.setId(2L);
        user.setEmail("student@example.com");
        user.setDisplayName("Student");
        user.setPasswordHash("old");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateMyProfile(
                "student@example.com",
                new UpdateProfileRequest("Updated", "student@example.com", "bio", "new-password-123")
        );

        assertEquals("new-hash", user.getPasswordHash());
        assertEquals("Updated", response.displayName());
        assertEquals("bio", response.bio());
    }

    @Test
    void updateMyProfile_throwsConflictWhenEmailAlreadyInUse() {
        User current = new User();
        current.setId(2L);
        current.setEmail("student@example.com");

        User existing = new User();
        existing.setId(3L);
        existing.setEmail("taken@example.com");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(current));
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> profileService.updateMyProfile(
                        "student@example.com",
                        new UpdateProfileRequest("Updated", "taken@example.com", "bio", null)
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void updateMyProfile_normalizesAndTrimsEmail() {
        User user = new User();
        user.setId(2L);
        user.setEmail("student@example.com");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileService.updateMyProfile(
                "student@example.com",
                new UpdateProfileRequest("Updated", "  NEW@EXAMPLE.COM  ", "bio", null)
        );

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("new@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void getMyProfile_returnsCurrentUserData() {
        User user = new User();
        user.setId(5L);
        user.setEmail("student@example.com");
        user.setDisplayName("Ty");
        user.setBio("bio");

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        ProfileResponse response = profileService.getMyProfile("student@example.com");

        assertEquals(5L, response.userId());
        assertEquals("student@example.com", response.email());
        assertEquals("Ty", response.displayName());
        assertEquals("bio", response.bio());
    }
}
