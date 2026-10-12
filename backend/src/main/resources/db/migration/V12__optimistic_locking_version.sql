-- V12: Add optimistic locking version column to concurrent clinical mutation tables
-- Ensures idempotent administration and guards against lost updates during double submissions.

ALTER TABLE medication_administration
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
