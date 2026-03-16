/*
defines websocket event contracts and payload shapes used by realtime messaging
*/
package com.tygilbert.virtualstudyroom.dto.ws;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TimerUpdatePayload(
        boolean isRunning,
        long elapsedSeconds,
        Instant startTime,
        String phase,
        long phaseDurationSeconds,
        long remainingSeconds
) {
}

