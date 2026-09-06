package com.careflow.ai.websocket;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks authenticated WebSocket sessions per user.
 *
 * The STOMP broker has no concept of "session belongs to user" beyond what we
 * establish at the JWT handshake, so we keep our own registry. It powers:
 *
 * <ul>
 *   <li>per-user notification delivery (each of a user's sessions gets a copy),</li>
 *   <li>presence cleanup when a session disconnects.</li>
 * </ul>
 */
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentHashMap<UUID, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> userBySession = new ConcurrentHashMap<>();

    public void register(String sessionId, UUID userId) {
        if (sessionId == null || userId == null) {
            return;
        }
        userBySession.put(sessionId, userId);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public void unregister(String sessionId) {
        if (sessionId == null) {
            return;
        }
        UUID userId = userBySession.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = sessionsByUser.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(userId, sessions);
                }
            }
        }
    }

    /** All live sessions belonging to the given user. */
    public Set<String> sessionsFor(UUID userId) {
        Set<String> sessions = sessionsByUser.get(userId);
        return sessions != null ? sessions : Set.of();
    }

    public UUID userFor(String sessionId) {
        return userBySession.get(sessionId);
    }

    public boolean isRegistered(String sessionId) {
        return sessionId != null && userBySession.containsKey(sessionId);
    }
}