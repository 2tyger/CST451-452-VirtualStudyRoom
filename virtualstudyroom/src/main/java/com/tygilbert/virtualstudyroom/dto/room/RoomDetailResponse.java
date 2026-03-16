/*
defines room request and response contracts shared across api layers
*/
package com.tygilbert.virtualstudyroom.dto.room;

import java.util.List;

public record RoomDetailResponse(
        RoomResponse room,
        List<RoomMemberResponse> members
) {
}

