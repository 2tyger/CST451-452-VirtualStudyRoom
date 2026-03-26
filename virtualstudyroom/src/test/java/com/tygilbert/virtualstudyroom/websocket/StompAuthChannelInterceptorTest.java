/*
verifies websocket auth interceptor behavior for stomp connect frames
*/
package com.tygilbert.virtualstudyroom.websocket;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.tygilbert.virtualstudyroom.security.AppUserDetailsService;
import com.tygilbert.virtualstudyroom.security.JwtService;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserDetailsService userDetailsService;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void preSend_setsAuthenticationForValidConnectToken() {
        Message<byte[]> message = buildConnectMessage("Bearer valid-token");

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractSubject("valid-token")).thenReturn("student@example.com");

        UserDetails userDetails = User.withUsername("student@example.com")
                .password("password")
                .authorities(List.of())
                .build();
        when(userDetailsService.loadUserByUsername("student@example.com")).thenReturn(userDetails);

        Message<?> output = interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(output);
        assertNotNull(accessor.getUser());
        assertEquals("student@example.com", accessor.getUser().getName());
        verify(userDetailsService).loadUserByUsername("student@example.com");
    }

    @Test
    void preSend_doesNotAuthenticateWhenTokenInvalid() {
        Message<byte[]> message = buildConnectMessage("Bearer invalid-token");
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        Message<?> output = interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(output);
        assertNull(accessor.getUser());
    }

    @Test
    void preSend_ignoresNonConnectFrames() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> output = interceptor.preSend(message, null);

        StompHeaderAccessor wrapped = StompHeaderAccessor.wrap(output);
        assertNull(wrapped.getUser());
    }

    private Message<byte[]> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", authHeader);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
