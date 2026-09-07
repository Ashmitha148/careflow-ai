-- ============================================================================
-- V7: Composite Indexes & Performance Optimization (Phase 3)
-- ============================================================================

-- 1. High-frequency timeline queries by patient and creation date
CREATE INDEX idx_timeline_events_patient_created ON timeline_events (patient_id, created_at DESC);

-- 2. Nurse task queue lookups by nurse, status, and due date
CREATE INDEX idx_tasks_nurse_status_due ON tasks (assigned_nurse_id, status, due_at ASC);

-- 3. Vital series queries by patient and recorded timestamp
CREATE INDEX idx_vitals_patient_recorded ON vitals (patient_id, recorded_at DESC);
CREATE INDEX idx_vitals_patient_type_recorded ON vitals (patient_id, type, recorded_at DESC);

-- 4. Audit log filtering by user and entity with timestamp ordering
CREATE INDEX idx_audit_logs_user_timestamp ON audit_logs (user_id, "timestamp" DESC);
CREATE INDEX idx_audit_logs_entity_timestamp ON audit_logs (entity_type, entity_id, "timestamp" DESC);
