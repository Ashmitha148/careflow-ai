package com.careflow.ai.websocket;

import com.careflow.ai.outbox.OutboxEvent;
import com.careflow.ai.outbox.OutboxService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publish entry point for domain events onto the STOMP broker.
 *
 * Phase 2: publishing is transactional. Each method enqueues a
 * REALTIME_BROADCAST row into the outbox IN the caller's transaction; the
 * {@link com.careflow.ai.outbox.OutboxRelay} broadcasts it only after that
 * transaction commits (see docs/adr/0002-transactional-outbox.md). Consequences:
 * <ul>
 *   <li>a client can never receive an event for data the database rolled back;</li>
 *   <li>a broker/WebSocket outage delays delivery (at-least-once retry with
 *       backoff) but can never silently lose a clinical event;</li>
 *   <li>private notifications resolve the recipient's live sessions at DELIVERY
 *       time, not enqueue time — sessions may come and go in between.</li>
 * </ul>
 *
 * The method signatures are unchanged from Phase 1: call sites compile and
 * behave identically from their perspective. Payloads are the very DTOs the
 * REST endpoints return, so the frontend can apply them to the TanStack Query
 * cache via setQueryData without any transformation.
 */
@Component
public class RealtimeEventPublisher {

    private final OutboxService outboxService;

    public RealtimeEventPublisher(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    /** New append-only timeline event for a patient. */
    public void publishTimelineEvent(UUID patientId, Object event) {
        outboxService.enqueue(
                OutboxEvent.TYPE_REALTIME_BROADCAST,
                "TIMELINE_EVENT",
                patientId,
                RealtimeTopics.patientTimeline(patientId),
                null,
                event);
    }

    /** Live vital reading for a patient. */
    public void publishVital(UUID patientId, Object vital) {
        outboxService.enqueue(
                OutboxEvent.TYPE_REALTIME_BROADCAST,
                "VITAL",
                patientId,
                RealtimeTopics.patientVitals(patientId),
                null,
                vital);
    }

    /** Task state change on a nurse's shift queue. */
    public void publishTaskUpdate(UUID assignedNurseId, Object task) {
        outboxService.enqueue(
                OutboxEvent.TYPE_REALTIME_BROADCAST,
                "TASK",
                null,
                RealtimeTopics.nurseQueue(assignedNurseId),
                null,
                task);
    }

    /** Task state change scoped to a patient workspace. */
    public void publishPatientTaskUpdate(UUID patientId, Object task) {
        if (patientId == null) {
            return;
        }
        outboxService.enqueue(
                OutboxEvent.TYPE_REALTIME_BROADCAST,
                "TASK",
                patientId,
                RealtimeTopics.patientTasks(patientId),
                null,
                task);
    }

    /** Private notification delivered to every live session of the recipient. */
    public void publishNotification(UUID userId, Object notification) {
        outboxService.enqueue(
                OutboxEvent.TYPE_REALTIME_BROADCAST,
                "NOTIFICATION",
                null,
                null,
                userId,
                notification);
    }
}