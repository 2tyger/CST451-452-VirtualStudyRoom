package com.tygilbert.virtualstudyroom.dto.ws;

import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;

public record TaskUpdatePayload(
        String action,
        TaskResponse task
) {
}