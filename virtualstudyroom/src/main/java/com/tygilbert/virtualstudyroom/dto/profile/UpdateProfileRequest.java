package com.tygilbert.virtualstudyroom.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 100) String displayName,
        @NotBlank @Email String email,
        @Size(max = 500) String bio,
        @Size(min = 8, max = 100) String newPassword
) {
}