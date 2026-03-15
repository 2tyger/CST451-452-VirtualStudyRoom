package com.tygilbert.virtualstudyroom.dto.task;

import java.time.OffsetDateTime;

public record TaskResponse(
        Long id,
        Long roomId,
        String title,
        String description,
        Long assigneeId,
        boolean done,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}