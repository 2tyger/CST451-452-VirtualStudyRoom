package com.tygilbert.virtualstudyroom.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.entity.Message;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @Mock
    private RealtimeEventService realtimeEventService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ChatContentPolicyService chatContentPolicyService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void saveAndPublishChat_persistsSanitizedMessageAndPublishesEvent() {
        User user = new User();
        user.setId(9L);
        user.setDisplayName("Ty");

        Room room = new Room();
        room.setId(10L);

        when(chatContentPolicyService.sanitizeAndValidate("<b>hello</b>")).thenReturn("&lt;b&gt;hello&lt;/b&gt;");
        when(roomService.getCurrentUser("ty@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.saveAndPublishChat(10L, "<b>hello</b>", "ty@example.com");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals("&lt;b&gt;hello&lt;/b&gt;", messageCaptor.getValue().getBody());
        assertEquals(room, messageCaptor.getValue().getRoom());
        assertEquals(user, messageCaptor.getValue().getUser());

        verify(realtimeEventService).publishChatMessage(10L, "Ty", "&lt;b&gt;hello&lt;/b&gt;");
    }

    @Test
    void saveAndPublishChat_enforcesRateLimitBeforePersisting() {
        User user = new User();
        user.setId(9L);
        user.setDisplayName("Ty");

        Room room = new Room();
        room.setId(10L);

        when(chatContentPolicyService.sanitizeAndValidate("hello")).thenReturn("hello");
        when(roomService.getCurrentUser("ty@example.com")).thenReturn(user);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        ResponseStatusException limitException = new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many chat messages");
        org.mockito.Mockito.doThrow(limitException).when(rateLimitService).enforceChatSendLimit(10L, 9L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> chatService.saveAndPublishChat(10L, "hello", "ty@example.com")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(messageRepository, org.mockito.Mockito.never()).save(any(Message.class));
        verify(realtimeEventService, org.mockito.Mockito.never()).publishChatMessage(any(), any(), any());
    }
}
