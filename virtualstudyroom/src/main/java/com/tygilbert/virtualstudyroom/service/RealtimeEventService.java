/*
contains business logic for this domain and coordinates repository operations
*/
package com.tygilbert.virtualstudyroom.service;

import java.time.OffsetDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;
import com.tygilbert.virtualstudyroom.dto.timer.TimerStateResponse;
import com.tygilbert.virtualstudyroom.dto.ws.ChatMessagePayload;
import com.tygilbert.virtualstudyroom.dto.ws.RealtimeEventType;
import com.tygilbert.virtualstudyroom.dto.ws.RoomEventDto;
import com.tygilbert.virtualstudyroom.dto.ws.TaskUpdatePayload;
import com.tygilbert.virtualstudyroom.dto.ws.TimerUpdatePayload;

@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // publishes timer state updates to the room topic
    public void publishTimerUpdate(Long roomId, TimerStateResponse state) {
        TimerUpdatePayload payload = new TimerUpdatePayload(
                state.isRunning(),
                state.elapsedSeconds(),
                state.isRunning() ? state.startTime() : null,
                state.phase(),
                state.phaseDurationSeconds(),
                state.remainingSeconds()
        );
        publish(roomId, RealtimeEventType.TIMER_UPDATE, payload);
    }

    // publishes task action updates to the room topic
    public void publishTaskUpdate(Long roomId, String action, TaskResponse task) {
        publish(roomId, RealtimeEventType.TASK_UPDATE, new TaskUpdatePayload(action, task));
    }

    // publishes chat messages to the room topic
    public void publishChatMessage(Long roomId, String sender, String body) {
        publish(roomId, RealtimeEventType.CHAT_MESSAGE, new ChatMessagePayload(sender, body));
    }

    // wraps payloads in a shared realtime event envelope and sends to room topic
    private <T> void publish(Long roomId, RealtimeEventType type, T payload) {
        RoomEventDto<T> event = new RoomEventDto<>(
                type,
                roomId,
                OffsetDateTime.now(),
                payload
        );
        messagingTemplate.convertAndSend(topicForRoom(roomId), event);
    }

    private String topicForRoom(Long roomId) {
        return "/topic/rooms/" + roomId;
    }
}

