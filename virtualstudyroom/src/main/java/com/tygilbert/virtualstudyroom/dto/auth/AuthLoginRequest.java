package com.tygilbert.virtualstudyroom.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}