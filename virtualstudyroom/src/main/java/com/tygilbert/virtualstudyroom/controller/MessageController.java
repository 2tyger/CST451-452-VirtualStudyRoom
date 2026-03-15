package com.tygilbert.virtualstudyroom.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tygilbert.virtualstudyroom.dto.message.MessageResponse;
import com.tygilbert.virtualstudyroom.dto.ws.ChatSendCommand;
import com.tygilbert.virtualstudyroom.service.ChatService;
import com.tygilbert.virtualstudyroom.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;
    private final ChatService chatService;

    public MessageController(MessageService messageService, ChatService chatService) {
        this.messageService = messageService;
        this.chatService = chatService;
    }

    @GetMapping
    public List<MessageResponse> listMessages(@PathVariable Long roomId,
                                              @RequestParam(defaultValue = "50") int limit,
                                              Authentication authentication) {
        return messageService.listMessages(roomId, authentication.getName(), limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable Long roomId,
                                       @Valid @RequestBody ChatSendCommand command,
                                       Authentication authentication) {
        return chatService.saveAndPublishChat(roomId, command.body(), authentication.getName());
    }
}