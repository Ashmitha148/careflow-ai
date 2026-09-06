package com.careflow.ai.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is currently viewing a patient and broadcasts presence updates to
 * {@code /topic/patient/{patientId}/presence}.
 *
 * Presence is intentionally best-effort and in-memory: a missed update is a
 * cosmetic issue, not a clinical-data issue — the timeline/vitals topics remain
 * the authoritative realtime channel. The registry is cleaned up on STOMP
 * UNSUBSCRIBE (via {@link #leave(String, UUID)}), on session disconnect
 * ({@link PresenceDisconnectListener}) and on logout.
 */
@Component
public class PresenceTracker {

    private final SimpMessagingTemplate messagingTemplate;

    /** patientId -> (sessionId -> viewer). */
    private final Map<UUID, Map<String, WebSocketPrincipal>> viewersByPatient = new ConcurrentHashMap<>();
    /** sessionId -> patientId, for O(1) disconnect cleanup. */
    private final Map<String, UUID> patientBySession = new ConcurrentHashMap<>();

    public PresenceTracker(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void join(String sessionId, UUID patientId, WebSocketPrincipal viewer) {
        if (sessionId == null || patientId == null || viewer == null) {
            return;
        }
        viewersByPatient
                .computeIfAbsent(patientId, ignored -> new ConcurrentHashMap<>())
                .put(sessionId, viewer);
        patientBySession.put(sessionId, patientId);
        broadcast(patientId);
    }

    public void leave(String sessionId, UUID patientId) {
        if (patientId == null) {
            return;
        }
        Map<String, WebSocketPrincipal> viewers = viewersByPatient.get(patientId);
        if (viewers != null) {
            viewers.remove(sessionId);
            if (viewers.isEmpty()) {
                viewersByPatient.remove(patientId, viewers);
            }
        }
        patientBySession.remove(sessionId, patientId);
        broadcast(patientId);
    }

    /** Removes a disconnected session from every patient it was viewing. */
    public void leaveAll(String sessionId) {
        if (sessionId == null) {
            return;
        }
        UUID patientId = patientBySession.remove(sessionId);
        if (patientId != null) {
            leave(sessionId, patientId);
        }
    }

    public void broadcastCurrent(UUID patientId) {
        broadcast(patientId);
    }

    private void broadcast(UUID patientId) {
        Map<String, WebSocketPrincipal> viewers = viewersByPatient.get(patientId);
        if (viewers == null || viewers.isEmpty()) {
            messagingTemplate.convertAndSend(
                    RealtimeTopics.patientPresence(patientId),
                    presencePayload(patientId, List.of()));
            return;
        }

        // Aggregate by user so a user with several open tabs appears once.
        Map<UUID, WebSocketPrincipal> distinct = new LinkedHashMap<>();
        for (WebSocketPrincipal viewer : viewers.values()) {
            distinct.putIfAbsent(viewer.userId(), viewer);
        }
        messagingTemplate.convertAndSend(
                RealtimeTopics.patientPresence(patientId),
                presencePayload(patientId, List.copyOf(distinct.values())));
    }

    private Map<String, Object> presencePayload(UUID patientId, List<WebSocketPrincipal> viewers) {
        List<Map<String, Object>> serialized = viewers.stream()
                .map(viewer -> Map.<String, Object>of(
                        "userId", viewer.userId(),
                        "fullName", viewer.fullName() != null ? viewer.fullName() : "Unknown",
                        "role", viewer.role()))
                .toList();

        return Map.of(
                "type", "PRESENCE_UPDATE",
                "patientId", patientId,
                "viewers", serialized);
    }
}