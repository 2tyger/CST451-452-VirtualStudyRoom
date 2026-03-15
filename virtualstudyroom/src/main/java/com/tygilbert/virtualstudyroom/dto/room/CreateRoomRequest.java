package com.tygilbert.virtualstudyroom.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank @Size(min = 2, max = 120) String name
) {
}