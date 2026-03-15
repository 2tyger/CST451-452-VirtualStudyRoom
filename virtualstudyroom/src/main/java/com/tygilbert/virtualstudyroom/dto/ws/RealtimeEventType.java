package com.tygilbert.virtualstudyroom.dto.ws;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RealtimeEventType {
    TIMER_UPDATE("timer_update"),
    TASK_UPDATE("task_update"),
    CHAT_MESSAGE("chat_message");

    private final String wireValue;

    RealtimeEventType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }
}