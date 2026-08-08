-- CareFlow AI Initial Database Schema (V1)
-- Compatible with PostgreSQL and H2 (PostgreSQL Mode)

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Patients Table
CREATE TABLE patients (
    id UUID PRIMARY KEY,
    mrn VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    contact_info TEXT,
    assigned_doctor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_nurse_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Patient Access (Family/Caregiver relationships)
CREATE TABLE patient_access (
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship VARCHAR(50) NOT NULL,
    PRIMARY KEY (patient_id, user_id)
);

-- 4. Timeline Events (Append-only core ledger)
CREATE TABLE timeline_events (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    corrects_event_id UUID REFERENCES timeline_events(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Diagnoses
CREATE TABLE diagnoses (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    "condition" VARCHAR(255) NOT NULL,
    notes TEXT,
    diagnosed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Medications
CREATE TABLE medications (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    prescribed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
);

-- 7. Medication Administration Records
CREATE TABLE medication_administration (
    id UUID PRIMARY KEY,
    medication_id UUID NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    administered_by UUID REFERENCES users(id) ON DELETE SET NULL,
    administered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    notes TEXT
);

-- 8. Vitals
CREATE TABLE vitals (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    "type" VARCHAR(50) NOT NULL,
    "value" VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Shift Handoffs
CREATE TABLE shift_handoffs (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    from_nurse_id UUID REFERENCES users(id) ON DELETE SET NULL,
    to_nurse_id UUID REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    pending_tasks TEXT,
    observations TEXT,
    completed_tasks TEXT,
    next_shift_instructions TEXT,
    ai_summary TEXT,
    shift_date DATE NOT NULL
);

-- 10. Tasks
CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    created_by_doctor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_nurse_id UUID REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 11. AI Prompt History & Audit Logs
CREATE TABLE ai_prompt_history (
    id UUID PRIMARY KEY,
    patient_id UUID REFERENCES patients(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    feature_name VARCHAR(100) NOT NULL,
    prompt TEXT NOT NULL,
    retrieved_context TEXT,
    ai_response TEXT,
    response_status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    "type" VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    "read" BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. Audit Logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    "action" VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    "timestamp" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT
);

-- 14. Appointments
CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED'
);

-- 15. File Attachments
CREATE TABLE file_attachments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    cloudinary_url TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Required Indexes
CREATE INDEX idx_patients_mrn ON patients(mrn);
CREATE INDEX idx_patients_name ON patients(name);
CREATE INDEX idx_timeline_events_patient_id ON timeline_events(patient_id);
CREATE INDEX idx_timeline_events_event_type ON timeline_events(event_type);
