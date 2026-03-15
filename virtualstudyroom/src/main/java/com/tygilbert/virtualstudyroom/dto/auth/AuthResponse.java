package com.tygilbert.virtualstudyroom.dto.auth;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String displayName
) {
}