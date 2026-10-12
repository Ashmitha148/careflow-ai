-- V11: Immutable event-audit log
-- Every outbox event that is dispatched (PROCESSED or FAILED permanently) gets
-- one row here for clinical/regulatory auditability.  Rows are written by the
-- OutboxRelay and are never updated or deleted.

CREATE TABLE IF NOT EXISTS event_audit (
    id            UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    outbox_id     UUID          NOT NULL,
    event_type    VARCHAR(64)   NOT NULL,
    aggregate_type VARCHAR(64),
    aggregate_id  UUID,
    status        VARCHAR(16)   NOT NULL,   -- DISPATCHED | FAILED
    attempts      INT           NOT NULL,
    correlation_id VARCHAR(64),
    dispatched_at TIMESTAMP     NOT NULL DEFAULT now()
);

-- Index to find all audit rows for a given outbox event (re-delivery forensics).
CREATE INDEX IF NOT EXISTS idx_event_audit_outbox_id ON event_audit (outbox_id);

-- Index to find all audit rows for a given clinical aggregate (patient timeline).
CREATE INDEX IF NOT EXISTS idx_event_audit_aggregate ON event_audit (aggregate_type, aggregate_id);
