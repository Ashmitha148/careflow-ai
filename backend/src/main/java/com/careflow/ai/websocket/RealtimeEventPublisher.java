package com.careflow.ai.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publish entry point for domain events onto the STOMP broker.
 *
 * Phase 1 publishes synchronously from the request thread (acceptable interim:
 * the publish is best-effort and the database is always the source of truth).
 * Phase 2 replaces these direct calls with the transactional outbox + poller,
 * which publishes the same payloads onto the same topics after the transaction
 * commits — no client-visible change.
 *
 * Payloads are the very DTOs the REST endpoints return, so the frontend can
 * apply them to the TanStack Query cache via setQueryData without any
 * transformation.
 */
@Component
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionRegistry sessionRegistry;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate,
                                  WebSocketSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    /** New append-only timeline event for a patient. */
    public void publishTimelineEvent(UUID patientId, Object event) {
        messagingTemplate.convertAndSend(RealtimeTopics.patientTimeline(patientId), event);
    }

    /** Live vital reading for a patient. */
    public void publishVital(UUID patientId, Object vital) {
        messagingTemplate.convertAndSend(RealtimeTopics.patientVitals(patientId), vital);
    }

    /** Task state change on a nurse's shift queue and the patient's workspace. */
    public void publishTaskUpdate(UUID assignedNurseId, Object task) {
        messagingTemplate.convertAndSend(RealtimeTopics.nurseQueue(assignedNurseId), task);
    }

    /** Task state change scoped to a patient workspace. */
    public void publishPatientTaskUpdate(UUID patientId, Object task) {
        if (patientId != null) {
            messagingTemplate.convertAndSend(RealtimeTopics.patientTasks(patientId), task);
        }
    }

    /** Private notification delivered to every live session of the recipient. */
    public void publishNotification(UUID userId, Object notification) {
        for (String sessionId : sessionRegistry.sessionsFor(userId)) {
            messagingTemplate.convertAndSend(RealtimeTopics.userQueue(sessionId), notification);
        }
    }
}