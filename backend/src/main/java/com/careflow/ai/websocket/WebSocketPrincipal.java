package com.careflow.ai.websocket;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves the authenticated principal carried in STOMP message headers.
 *
 * {@link JwtHandshakeInterceptor} stores the authenticated user's identity in
 * the WebSocket session attributes, which {@code StompSubProtocolHandler} copies
 * into every STOMP message header under {@link SimpMessageHeaderAccessor#SESSION_ATTRIBUTES}.
 */
public record WebSocketPrincipal(UUID userId, String role, String fullName) {

    public static WebSocketPrincipal from(MessageHeaders headers) {
        if (headers == null) {
            return null;
        }
        Object rawAttributes = headers.get(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES);
        if (!(rawAttributes instanceof Map<?, ?> attributes)) {
            return null;
        }

        Object rawUserId = attributes.get(JwtHandshakeInterceptor.USER_ID_ATTRIBUTE);
        Object rawRole = attributes.get(JwtHandshakeInterceptor.ROLE_ATTRIBUTE);
        Object rawFullName = attributes.get(JwtHandshakeInterceptor.FULL_NAME_ATTRIBUTE);

        if (!(rawUserId instanceof String userIdString)) {
            return null;
        }
        try {
            return new WebSocketPrincipal(
                    UUID.fromString(userIdString),
                    rawRole instanceof String role ? role : null,
                    rawFullName instanceof String fullName ? fullName : null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}