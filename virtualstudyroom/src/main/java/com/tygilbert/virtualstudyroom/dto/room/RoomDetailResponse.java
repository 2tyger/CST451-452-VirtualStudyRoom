package com.tygilbert.virtualstudyroom.dto.room;

import java.util.List;

public record RoomDetailResponse(
        RoomResponse room,
        List<RoomMemberResponse> members
) {
}