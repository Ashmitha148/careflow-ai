package com.careflow.ai.outbox;

import com.careflow.ai.websocket.RealtimeTopics;
import com.careflow.ai.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Delays REALTIME_BROADCAST outbox rows to live WebSocket subscribers.
 *
 * Routing contract (mirrors Phase 1 semantics exactly — the frontend cannot
 * tell the difference):
 *   - topic column set  → broadcast to that STOMP destination
 *   - userId column set → private /user/queue/notifications to every live
 *                         session of that user, resolved at DELIVERY time
 *                         (sessions may have changed since enqueue)
 *
 * The payload is the same DTO JSON the REST endpoints return, so the frontend
 * applies it to the TanStack Query cache via setQueryData unchanged.
 */
@Component
public class RealtimeBroadcastHandler implements OutboxHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeBroadcastHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public RealtimeBroadcastHandler(
            SimpMessagingTemplate messagingTemplate,
            WebSocketSessionRegistry sessionRegistry,
            ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public String eventType() {
        return OutboxEvent.TYPE_REALTIME_BROADCAST;
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());

        if (event.getTopic() != null) {
            messagingTemplate.convertAndSend(event.getTopic(), payload);
            return;
        }

        UUID userId = event.getUserId();
        if (userId != null) {
            for (String sessionId : sessionRegistry.sessionsFor(userId)) {
                messagingTemplate.convertAndSend(RealtimeTopics.userQueue(sessionId), payload);
            }
            return;
        }

        // Malformed routing: neither topic nor user. Retry won't fix it — fail
        // fast so the relay's attempt budget parks the row as FAILED for review.
        throw new IllegalStateException(
                "REALTIME_BROADCAST row " + event.getId() + " has no topic and no userId");
    }
}
