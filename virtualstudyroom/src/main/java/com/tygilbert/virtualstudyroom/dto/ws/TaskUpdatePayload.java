/*
defines websocket event contracts and payload shapes used by realtime messaging
*/
package com.tygilbert.virtualstudyroom.dto.ws;

import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;

public record TaskUpdatePayload(
        String action,
        TaskResponse task
) {
}

