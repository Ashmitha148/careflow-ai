-- ============================================================================
-- V6: Transactional outbox, idempotency keys, async AI handoff state (Phase 2)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. outbox_events — transactional outbox.
--    Every domain event that must reach the broker/WebSocket layer is written
--    HERE, in the SAME transaction as the business row (e.g. timeline_events).
--    The relay (OutboxRelay) publishes these after commit, so a broker outage
--    can never lose or duplicate a clinical event: rows are retried with
--    exponential backoff until processed. Append-only by design.
-- ----------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    event_type     VARCHAR(64)  NOT NULL,
    aggregate_type VARCHAR(64),
    aggregate_id   UUID,
    topic          VARCHAR(255),
    user_id        UUID,
    payload        TEXT         NOT NULL,
    correlation_id VARCHAR(64),
    status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at   TIMESTAMP
);

-- Relay claim query: status = PENDING AND next_attempt_at <= now
CREATE INDEX idx_outbox_pending ON outbox_events (status, next_attempt_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);

-- ----------------------------------------------------------------------------
-- 2. idempotency_keys — retry protection for mutating clinical endpoints.
--    A retried request carrying the same Idempotency-Key replays the stored
--    response instead of double-writing a clinical record.
-- ----------------------------------------------------------------------------
CREATE TABLE idempotency_keys (
    id                  UUID PRIMARY KEY,
    idem_key            VARCHAR(128) NOT NULL,
    endpoint            VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64)  NOT NULL,
    response_status     INT,
    response_body       TEXT,
    status              VARCHAR(16)  NOT NULL DEFAULT 'PROCESSING',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency_key UNIQUE (idem_key)
);

CREATE INDEX idx_idempotency_expiry ON idempotency_keys (expires_at);

-- ----------------------------------------------------------------------------
-- 3. shift_handoffs — async AI summary lifecycle (Gemini moves off the
--    request thread; POST returns 202 while a worker computes the summary).
-- ----------------------------------------------------------------------------
ALTER TABLE shift_handoffs ADD COLUMN ai_status VARCHAR(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE shift_handoffs ADD COLUMN ai_error VARCHAR(512);

-- Rows created before this migration already carry a synchronous summary.
UPDATE shift_handoffs SET ai_status = 'DONE' WHERE ai_summary IS NOT NULL;
