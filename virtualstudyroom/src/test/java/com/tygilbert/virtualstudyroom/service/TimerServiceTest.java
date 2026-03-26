/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.tygilbert.virtualstudyroom.dto.timer.TimerStateResponse;
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

    @Test
    void pause_stopsRunningRoomAndStoresElapsedSeconds() {
        User owner = new User();
        owner.setId(1L);

        Room room = new Room();
        room.setId(20L);
        room.setRunning(true);
        room.setBreakPhase(false);
        room.setElapsedSeconds(2L);
        room.setStartTime(Instant.now().minusSeconds(1));

        when(roomService.getCurrentUser("owner@example.com")).thenReturn(owner);
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        TimerStateResponse state = timerService.pause(20L, "owner@example.com");

        assertFalse(state.isRunning());
        assertFalse(room.isRunning());
        assertNull(room.getStartTime());
        assertTrue(room.getElapsedSeconds() >= 2L);
        verify(roomRepository).save(room);
    }

    @Test
    void reset_returnsRoomToDefaultFocusState() {
        User owner = new User();
        owner.setId(1L);

        Room room = new Room();
        room.setId(20L);
        room.setRunning(true);
        room.setBreakPhase(true);
        room.setElapsedSeconds(9L);
        room.setStartTime(Instant.now());

        when(roomService.getCurrentUser("owner@example.com")).thenReturn(owner);
        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        TimerStateResponse state = timerService.reset(20L, "owner@example.com");

        assertFalse(state.isRunning());
        assertEquals("FOCUS", state.phase());
        assertEquals(0L, state.elapsedSeconds());
        assertEquals(5L, state.phaseDurationSeconds());
        assertEquals(5L, state.remainingSeconds());
    }

    @Test
    void getCurrentState_usesComputedElapsedForRunningRoom() {
        Room room = new Room();
        room.setRunning(true);
        room.setBreakPhase(false);
        room.setElapsedSeconds(2L);
        room.setStartTime(Instant.now().minusSeconds(2));

        TimerStateResponse state = timerService.getCurrentState(room);

        assertTrue(state.elapsedSeconds() >= 2L);
        assertEquals("FOCUS", state.phase());
        assertEquals(5L, state.phaseDurationSeconds());
    }
}


