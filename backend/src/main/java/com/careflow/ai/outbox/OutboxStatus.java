package com.careflow.ai.outbox;

/**
 * Lifecycle of a transactional-outbox row.
 *
 * PENDING  — committed with the business transaction, awaiting relay/processing.
 * RELAYED  — handed to the external broker (Redis Streams); awaiting consumer.
 *            Only used when careflow.outbox.transport=redis.
 * PROCESSED— all handlers completed successfully; terminal, safe to purge.
 * FAILED   — retry budget exhausted; kept for forensics, alerting hooks fire.
 */
public enum OutboxStatus {
    PENDING,
    RELAYED,
    PROCESSED,
    FAILED
}
