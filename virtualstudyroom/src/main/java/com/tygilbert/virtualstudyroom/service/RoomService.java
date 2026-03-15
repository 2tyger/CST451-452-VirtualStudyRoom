package com.tygilbert.virtualstudyroom.service;

import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tygilbert.virtualstudyroom.dto.room.CreateRoomRequest;
import com.tygilbert.virtualstudyroom.dto.room.RoomDetailResponse;
import com.tygilbert.virtualstudyroom.dto.room.RoomMemberResponse;
import com.tygilbert.virtualstudyroom.dto.room.RoomResponse;
import com.tygilbert.virtualstudyroom.entity.Room;
import com.tygilbert.virtualstudyroom.entity.RoomMember;
import com.tygilbert.virtualstudyroom.entity.User;
import com.tygilbert.virtualstudyroom.repository.MessageRepository;
import com.tygilbert.virtualstudyroom.repository.RoomMemberRepository;
import com.tygilbert.virtualstudyroom.repository.RoomRepository;
import com.tygilbert.virtualstudyroom.repository.TaskRepository;
import com.tygilbert.virtualstudyroom.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;
    private final TaskRepository taskRepository;
    private final MessageRepository messageRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomMemberRepository roomMemberRepository,
                       UserRepository userRepository,
                       RateLimitService rateLimitService,
                       TaskRepository taskRepository,
                       MessageRepository messageRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.rateLimitService = rateLimitService;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
    }

    public List<RoomResponse> listRoomsForUser(String email) {
        User user = getCurrentUser(email);
        return roomMemberRepository.findByUserId(user.getId()).stream()
                .map(RoomMember::getRoom)
                .map(this::toRoomResponse)
                .toList();
    }

    public RoomResponse createRoom(CreateRoomRequest request, String email) {
        User owner = getCurrentUser(email);
        rateLimitService.enforceRoomCreateLimit(owner.getId());

        Room room = new Room();
        room.setName(request.name().trim());
        room.setOwner(owner);
        Room saved = roomRepository.save(room);

        RoomMember ownerLink = new RoomMember();
        ownerLink.setRoom(saved);
        ownerLink.setUser(owner);
        ownerLink.setRole("OWNER");
        roomMemberRepository.save(ownerLink);

        return toRoomResponse(saved);
    }

    public RoomDetailResponse getRoom(@NonNull Long roomId, String email) {
        User user = getCurrentUser(email);
        Long userId = Objects.requireNonNull(user.getId(), "Current user id is required");
        ensureMembership(roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        List<RoomMemberResponse> members = roomMemberRepository.findByRoomId(roomId).stream()
                .map(member -> new RoomMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getDisplayName(),
                        member.getRole()
                ))
                .toList();

        return new RoomDetailResponse(toRoomResponse(room), members);
    }

    public RoomResponse joinRoom(@NonNull Long roomId, String email) {
        User user = getCurrentUser(email);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, user.getId())) {
            RoomMember roomMember = new RoomMember();
            roomMember.setRoom(room);
            roomMember.setUser(user);
            roomMember.setRole("MEMBER");
            roomMemberRepository.save(roomMember);
        }

        return toRoomResponse(room);
    }

    public RoomResponse leaveRoom(@NonNull Long roomId, String email) {
        User user = getCurrentUser(email);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a room member"));

        if ("OWNER".equalsIgnoreCase(membership.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Room owner cannot leave until ownership transfer is supported"
            );
        }

        roomMemberRepository.delete(membership);
        return toRoomResponse(room);
    }

    @Transactional
    public void deleteRoom(@NonNull Long roomId, String email) {
        User user = getCurrentUser(email);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (!room.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room owner can delete the room");
        }

        messageRepository.deleteByRoomId(roomId);
        taskRepository.deleteByRoomId(roomId);
        roomMemberRepository.deleteByRoomId(roomId);
        roomRepository.delete(room);
    }

    public void ensureMembership(@NonNull Long roomId, @NonNull Long userId) {
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a room member");
        }
    }

    public void ensureOwnerRole(@NonNull Long roomId, @NonNull Long userId) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a room member"));

        if (!"OWNER".equalsIgnoreCase(membership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room owner can control the timer");
        }
    }

    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user context"));
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner().getId(),
                roomMemberRepository.countByRoomId(room.getId()),
                room.isActive(),
            room.isBreakPhase(),
                room.isRunning(),
                room.getElapsedSeconds(),
                room.getStartTime(),
                room.getCreatedAt()
        );
    }
}