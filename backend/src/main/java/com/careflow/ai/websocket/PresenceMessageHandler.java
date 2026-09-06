package com.careflow.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * STOMP handler for presence control messages sent by clients to
 * {@code /app/presence/patient/{patientId}}.
 *
 * Protocol (documented in docs/adr/0001-websocket-topic-design.md):
 * <pre>
 *   {"action": "JOIN"}   — this session is now viewing the patient
 *   {"action": "LEAVE"}  — this session stopped viewing the patient
 *   {"action": "LIST"}   — re-broadcast the current viewer list
 * </pre>
 *
 * Sends are processed strictly in order per connection and after any
 * preceding SUBSCRIBE, so a JOIN broadcast always reaches a subscriber that
 * subscribed just before it. Disconnect cleanup happens in
 * {@link PresenceDisconnectListener}.
 */
@Controller
public class PresenceMessageHandler {

    public enum PresenceAction {
        JOIN, LEAVE, LIST
    }

    private final PresenceTracker presenceTracker;
    private final PatientAccessChecker patientAccessChecker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PresenceMessageHandler(PresenceTracker presenceTracker,
                                  PatientAccessChecker patientAccessChecker) {
        this.presenceTracker = presenceTracker;
        this.patientAccessChecker = patientAccessChecker;
    }

    @MessageMapping("/presence/patient/{patientId}")
    public void handle(@DestinationVariable UUID patientId,
                       @Payload String payload,
                       Message<?> message) {
        WebSocketPrincipal principal = WebSocketPrincipal.from(message.getHeaders());
        if (principal == null) {
            return;
        }
        if (!patientAccessChecker.canAccess(principal, patientId)) {
            return;
        }

        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        PresenceAction action = parseAction(payload);
        if (action == null) {
            return;
        }

        switch (action) {
            case JOIN -> presenceTracker.join(sessionId, patientId, principal);
            case LEAVE -> presenceTracker.leave(sessionId, patientId);
            case LIST -> presenceTracker.broadcastCurrent(patientId);
        }
    }

    private PresenceAction parseAction(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = objectMapper.readValue(payload, Map.class);
            Object raw = body.get("action");
            if (!(raw instanceof String action)) {
                return null;
            }
            return PresenceAction.valueOf(action.toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}