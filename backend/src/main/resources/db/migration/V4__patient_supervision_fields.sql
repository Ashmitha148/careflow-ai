-- V4 migration: add remote supervision fields to patients
ALTER TABLE patients ADD COLUMN remote_supervision_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE patients ADD COLUMN caregiver_physically_present BOOLEAN NOT NULL DEFAULT FALSE;