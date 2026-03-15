package com.tygilbert.virtualstudyroom.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.timer.TimerStateResponse;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@Service
public class TimerService {

    private static final String FOCUS_PHASE = "FOCUS";
    private static final String BREAK_PHASE = "BREAK";

    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final RealtimeEventService realtimeEventService;
    private final long focusDurationSeconds;
    private final long breakDurationSeconds;

    public TimerService(RoomRepository roomRepository,
                        RoomService roomService,
                        RealtimeEventService realtimeEventService,
                        @Value("${app.pomodoro.focus-seconds:1500}") long focusDurationSeconds,
                        @Value("${app.pomodoro.break-seconds:300}") long breakDurationSeconds) {
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.realtimeEventService = realtimeEventService;
        this.focusDurationSeconds = focusDurationSeconds;
        this.breakDurationSeconds = breakDurationSeconds;
    }

    @Scheduled(fixedDelayString = "${app.pomodoro.auto-advance-ms:1000}")
    public void autoAdvanceRunningTimers() {
        List<Room> runningRooms = roomRepository.findByRunningTrue();
        for (Room room : runningRooms) {
            long elapsed = computeElapsedSeconds(room);
            long duration = phaseDuration(room.isBreakPhase());
            if (elapsed < duration) {
                continue;
            }

            room.setBreakPhase(!room.isBreakPhase());
            room.setElapsedSeconds(0);
            room.setStartTime(Instant.now());
            room.setRunning(true);
            Room saved = roomRepository.save(room);
            realtimeEventService.publishTimerUpdate(saved.getId(), toTimerState(saved));
        }
    }

    public TimerStateResponse start(Long roomId, String email) {
        Room room = getAuthorizedRoom(roomId, email);
        if (!room.isRunning()) {
            normalizeCompletedPausedPhase(room);
            room.setStartTime(Instant.now());
            room.setRunning(true);
            roomRepository.save(room);
        }
        return toTimerState(room);
    }

    public TimerStateResponse pause(Long roomId, String email) {
        Room room = getAuthorizedRoom(roomId, email);
        if (room.isRunning()) {
            long computedElapsed = computeElapsedSeconds(room);
            room.setElapsedSeconds(computedElapsed);
            room.setRunning(false);
            room.setStartTime(null);
            normalizeCompletedPausedPhase(room);
            roomRepository.save(room);
        }
        return toTimerState(room);
    }

    public TimerStateResponse reset(Long roomId, String email) {
        Room room = getAuthorizedRoom(roomId, email);
        room.setElapsedSeconds(0);
        room.setRunning(false);
        room.setStartTime(null);
        room.setBreakPhase(false);
        roomRepository.save(room);
        return toTimerState(room);
    }

    public TimerStateResponse getCurrentState(Room room) {
        return toTimerState(room);
    }

    private Room getAuthorizedRoom(Long roomId, String email) {
        User user = roomService.getCurrentUser(email);
        roomService.ensureOwnerRole(roomId, user.getId());
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private TimerStateResponse toTimerState(Room room) {
        long elapsed = computeElapsedSeconds(room);
        String phase = room.isBreakPhase() ? BREAK_PHASE : FOCUS_PHASE;
        long duration = phaseDuration(room.isBreakPhase());
        long remaining = Math.max(0, duration - elapsed);
        return new TimerStateResponse(
                room.isRunning(),
                elapsed,
            room.isRunning() ? room.getStartTime() : null,
            phase,
            duration,
            remaining
        );
    }

    private long computeElapsedSeconds(Room room) {
        if (!room.isRunning()) {
            return room.getElapsedSeconds();
        }

        Instant startTime = room.getStartTime();
        if (startTime == null) {
            return room.getElapsedSeconds();
        }

        long runningSeconds = Math.max(0, Duration.between(startTime, Instant.now()).getSeconds());
        return room.getElapsedSeconds() + runningSeconds;
    }

    private void normalizeCompletedPausedPhase(Room room) {
        if (room.isRunning()) {
            return;
        }

        long duration = phaseDuration(room.isBreakPhase());
        if (room.getElapsedSeconds() >= duration) {
            room.setElapsedSeconds(0);
            room.setBreakPhase(!room.isBreakPhase());
        }
    }

    private long phaseDuration(boolean isBreakPhase) {
        return isBreakPhase ? breakDurationSeconds : focusDurationSeconds;
    }
}