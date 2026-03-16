/*
defines websocket event contracts and payload shapes used by realtime messaging
*/
package com.tygilbert.virtualstudyroom.dto.ws;

public record ChatMessagePayload(
        String sender,
        String body
) {
}

