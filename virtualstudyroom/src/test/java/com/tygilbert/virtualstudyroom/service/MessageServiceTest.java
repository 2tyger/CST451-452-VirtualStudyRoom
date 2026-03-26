/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.message.MessageResponse;
import com.tygilbert.virtualstudyroom.entity.Message;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void listMessages_returnsMessagesInAscendingCreatedOrder() {
        User user = new User();
        user.setId(3L);

        Room room = new Room();
        room.setId(10L);

        User sender = new User();
        sender.setId(8L);
        sender.setDisplayName("Ty");

        Message first = new Message();
        first.setId(1L);
        first.setRoom(room);
        first.setUser(sender);
        first.setBody("first");
        first.setCreatedAt(OffsetDateTime.now().minusMinutes(2));

        Message second = new Message();
        second.setId(2L);
        second.setRoom(room);
        second.setUser(sender);
        second.setBody("second");
        second.setCreatedAt(OffsetDateTime.now().minusMinutes(1));

        Page<Message> page = new PageImpl<>(List.of(first, second));

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(messageRepository.findByRoomId(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);

        List<MessageResponse> response = messageService.listMessages(10L, "member@example.com", 50);

        assertEquals(2, response.size());
        assertEquals("first", response.get(0).body());
        assertEquals("second", response.get(1).body());
    }

    @Test
    void listMessages_capsLimitAtTwoHundred() {
        User user = new User();
        user.setId(3L);

        Room room = new Room();
        room.setId(10L);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(messageRepository.findByRoomId(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());

        messageService.listMessages(10L, "member@example.com", 999);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(messageRepository).findByRoomId(org.mockito.ArgumentMatchers.eq(10L), pageableCaptor.capture());
        assertEquals(200, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void listMessages_throwsNotFoundWhenRoomDoesNotExist() {
        User user = new User();
        user.setId(3L);

        when(roomService.getCurrentUser("member@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> messageService.listMessages(10L, "member@example.com", 10)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
