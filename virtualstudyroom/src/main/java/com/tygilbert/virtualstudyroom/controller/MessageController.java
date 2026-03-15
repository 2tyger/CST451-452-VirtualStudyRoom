package com.tygilbert.virtualstudyroom.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tygilbert.virtualstudyroom.dto.message.MessageResponse;
import com.tygilbert.virtualstudyroom.service.MessageService;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<MessageResponse> listMessages(@PathVariable Long roomId,
                                              @RequestParam(defaultValue = "50") int limit,
                                              Authentication authentication) {
        return messageService.listMessages(roomId, authentication.getName(), limit);
    }
}