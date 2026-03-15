package com.tygilbert.virtualstudyroom.dto.ws;

public record ChatMessagePayload(
        String sender,
        String body
) {
}