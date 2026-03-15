package com.tygilbert.virtualstudyroom.dto.room;

import java.time.Instant;
import java.time.OffsetDateTime;

public record RoomResponse(
        Long id,
        String name,
        Long ownerId,
        long memberCount,
        boolean active,
        boolean breakPhase,
        boolean isRunning,
        long elapsedSeconds,
        Instant startTime,
        OffsetDateTime createdAt
) {
}