/*
defines timer state payload returned by timer endpoints and realtime updates
*/
package com.tygilbert.virtualstudyroom.dto.timer;

import java.time.Instant;

public record TimerStateResponse(
        boolean isRunning,
        long elapsedSeconds,
        Instant startTime,
        String phase,
        long phaseDurationSeconds,
        long remainingSeconds
) {
}

