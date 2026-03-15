package com.tygilbert.virtualstudyroom.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.entity.Message;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final RealtimeEventService realtimeEventService;
    private final RateLimitService rateLimitService;
    private final ChatContentPolicyService chatContentPolicyService;

    public ChatService(MessageRepository messageRepository,
                       RoomRepository roomRepository,
                       RoomService roomService,
                       RealtimeEventService realtimeEventService,
                       RateLimitService rateLimitService,
                       ChatContentPolicyService chatContentPolicyService) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.realtimeEventService = realtimeEventService;
        this.rateLimitService = rateLimitService;
        this.chatContentPolicyService = chatContentPolicyService;
    }

    public void saveAndPublishChat(Long roomId, String body, String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user context");
        }

        String normalizedBody = chatContentPolicyService.sanitizeAndValidate(body);

        User user = roomService.getCurrentUser(email);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        roomService.ensureMembership(roomId, user.getId());
        rateLimitService.enforceChatSendLimit(roomId, user.getId());

        Message message = new Message();
        message.setRoom(room);
        message.setUser(user);
        message.setBody(normalizedBody);
        Message saved = messageRepository.save(message);

        realtimeEventService.publishChatMessage(
                roomId,
                saved.getUser().getDisplayName(),
                saved.getBody()
        );
    }
}