/*
verifies websocket controller behavior and error mapping
*/
package com.tygilbert.virtualstudyroom.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.ws.ChatSendCommand;
import com.tygilbert.virtualstudyroom.dto.ws.WsErrorPayload;
import com.tygilbert.virtualstudyroom.service.ChatService;

@ExtendWith(MockitoExtension.class)
class RoomWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private RoomWebSocketController roomWebSocketController;

    @Test
    void sendChat_passesPrincipalNameToService() {
        java.security.Principal principal = () -> "student@example.com";

        roomWebSocketController.sendChat(10L, new ChatSendCommand("hello"), principal);

        verify(chatService).saveAndPublishChat(10L, "hello", "student@example.com");
    }

    @Test
    void sendChat_usesNullEmailWhenPrincipalMissing() {
        roomWebSocketController.sendChat(10L, new ChatSendCommand("hello"), null);

        verify(chatService).saveAndPublishChat(10L, "hello", null);
    }

    @Test
    void handleChatException_mapsRateLimitError() {
        WsErrorPayload payload = roomWebSocketController.handleChatException(
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Retry later")
        );

        assertEquals("RATE_LIMITED", payload.code());
        assertEquals("Retry later", payload.message());
    }

    @Test
    void handleChatException_usesFallbackMessageWhenReasonMissing() {
        WsErrorPayload payload = roomWebSocketController.handleChatException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST)
        );

        assertEquals("CHAT_SEND_ERROR", payload.code());
        assertEquals("Unable to send chat right now. Please try again.", payload.message());
    }
}
