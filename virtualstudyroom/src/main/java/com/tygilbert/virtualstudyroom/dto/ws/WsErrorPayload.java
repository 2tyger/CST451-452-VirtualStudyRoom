package com.tygilbert.virtualstudyroom.dto.ws;

public record WsErrorPayload(
        String code,
        String message
) {
}