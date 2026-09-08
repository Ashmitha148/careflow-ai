-- =============================================================================
-- V10 — Multi-hospital tenancy (Phase 7)
--
-- Shared-schema, row-scoped tenancy: every user and patient belongs to a
-- hospital. Access control adds a tenant check on top of the existing
-- role/assignment rules — an admin only sees patients of their own hospital,
-- and cross-hospital assignments are rejected. Rows with a NULL hospital are
-- legacy/unscoped and keep pre-tenancy behaviour.
-- =============================================================================

CREATE TABLE hospitals (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    code       VARCHAR(32)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Two demo tenants: the default hospital owns ALL pre-existing rows.
INSERT INTO hospitals (id, name, code) VALUES
('c0000000-0000-0000-0000-000000000001', 'CareFlow General', 'CGH'),
('c0000000-0000-0000-0000-000000000002', 'Riverside Clinic', 'RVS');

ALTER TABLE users    ADD COLUMN hospital_id UUID REFERENCES hospitals(id);
ALTER TABLE patients ADD COLUMN hospital_id UUID REFERENCES hospitals(id);

-- Backfill: everything that exists today belongs to the default hospital.
UPDATE users    SET hospital_id = 'c0000000-0000-0000-0000-000000000001';
UPDATE patients SET hospital_id = 'c0000000-0000-0000-0000-000000000001';

CREATE INDEX idx_users_hospital    ON users(hospital_id);
CREATE INDEX idx_patients_hospital ON patients(hospital_id);

-- A second tenant with its own staff and patient, to demonstrate isolation
-- (the CareFlow General admin must NOT be able to reach Robert Riverside).
INSERT INTO users (id, email, password_hash, full_name, role, hospital_id, created_at) VALUES
('d0000000-0000-0000-0000-000000000001',
 'riverside.doctor@careflow.ai',
 '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm',  -- 'password123'
 'Dr. Alan Riverside',
 'DOCTOR',
 'c0000000-0000-0000-0000-000000000002',
 CURRENT_TIMESTAMP);

INSERT INTO patients (id, mrn, name, dob, gender, contact_info,
                      assigned_doctor_id, hospital_id, created_at) VALUES
('b0000000-0000-0000-0000-000000000001',
 'MRN-RVS-0001',
 'Robert Riverside',
 '1958-03-14',
 'Male',
 '555-0100 (home)',
 'd0000000-0000-0000-0000-000000000001',
 'c0000000-0000-0000-0000-000000000002',
 CURRENT_TIMESTAMP);
