/*
defines websocket event contracts and payload shapes used by realtime messaging
*/
package com.tygilbert.virtualstudyroom.dto.ws;

import java.time.OffsetDateTime;

public record RoomEventDto<T>(
        RealtimeEventType type,
        Long roomId,
        OffsetDateTime timestamp,
        T payload
) {
}

