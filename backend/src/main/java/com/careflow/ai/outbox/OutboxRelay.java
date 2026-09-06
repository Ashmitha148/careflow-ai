package com.careflow.ai.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The outbox poller: claims due PENDING rows in one atomic UPDATE, then
 * dispatches each to its {@link OutboxHandler}.
 *
 * Delivery guarantee: at-least-once. A crash between handler completion and
 * the PROCESSED transition re-delivers on the next poll, so handlers must be
 * idempotent (broadcast re-send is harmless; the AI worker checks the handoff's
 * current state before writing).
 *
 * No event is ever lost: rows stay PENDING (with backoff) until a handler
 * succeeds or the attempt budget parks them as FAILED for forensics — a broker
 * or Gemini outage therefore delays delivery but can never drop a clinical
 * event, which is the legal-auditability requirement this table exists for.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final List<OutboxHandler> handlers;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final int maxAttempts;
    private final long backoffBaseMs;

    public OutboxRelay(
            OutboxEventRepository repository,
            List<OutboxHandler> handlers,
            PlatformTransactionManager transactionManager,
            @Value("${careflow.outbox.batch-size:50}") int batchSize,
            @Value("${careflow.outbox.max-attempts:5}") int maxAttempts,
            @Value("${careflow.outbox.backoff-base-ms:2000}") long backoffBaseMs) {
        this.repository = repository;
        this.handlers = handlers;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.backoffBaseMs = backoffBaseMs;
    }

    @Scheduled(fixedDelayString = "${careflow.outbox.poll-interval-ms:2000}")
    public void relayDueEvents() {
        Integer claimed = transactionTemplate.execute(
                tx -> repository.claimDueBatch(LocalDateTime.now(), batchSize));
        if (claimed == null || claimed == 0) {
            return;
        }

        // Claim flips rows to RELAYED; fetch and process them. Cross-instance
        // overlap is safe because handlers are idempotent (see class javadoc).
        List<OutboxEvent> batch =
                repository.findByStatusOrderByCreatedAtAsc(OutboxStatus.RELAYED);
        log.debug("Outbox relay claimed {} event(s)", batch.size());

        for (OutboxEvent event : batch) {
            processOne(event);
        }
    }

    private void processOne(OutboxEvent event) {
        try {
            OutboxHandler handler = handlerFor(event.getEventType());
            handler.handle(event);
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
            repository.save(event);
        } catch (Exception e) {
            int attempts = event.getAttempts(); // already incremented by the claim
            if (attempts >= maxAttempts) {
                log.error(
                        "Outbox event {} ({}) failed permanently after {} attempts — parked as FAILED",
                        event.getId(), event.getEventType(), attempts, e);
                event.setStatus(OutboxStatus.FAILED);
                repository.save(event);
            } else {
                long backoffMs = backoffMs(attempts);
                log.warn(
                        "Outbox event {} ({}) dispatch failed (attempt {}/{}), retrying in {} ms",
                        event.getId(), event.getEventType(), attempts, maxAttempts, backoffMs, e);
                event.setStatus(OutboxStatus.PENDING);
                event.setNextAttemptAt(
                        LocalDateTime.now().plusNanos(backoffMs * 1_000_000));
                repository.save(event);
            }
        }
    }

    private OutboxHandler handlerFor(String eventType) {
        return handlers.stream()
                .filter(h -> h.eventType().equals(eventType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No OutboxHandler registered for event type " + eventType));
    }

    /** Exponential backoff with jitter: base * 2^(attempt-1) + [0, base/2). */
    private long backoffMs(int attempt) {
        long exponential = backoffBaseMs * (1L << Math.min(attempt - 1, 10));
        long jitter = ThreadLocalRandom.current().nextLong(Math.max(1, backoffBaseMs / 2));
        return exponential + jitter;
    }
}
