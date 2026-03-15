package com.tygilbert.virtualstudyroom.dto.room;

public record RoomMemberResponse(
        Long userId,
        String displayName,
        String role
) {
}