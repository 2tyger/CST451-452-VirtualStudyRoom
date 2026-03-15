package com.tygilbert.virtualstudyroom.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.message.MessageResponse;
import com.tygilbert.virtualstudyroom.entity.Message;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    public MessageService(MessageRepository messageRepository,
                          RoomRepository roomRepository,
                          RoomService roomService) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
    }

    public List<MessageResponse> listMessages(Long roomId, String email, int limit) {
        User user = roomService.getCurrentUser(email);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        roomService.ensureMembership(roomId, user.getId());

        int safeLimit = Math.max(1, Math.min(limit, 200));
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.ASC, "createdAt"));

        return messageRepository.findByRoomId(room.getId(), pageable).getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getUser().getId(),
                message.getUser().getDisplayName(),
                message.getBody(),
                message.getCreatedAt()
        );
    }
}