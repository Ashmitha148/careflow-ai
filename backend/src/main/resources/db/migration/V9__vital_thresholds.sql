-- =============================================================================
-- V9 — Per-patient vital alert thresholds (Phase 5)
--
-- Care teams can override the global critical-vital rules per patient (e.g. a
-- COPD patient's oxygen floor is 88%, not 90%). One row per
-- (patient, vital type); missing rows fall back to the hardcoded clinical
-- defaults in VitalService. All bounds are nullable so only the relevant
-- bound(s) for a vital type are set:
--   - OXYGEN:      min_value   (alert when value < min_value)
--   - TEMPERATURE: max_value   (°F, alert when value > max_value)
--   - SUGAR:       min_value / max_value (mg/dL)
--   - BLOOD_PRESSURE: systolic_max / diastolic_max (alert when either exceeds)
-- =============================================================================

CREATE TABLE vital_thresholds (
    id            UUID PRIMARY KEY,
    patient_id    UUID NOT NULL REFERENCES patients(id),
    vital_type    VARCHAR(32) NOT NULL CHECK (vital_type IN ('OXYGEN','BLOOD_PRESSURE','TEMPERATURE','SUGAR')),
    min_value     DOUBLE PRECISION,
    max_value     DOUBLE PRECISION,
    systolic_max  DOUBLE PRECISION,
    diastolic_max DOUBLE PRECISION,
    created_by    UUID REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_vital_thresholds_patient_type UNIQUE (patient_id, vital_type)
);

CREATE INDEX idx_vital_thresholds_patient ON vital_thresholds(patient_id);