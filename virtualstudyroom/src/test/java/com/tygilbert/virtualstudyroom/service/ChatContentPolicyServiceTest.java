/*
verifies service behavior and edge cases for this module
*/
package com.tygilbert.virtualstudyroom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ChatContentPolicyServiceTest {

    @Test
    void sanitizeAndValidate_preservesReadableTextAndStripsControlChars() {
        ChatContentPolicyService policy = new ChatContentPolicyService(100);

        String sanitized = policy.sanitizeAndValidate("  <b>hello</b>\u0001  ");

        assertEquals("<b>hello</b>", sanitized);
    }

    @Test
    void sanitizeAndValidate_rejectsBlankMessages() {
        ChatContentPolicyService policy = new ChatContentPolicyService(100);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> policy.sanitizeAndValidate("  \n  ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void sanitizeAndValidate_rejectsTooLongMessages() {
        ChatContentPolicyService policy = new ChatContentPolicyService(5);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> policy.sanitizeAndValidate("123456")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}


