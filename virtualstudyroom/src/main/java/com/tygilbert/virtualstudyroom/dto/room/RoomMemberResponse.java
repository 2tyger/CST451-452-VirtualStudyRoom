/*
defines room request and response contracts shared across api layers
*/
package com.tygilbert.virtualstudyroom.dto.room;

public record RoomMemberResponse(
        Long userId,
        String displayName,
        String role
) {
}

