package com.tygilbert.virtualstudyroom.dto.ws;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSendCommand(
        @NotBlank @Size(max = 2000) String body
) {
}