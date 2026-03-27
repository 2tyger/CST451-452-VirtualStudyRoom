/*
handles api requests for this domain and delegates work to services
*/
package com.tygilbert.virtualstudyroom.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tygilbert.virtualstudyroom.dto.room.CreateRoomRequest;
import com.tygilbert.virtualstudyroom.dto.room.RoomDetailResponse;
import com.tygilbert.virtualstudyroom.dto.room.RoomResponse;
import com.tygilbert.virtualstudyroom.dto.timer.TimerStateResponse;
import com.tygilbert.virtualstudyroom.service.RealtimeEventService;
import com.tygilbert.virtualstudyroom.service.RoomService;
import com.tygilbert.virtualstudyroom.service.TimerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final TimerService timerService;
    private final RealtimeEventService realtimeEventService;

    public RoomController(RoomService roomService,
                          TimerService timerService,
                          RealtimeEventService realtimeEventService) {
        this.roomService = roomService;
        this.timerService = timerService;
        this.realtimeEventService = realtimeEventService;
    }

    // returns rooms that the authenticated user belongs to
    @GetMapping
    public List<RoomResponse> getRooms(Authentication authentication) {
        return roomService.listRoomsForUser(authentication.getName());
    }

    // creates a room and links the creator as owner member
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request, Authentication authentication) {
        return roomService.createRoom(request, authentication.getName());
    }

    // loads a room and its member list for authorized users
    @GetMapping("/{roomId}")
    public RoomDetailResponse getRoom(@PathVariable Long roomId, Authentication authentication) {
        return roomService.getRoom(roomId, authentication.getName());
    }

    // adds the authenticated user as a room member when needed
    @PostMapping("/{roomId}/join")
    public RoomResponse joinRoom(@PathVariable Long roomId, Authentication authentication) {
        RoomResponse room = roomService.joinRoom(roomId, authentication.getName());
        realtimeEventService.publishRoomMembershipUpdate(roomId, "joined", room.memberCount());
        return room;
    }

    // removes a non owner member from a room
    @PostMapping("/{roomId}/leave")
    public RoomResponse leaveRoom(@PathVariable Long roomId, Authentication authentication) {
        RoomResponse room = roomService.leaveRoom(roomId, authentication.getName());
        realtimeEventService.publishRoomMembershipUpdate(roomId, "left", room.memberCount());
        return room;
    }

    // deletes a room and dependent data when requested by the owner
    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long roomId, Authentication authentication) {
        roomService.deleteRoom(roomId, authentication.getName());
    }

    // starts the room timer and publishes a timer update event
    @PostMapping("/{roomId}/timer/start")
    public TimerStateResponse startTimer(@PathVariable Long roomId, Authentication authentication) {
        TimerStateResponse state = timerService.start(roomId, authentication.getName());
        realtimeEventService.publishTimerUpdate(roomId, state);
        return state;
    }

    // pauses the room timer and publishes a timer update event
    @PostMapping("/{roomId}/timer/pause")
    public TimerStateResponse pauseTimer(@PathVariable Long roomId, Authentication authentication) {
        TimerStateResponse state = timerService.pause(roomId, authentication.getName());
        realtimeEventService.publishTimerUpdate(roomId, state);
        return state;
    }

    // resets the room timer and publishes a timer update event
    @PostMapping("/{roomId}/timer/reset")
    public TimerStateResponse resetTimer(@PathVariable Long roomId, Authentication authentication) {
        TimerStateResponse state = timerService.reset(roomId, authentication.getName());
        realtimeEventService.publishTimerUpdate(roomId, state);
        return state;
    }
}


