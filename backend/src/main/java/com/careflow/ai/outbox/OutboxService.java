package com.careflow.ai.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Write-side API for the transactional outbox.
 *
 * {@link #enqueue} MUST be called from inside the business transaction that
 * produced the event (REQUIRED propagation joins the caller's transaction).
 * The row commits atomically with the business change — if the transaction
 * rolls back, the event never existed; if it commits, the relay guarantees
 * delivery (at-least-once) even across broker outages and process restarts.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records a domain event for post-commit relay.
     *
     * @param eventType     handler routing key (see OutboxEvent TYPE_ constants)
     * @param aggregateType optional aggregate label for forensics/indexing
     * @param aggregateId   optional aggregate id
     * @param topic         STOMP destination for realtime broadcasts (null for non-broadcast events)
     * @param userId        recipient user for private-queue events (null otherwise)
     * @param payload       payload object; serialized to JSON, never mutated after insert
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void enqueue(
            String eventType,
            String aggregateType,
            UUID aggregateId,
            String topic,
            UUID userId,
            Object payload) {

        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setTopic(topic);
        event.setUserId(userId);
        event.setPayload(toJson(payload));
        // Phase 5 formalizes the correlation-ID filter; reading the MDC key here
        // already propagates it when present so the chain stays traceable.
        event.setCorrelationId(MDC.get("correlationId"));
        repository.save(event);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            // A payload that cannot be serialized is a programming error — fail
            // the business transaction rather than enqueue a corrupt event.
            throw new IllegalStateException("Outbox payload serialization failed", e);
        }
    }
}
