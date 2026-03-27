/*
contains business logic for this domain and coordinates repository operations
*/
package com.tygilbert.virtualstudyroom.service;

import java.time.OffsetDateTime;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.tygilbert.virtualstudyroom.dto.task.TaskResponse;
import com.tygilbert.virtualstudyroom.dto.timer.TimerStateResponse;
import com.tygilbert.virtualstudyroom.dto.ws.ChatMessagePayload;
import com.tygilbert.virtualstudyroom.dto.ws.RealtimeEventType;
import com.tygilbert.virtualstudyroom.dto.ws.RoomMembershipPayload;
import com.tygilbert.virtualstudyroom.dto.ws.RoomEventDto;
import com.tygilbert.virtualstudyroom.dto.ws.TaskUpdatePayload;
import com.tygilbert.virtualstudyroom.dto.ws.TimerUpdatePayload;

@Service
public class RealtimeEventService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    @Autowired
    public RealtimeEventService(SimpMessagingTemplate messagingTemplate, MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.meterRegistry = meterRegistry;
    }

    public RealtimeEventService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.meterRegistry = null;
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

    // publishes room membership updates to notify participant list refreshes
    public void publishRoomMembershipUpdate(Long roomId, String action, long memberCount) {
        publish(roomId, RealtimeEventType.ROOM_MEMBERSHIP_UPDATE, new RoomMembershipPayload(action, memberCount));
    }

    // wraps payloads in a shared realtime event envelope and sends to room topic
    private <T> void publish(Long roomId, RealtimeEventType type, T payload) {
        Timer.Sample sample = startSample();
        RoomEventDto<T> event = new RoomEventDto<>(
                type,
                roomId,
                OffsetDateTime.now(),
                payload
        );

        try {
            messagingTemplate.convertAndSend(topicForRoom(roomId), event);
            incrementPublishCount(type, "success");
        } catch (RuntimeException exception) {
            incrementPublishCount(type, "error");
            throw exception;
        } finally {
            stopPublishTimer(sample, type);
        }
    }

    private String topicForRoom(Long roomId) {
        return "/topic/rooms/" + roomId;
    }

    private void incrementPublishCount(RealtimeEventType type, String result) {
        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
                "vsr.realtime.events.published",
                "event_type", type.wireValue(),
                "result", result
        ).increment();
    }

    private Timer.Sample startSample() {
        if (meterRegistry == null) {
            return null;
        }
        return Timer.start(meterRegistry);
    }

    private void stopPublishTimer(Timer.Sample sample, RealtimeEventType type) {
        if (meterRegistry == null || sample == null) {
            return;
        }

        sample.stop(
                Timer.builder("vsr.realtime.events.publish.duration")
                        .tag("event_type", type.wireValue())
                        .register(meterRegistry)
        );
    }
}

