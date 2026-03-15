package com.tygilbert.virtualstudyroom.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RateLimitService {

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();
    private final int roomCreateMaxRequests;
    private final Duration roomCreateWindow;
    private final int chatSendMaxRequests;
    private final Duration chatSendWindow;

    public RateLimitService(
            @Value("${app.rate-limit.room-create.max-requests:5}") int roomCreateMaxRequests,
            @Value("${app.rate-limit.room-create.window-seconds:600}") long roomCreateWindowSeconds,
            @Value("${app.rate-limit.chat-send.max-requests:20}") int chatSendMaxRequests,
            @Value("${app.rate-limit.chat-send.window-seconds:10}") long chatSendWindowSeconds
    ) {
        this.roomCreateMaxRequests = roomCreateMaxRequests;
        this.roomCreateWindow = Duration.ofSeconds(roomCreateWindowSeconds);
        this.chatSendMaxRequests = chatSendMaxRequests;
        this.chatSendWindow = Duration.ofSeconds(chatSendWindowSeconds);
    }

    public void enforceRoomCreateLimit(Long userId) {
        String key = "room-create:" + userId;
        enforceLimit(key, roomCreateMaxRequests, roomCreateWindow, "Too many room creation requests");
    }

    public void enforceChatSendLimit(Long roomId, Long userId) {
        String key = "chat-send:" + roomId + ":" + userId;
        enforceLimit(key, chatSendMaxRequests, chatSendWindow, "Too many chat messages sent");
    }

    private void enforceLimit(String key, int maxRequests, Duration window, String message) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();

        Deque<Long> bucket = requestWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst() <= cutoff) {
                bucket.pollFirst();
            }

            if (bucket.size() >= maxRequests) {
                long retryAfterMs = bucket.peekFirst() + window.toMillis() - now;
                long retryAfterSeconds = Math.max(1L, (long) Math.ceil(retryAfterMs / 1000.0));
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        message + ". Retry in " + retryAfterSeconds + " seconds."
                );
            }

            bucket.addLast(now);
        }
    }
}