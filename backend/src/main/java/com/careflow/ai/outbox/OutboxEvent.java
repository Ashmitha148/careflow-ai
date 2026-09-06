package com.careflow.ai.outbox;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A transactional-outbox row (see V6__outbox_idempotency_async_ai.sql).
 *
 * Written in the SAME transaction as the business change it describes; the
 * relay publishes it only after that transaction commits. Append-only by
 * design: rows are never mutated in place except for the relay's bookkeeping
 * columns (status/attempts/timestamps) — payload content is immutable.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public static final String TYPE_REALTIME_BROADCAST = "REALTIME_BROADCAST";
    public static final String TYPE_HANDOFF_AI_SUMMARY = "HANDOFF_AI_SUMMARY";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    /** Destination STOMP topic for realtime events (informational; authoritative routing lives in the payload). */
    @Column(name = "topic", length = 255)
    private String topic;

    @Column(name = "user_id")
    private UUID userId;

    /** JSON-serialized event payload. Never mutated after insert. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** Propagated from the originating request so the event can be traced end-to-end. */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public OutboxEvent() {}

    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
