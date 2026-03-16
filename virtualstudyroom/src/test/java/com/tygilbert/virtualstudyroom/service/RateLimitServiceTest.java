/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RateLimitServiceTest {

    @Test
    void enforceChatSendLimit_blocksAfterConfiguredThreshold() {
        RateLimitService service = new RateLimitService(5, 600, 2, 60);

        service.enforceChatSendLimit(10L, 7L);
        service.enforceChatSendLimit(10L, 7L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.enforceChatSendLimit(10L, 7L)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason() != null && ex.getReason().contains("Too many chat messages sent"));
    }
}


