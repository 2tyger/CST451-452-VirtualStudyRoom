/*
defines message response contracts for room chat history and send operations
*/
package com.tygilbert.virtualstudyroom.dto.message;

import java.time.OffsetDateTime;

public record MessageResponse(
        Long id,
        Long roomId,
        Long userId,
        String sender,
        String body,
        OffsetDateTime createdAt
) {
}

