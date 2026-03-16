/*
handles websocket authentication and room realtime message entry points
*/
package com.tygilbert.virtualstudyroom.websocket;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.ws.ChatSendCommand;
import com.tygilbert.virtualstudyroom.dto.ws.WsErrorPayload;
import com.tygilbert.virtualstudyroom.service.ChatService;

@Controller
public class RoomWebSocketController {

    private final ChatService chatService;

    public RoomWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    // accepts room chat send commands from websocket clients
    @MessageMapping("/rooms/{roomId}/chat.send")
    public void sendChat(@DestinationVariable Long roomId, ChatSendCommand command, Principal principal) {
        String email = principal != null ? principal.getName() : null;
        chatService.saveAndPublishChat(roomId, command.body(), email);
    }

    // maps service errors to user scoped websocket error payloads
    @MessageExceptionHandler(ResponseStatusException.class)
    @SendToUser("/queue/errors")
    public WsErrorPayload handleChatException(ResponseStatusException exception) {
        int statusCode = exception.getStatusCode().value();
        String code = statusCode == 429 ? "RATE_LIMITED" : "CHAT_SEND_ERROR";
        String fallback = statusCode == 429
                ? "You are sending messages too quickly. Please wait a moment and try again."
                : "Unable to send chat right now. Please try again.";
        String message = exception.getReason() != null ? exception.getReason() : fallback;
        return new WsErrorPayload(code, message);
    }
}

