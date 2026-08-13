-- CareFlow AI Phase 5B: Patient Role & Extended Demo Data

-- ============================================================================
-- 1. PATIENT USER (Eleanor Vance can log in as herself)
-- ============================================================================
INSERT INTO users (id, email, password_hash, full_name, role, created_at) VALUES
('66666666-6666-6666-6666-666666666666', 'eleanor.vance@email.com', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Eleanor Vance', 'PATIENT', CURRENT_TIMESTAMP - INTERVAL '10' DAY);

-- Link patient record to user account
INSERT INTO patient_access (patient_id, user_id, relationship) VALUES
('a1111111-1111-1111-1111-111111111111', '66666666-6666-6666-6666-666666666666', 'SELF');

-- ============================================================================
-- 2. UPDATE PATIENTS WITH REMOTE SUPERVISION FLAGS (V4 fields)
-- ============================================================================
UPDATE patients SET remote_supervision_enabled = true, caregiver_physically_present = false WHERE id = 'a1111111-1111-1111-1111-111111111111';
UPDATE patients SET remote_supervision_enabled = false, caregiver_physically_present = true WHERE id = 'a2222222-2222-2222-2222-222222222222';
UPDATE patients SET remote_supervision_enabled = true, caregiver_physically_present = false WHERE id = 'a3333333-3333-3333-3333-333333333333';
UPDATE patients SET remote_supervision_enabled = false, caregiver_physically_present = false WHERE id = 'a4444444-4444-4444-4444-444444444444';

-- ============================================================================
-- 3. UPDATE MEDICATIONS WITH IMPORTANT FLAG (V3 fields)
-- ============================================================================
UPDATE medications SET important = true WHERE id = 'b1111111-1111-1111-1111-111111111111'; -- Furosemide - important for CHF
UPDATE medications SET important = true WHERE id = 'b1111111-2222-2222-2222-222222222222'; -- Lisinopril - important for BP
UPDATE medications SET important = false WHERE id = 'b2222222-1111-1111-1111-111111111111'; -- Warfarin
UPDATE medications SET important = true WHERE id = 'b3333333-1111-1111-1111-111111111111'; -- Albuterol - important for asthma
UPDATE medications SET important = false WHERE id = 'b4444444-1111-1111-1111-111111111111'; -- Ceftriaxone

-- ============================================================================
-- 4. ADDITIONAL MEDICATIONS
-- ============================================================================
INSERT INTO medications (id, patient_id, prescribed_by, name, dosage, frequency, start_date, end_date, status, important) VALUES
('b5555555-5555-5555-5555-555555555555', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Metformin', '500mg', 'Oral Twice Daily', CURRENT_DATE - 10, NULL, 'ACTIVE', true),
('b6666666-6666-6666-6666-666666666666', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'Aspirin', '81mg', 'Oral Daily', CURRENT_DATE - 7, NULL, 'ACTIVE', false),
('b7777777-7777-7777-7777-777777777777', 'a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Prednisone', '20mg', 'Oral Daily', CURRENT_DATE - 5, CURRENT_DATE + 5, 'ACTIVE', true),
('b8888888-8888-8888-8888-888888888888', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 'Azithromycin', '500mg', 'Oral Daily', CURRENT_DATE - 3, CURRENT_DATE + 4, 'ACTIVE', false);

-- ============================================================================
-- 5. ADDITIONAL MEDICATION ADMINISTRATIONS
-- ============================================================================
INSERT INTO medication_administration (id, medication_id, administered_by, administered_at, status, notes) VALUES
('ba333333-3333-3333-3333-333333333333', 'b1111111-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '8' HOUR, 'GIVEN', 'Lisinopril 10mg oral. BP stable.'),
('ba555555-5555-5555-5555-555555555555', 'b3333333-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'GIVEN', 'Albuterol nebulizer administered. Patient reports improved breathing.'),
('ba666666-6666-6666-6666-666666666666', 'b5555555-5555-5555-5555-555555555555', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '12' HOUR, 'GIVEN', 'Metformin 500mg with breakfast. Blood glucose 142 mg/dL.'),
('ba777777-7777-7777-7777-777777777777', 'b1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 'MISSED', 'Patient refused IV Furosemide. Will retry in 2 hours.'),
('ba888888-8888-8888-8888-888888888888', 'b7777777-7777-7777-7777-777777777777', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '6' HOUR, 'GIVEN', 'Prednisone 20mg oral. Take with food.');

-- ============================================================================
-- 6. ADDITIONAL TASKS
-- ============================================================================
INSERT INTO tasks (id, patient_id, created_by_doctor_id, assigned_nurse_id, title, description, due_at, status, created_at, completed_at) VALUES
('f5555555-5555-5555-5555-555555555555', 'a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'Administer Albuterol Nebulizer', 'Give 2.5mg/3mL albuterol via nebulizer. Monitor for tremors.', CURRENT_TIMESTAMP + INTERVAL '1' HOUR, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '2' HOUR, NULL),
('f6666666-6666-6666-6666-666666666666', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'Check Blood Glucose', 'Fingerstick glucose check before lunch.', CURRENT_TIMESTAMP + INTERVAL '3' HOUR, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, NULL),
('f7777777-7777-7777-7777-777777777777', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'Administer Ceftriaxone IV', '1g IV over 30 mins. Check for allergic reaction.', CURRENT_TIMESTAMP + INTERVAL '30' MINUTE, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE, NULL),
('f8888888-8888-8888-8888-888888888888', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'Check INR Results', 'Review morning INR lab. Adjust Warfarin if needed.', CURRENT_TIMESTAMP + INTERVAL '4' HOUR, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '3' HOUR, NULL),
('f9999999-9999-9999-9999-999999999999', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'Daily Weight Check', 'Record weight before breakfast. Alert if >2lb gain.', CURRENT_TIMESTAMP + INTERVAL '1' DAY, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '6' HOUR, NULL);

-- ============================================================================
-- 7. ADDITIONAL APPOINTMENTS
-- ============================================================================
INSERT INTO appointments (id, patient_id, doctor_id, scheduled_at, status) VALUES
('ac333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP + INTERVAL '3' DAY, 'SCHEDULED'),
('ac444444-4444-4444-4444-444444444444', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP + INTERVAL '4' DAY, 'SCHEDULED'),
('ac555555-5555-5555-5555-555555555555', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP + INTERVAL '7' DAY, 'SCHEDULED');

-- ============================================================================
-- 8. ADDITIONAL TIMELINE EVENTS
-- ============================================================================
INSERT INTO timeline_events (id, patient_id, event_type, description, corrects_event_id, created_by, created_at) VALUES
('e5555555-5555-5555-5555-555555555555', 'a2222222-2222-2222-2222-222222222222', 'MEDICATION_ADMINISTRATION', 'Warfarin 5mg given orally in evening. INR pending.', NULL, '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '14' HOUR),
('e6666666-6666-6666-6666-666666666666', 'a3333333-3333-3333-3333-333333333333', 'VITAL', 'Peak flow measured at 350 L/min. Improved from 320.', NULL, '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('e7777777-7777-7777-7777-777777777777', 'a1111111-1111-1111-1111-111111111111', 'TASK', 'Daily weight recorded: 68.2kg (down 0.5kg from yesterday).', NULL, '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '4' HOUR),
('e8888888-8888-8888-8888-888888888888', 'a4444444-4444-4444-4444-444444444444', 'MEDICATION_ADMINISTRATION', 'Ceftriaxone 1g IV started at 14:00. No immediate reaction.', NULL, '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
('e9999999-9999-9999-9999-999999999999', 'a1111111-1111-1111-1111-111111111111', 'MEDICATION_ADMINISTRATION', 'Metformin 500mg given with breakfast. Blood glucose stable.', NULL, '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '12' HOUR),
('e0000000-0000-0000-0000-000000000001', 'a2222222-2222-2222-2222-222222222222', 'DIAGNOSIS', 'Post-op CABG x3 recovery progressing well. Sternal wound clean.', NULL, '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP - INTERVAL '2' DAY),
('e0000000-0000-0000-0000-000000000002', 'a3333333-3333-3333-3333-333333333333', 'MEDICATION', 'Prednisone 20mg added for asthma exacerbation. Taper in 5 days.', NULL, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP - INTERVAL '5' DAY);

-- ============================================================================
-- 9. ADDITIONAL SHIFT HANDOFFS
-- ============================================================================
INSERT INTO shift_handoffs (id, patient_id, from_nurse_id, to_nurse_id, notes, pending_tasks, observations, completed_tasks, next_shift_instructions, ai_summary, shift_date) VALUES
('fa222222-2222-2222-2222-222222222222', 'a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 'Patient stable overnight. SpO2 improved to 94% on 2L O2.', 'Continue Ceftriaxone IV. Monitor SpO2 hourly.', 'Fever resolved. Cough productive but less frequent.', 'Chest X-ray completed. IV line patent.', 'Maintain O2 at 2L. Call MD if SpO2 < 92%.', 'AI Summary: Arthur Pendelton (69M) COPD + pneumonia. Responding to antibiotics. SpO2 94% on 2L.', CURRENT_DATE),
('fa333333-3333-3333-3333-333333333333', 'a2222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', '33333333-3333-3333-3333-333333333333', 'Post-op day 5. Patient ambulating with assistance.', 'Check INR before evening Warfarin dose.', 'Sternal incision healing well. No drainage.', 'Ambulated 100ft x2 with PT.', 'Continue Warfarin 5mg. PT/OT daily.', 'AI Summary: Robert Thorne (64M) POD5 CABG x3. Stable. INR pending.', CURRENT_DATE - INTERVAL '1' DAY);

-- ============================================================================
-- 10. ADDITIONAL NOTIFICATIONS
-- ============================================================================
INSERT INTO notifications (id, user_id, "type", message, "read", created_at) VALUES
('da555555-5555-5555-5555-555555555555', '55555555-5555-5555-5555-555555555555', 'MEDICATION_REMINDER', 'Eleanor Vance has a medication due: Metformin 500mg.', FALSE, CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('da666666-6666-6666-6666-666666666666', '55555555-5555-5555-5555-555555555555', 'TASK_COMPLETED', 'Nurse Emily Watson completed: Assess Pedal Edema & Weight.', TRUE, CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('da777777-7777-7777-7777-777777777777', '33333333-3333-3333-3333-333333333333', 'MISSED_MEDICATION', 'URGENT: Furosemide missed for Eleanor Vance. Patient refused.', FALSE, CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('da888888-8888-8888-8888-888888888888', '11111111-1111-1111-1111-111111111111', 'CRITICAL_VITAL', 'Arthur Pendelton SpO2 89%. Oxygen initiated.', FALSE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
('da999999-9999-9999-9999-999999999999', '44444444-4444-4444-4444-444444444444', 'TASK_ASSIGNED', 'New task: Administer Ceftriaxone IV for Arthur Pendelton.', TRUE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE);

-- ============================================================================
-- 11. ADDITIONAL VITALS
-- ============================================================================
INSERT INTO vitals (id, patient_id, recorded_by, "type", "value", recorded_at) VALUES
('c5555555-5555-5555-5555-555555555555', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'HEART_RATE', '78 bpm', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('c6666666-6666-6666-6666-666666666666', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'WEIGHT', '68.2 kg', CURRENT_TIMESTAMP - INTERVAL '4' HOUR),
('c7777777-7777-7777-7777-777777777777', 'a2222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'HEART_RATE', '82 bpm', CURRENT_TIMESTAMP - INTERVAL '3' HOUR),
('c8888888-8888-8888-8888-888888888888', 'a3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'PEAK_FLOW', '350 L/min', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('c9999999-9999-9999-9999-999999999999', 'a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'BLOOD_PRESSURE', '132/84', CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('c0000000-0000-0000-0000-000000000001', 'a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'RESPIRATORY_RATE', '22/min', CURRENT_TIMESTAMP - INTERVAL '1' HOUR);

-- ============================================================================
-- 12. ROLE-BASED DEMO ACCOUNT EMAILS
-- ============================================================================

-- Existing seeded users from V2
UPDATE users
SET email = 'doctor1@careflow.ai',
    full_name = 'Dr. Sarah Jenkins'
WHERE id = '11111111-1111-1111-1111-111111111111';

UPDATE users
SET email = 'doctor2@careflow.ai',
    full_name = 'Dr. Michael Chen'
WHERE id = '22222222-2222-2222-2222-222222222222';

UPDATE users
SET email = 'nurse1@careflow.ai',
    full_name = 'Nurse Emily Watson'
WHERE id = '33333333-3333-3333-3333-333333333333';

UPDATE users
SET email = 'nurse2@careflow.ai',
    full_name = 'Nurse James Rivera'
WHERE id = '44444444-4444-4444-4444-444444444444';

UPDATE users
SET email = 'caregiver@careflow.ai',
    full_name = 'Maria Rodriguez'
WHERE id = '55555555-5555-5555-5555-555555555555';

-- Existing Eleanor patient account
UPDATE users
SET email = 'patient@careflow.ai',
    full_name = 'Eleanor Vance'
WHERE id = '66666666-6666-6666-6666-666666666666';

-- New admin account
INSERT INTO users
(id, email, password_hash, full_name, role, created_at)
VALUES
('77777777-7777-7777-7777-777777777777',
 'admin@careflow.ai',
 '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm',
 'System Administrator',
 'ADMIN',
 CURRENT_TIMESTAMP);

-- New read-only account
INSERT INTO users
(id, email, password_hash, full_name, role, created_at)
VALUES
('88888888-8888-8888-8888-888888888888',
 'readonly@careflow.ai',
 '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm',
 'Dr. Reviewer Smith',
 'READ_ONLY',
 CURRENT_TIMESTAMP);