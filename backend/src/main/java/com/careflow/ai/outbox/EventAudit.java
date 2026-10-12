package com.careflow.ai.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit record written by {@link OutboxRelay} every time an outbox
 * event is dispatched (successfully or permanently failed).
 *
 * <p>Rows are append-only: they are never updated or deleted. This table provides
 * the regulatory-grade audit trail required by clinical systems — every clinical
 * event that entered the system can be traced from creation (outbox_events) through
 * delivery (event_audit) without any gap.
 *
 * <p>Schema lives in {@code V11__event_audit.sql}.
 */
@Entity
@Table(name = "event_audit")
public class EventAudit {

    /** Terminal status: the handler completed without exception. */
    public static final String STATUS_DISPATCHED = "DISPATCHED";

    /** Terminal status: the relay exhausted its attempt budget. */
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to the originating {@code outbox_events} row (not a DB FK — intentional;
     *  the outbox row may be retained or pruned independently). */
    @Column(name = "outbox_id", nullable = false)
    private UUID outboxId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "aggregate_type", length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    /** DISPATCHED or FAILED — see constants above. */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /** How many attempts the relay made before this terminal state. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt = LocalDateTime.now();

    public EventAudit() {}

    public UUID getId() { return id; }
    public UUID getOutboxId() { return outboxId; }
    public void setOutboxId(UUID outboxId) { this.outboxId = outboxId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public void setAggregateId(UUID aggregateId) { this.aggregateId = aggregateId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
}
