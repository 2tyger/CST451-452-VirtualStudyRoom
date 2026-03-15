package com.tygilbert.virtualstudyroom.dto.profile;

import java.time.OffsetDateTime;

public record ProfileResponse(
        Long userId,
        String email,
        String displayName,
        String bio,
        OffsetDateTime createdAt
) {
}