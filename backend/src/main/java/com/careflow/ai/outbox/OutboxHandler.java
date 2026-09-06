package com.careflow.ai.outbox;

/**
 * A single event-type processor invoked by the {@link OutboxRelay} after the
 * event's originating transaction has committed.
 *
 * Handlers MUST be idempotent: the relay guarantees at-least-once delivery,
 * so a handler may see the same event twice after a crash between dispatch
 * and state transition.
 */
public interface OutboxHandler {

    /** The OutboxEvent.eventType this handler processes. */
    String eventType();

    /**
     * Processes the event. Throwing signals a retryable failure — the relay
     * reschedules the row with exponential backoff until the attempt budget
     * is exhausted, then parks it as FAILED for forensics.
     */
    void handle(OutboxEvent event) throws Exception;
}
