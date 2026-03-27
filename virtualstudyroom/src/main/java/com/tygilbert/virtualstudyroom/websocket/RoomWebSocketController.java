/*
handles websocket authentication and room realtime message entry points
*/
package com.tygilbert.virtualstudyroom.websocket;

import java.security.Principal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final MeterRegistry meterRegistry;

    @Autowired
    public RoomWebSocketController(ChatService chatService, MeterRegistry meterRegistry) {
        this.chatService = chatService;
        this.meterRegistry = meterRegistry;
    }

    public RoomWebSocketController(ChatService chatService) {
        this.chatService = chatService;
        this.meterRegistry = null;
    }

    // accepts room chat send commands from websocket clients
    @MessageMapping("/rooms/{roomId}/chat.send")
    public void sendChat(@DestinationVariable Long roomId, ChatSendCommand command, Principal principal) {
        Timer.Sample sample = startSample();
        String email = principal != null ? principal.getName() : null;

        try {
            chatService.saveAndPublishChat(roomId, command.body(), email);
            incrementInboundChatCount("success");
        } catch (RuntimeException exception) {
            incrementInboundChatCount("error");
            throw exception;
        } finally {
            stopInboundTimer(sample);
        }
    }

    // maps service errors to user scoped websocket error payloads
    @MessageExceptionHandler(ResponseStatusException.class)
    @SendToUser("/queue/errors")
    public WsErrorPayload handleChatException(ResponseStatusException exception) {
        int statusCode = exception.getStatusCode().value();
        incrementInboundErrorCount(statusCode);
        String code = statusCode == 429 ? "RATE_LIMITED" : "CHAT_SEND_ERROR";
        String fallback = statusCode == 429
                ? "You are sending messages too quickly. Please wait a moment and try again."
                : "Unable to send chat right now. Please try again.";
        String message = exception.getReason() != null ? exception.getReason() : fallback;
        return new WsErrorPayload(code, message);
    }

    private void incrementInboundChatCount(String result) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
                "vsr.websocket.chat.inbound",
                "result", result
        ).increment();
    }

    private void incrementInboundErrorCount(int statusCode) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
                "vsr.websocket.chat.errors",
                "status", Integer.toString(statusCode)
        ).increment();
    }

    private Timer.Sample startSample() {
        if (meterRegistry == null) {
            return null;
        }
        return Timer.start(meterRegistry);
    }

    private void stopInboundTimer(Timer.Sample sample) {
        if (meterRegistry == null || sample == null) {
            return;
        }

        sample.stop(
                Timer.builder("vsr.websocket.chat.inbound.duration")
                        .register(meterRegistry)
        );
    }
}

