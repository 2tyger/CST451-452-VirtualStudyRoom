package com.tygilbert.virtualstudyroom.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private RealtimeEventService realtimeEventService;

    private TimerService timerService;

    @BeforeEach
    void setUp() {
        timerService = new TimerService(roomRepository, roomService, realtimeEventService, 5L, 2L);
    }

    @Test
    void autoAdvanceRunningTimers_switchesPhaseAndPublishesUpdate() {
        Room room = new Room();
        room.setId(15L);
        room.setRunning(true);
        room.setBreakPhase(false);
        room.setElapsedSeconds(5L);
        room.setStartTime(Instant.now().minusSeconds(1));

        when(roomRepository.findByRunningTrue()).thenReturn(List.of(room));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        timerService.autoAdvanceRunningTimers();

        assertTrue(room.isBreakPhase());
        assertTrue(room.isRunning());
        assertFalse(room.getStartTime() == null);
        verify(realtimeEventService).publishTimerUpdate(
                eq(15L),
                argThat(state -> "BREAK".equals(state.phase()) && state.phaseDurationSeconds() == 2L)
        );
    }

    @Test
    void start_throwsForbiddenWhenUserIsNotOwner() {
        User user = new User();
        user.setId(3L);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room owner can control the timer"))
                .when(roomService)
                .ensureOwnerRole(10L, 3L);

        assertThrows(ResponseStatusException.class, () -> timerService.start(10L, "member@example.com"));
        verify(roomRepository, never()).findById(10L);
    }

    @Test
    void start_resumesPausedRoomForOwner() {
        User owner = new User();
        owner.setId(1L);

        Room room = new Room();
        room.setId(20L);
        room.setRunning(false);
        room.setBreakPhase(false);
        room.setElapsedSeconds(1L);

        when(roomService.getCurrentUser("owner@example.com")).thenReturn(owner);
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        timerService.start(20L, "owner@example.com");

        assertTrue(room.isRunning());
        assertFalse(room.getStartTime() == null);
        verify(roomRepository).save(room);
    }
}
