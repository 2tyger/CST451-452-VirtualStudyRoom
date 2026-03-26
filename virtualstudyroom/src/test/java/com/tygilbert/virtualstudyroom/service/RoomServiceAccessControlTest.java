/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.RoomMember;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomMemberRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;
import com.tygilbert.virtualstudyroom.repository.TaskRepository;
import com.tygilbert.virtualstudyroom.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RoomServiceAccessControlTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void getRoom_throwsForbiddenForNonMember() {
        User user = new User();
        user.setId(7L);
        user.setEmail("member@example.com");

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(roomMemberRepository.existsByRoomIdAndUserId(10L, 7L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.getRoom(10L, "member@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(roomRepository, never()).findById(10L);
    }

    @Test
    void ensureOwnerRole_throwsForbiddenForMemberRole() {
        RoomMember membership = new RoomMember();
        membership.setRole("MEMBER");
        when(roomMemberRepository.findByRoomIdAndUserId(10L, 7L)).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.ensureOwnerRole(10L, 7L)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void leaveRoom_throwsBadRequestForOwner() {
        User user = new User();
        user.setId(7L);
        user.setEmail("owner@example.com");

        Room room = new Room();
        room.setId(10L);

        RoomMember membership = new RoomMember();
        membership.setRole("OWNER");

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.findByRoomIdAndUserId(10L, 7L)).thenReturn(Optional.of(membership));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.leaveRoom(10L, "owner@example.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void joinRoom_createsMembershipWhenUserIsNotMember() {
        User user = new User();
        user.setId(7L);
        user.setEmail("member@example.com");

        User owner = new User();
        owner.setId(1L);

        Room room = new Room();
        room.setId(10L);
        room.setOwner(owner);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(roomMemberRepository.existsByRoomIdAndUserId(10L, 7L)).thenReturn(false);

        roomService.joinRoom(10L, "member@example.com");

        verify(roomMemberRepository).save(any(RoomMember.class));
    }

    @Test
    void deleteRoom_throwsForbiddenForNonOwner() {
        User member = new User();
        member.setId(99L);
        member.setEmail("member@example.com");

        User owner = new User();
        owner.setId(1L);

        Room room = new Room();
        room.setId(10L);
        room.setOwner(owner);

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> roomService.deleteRoom(10L, "member@example.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(messageRepository, never()).deleteByRoomId(10L);
        verify(taskRepository, never()).deleteByRoomId(10L);
        verify(roomMemberRepository, never()).deleteByRoomId(10L);
    }
}


