-- CareFlow AI Seed Data (V2)

-- ============================================================================
-- 1. SEED USERS (5 Users: 2 Doctors, 2 Nurses, 1 Caregiver)
-- BCrypt Hash for 'password123': $2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm
-- ============================================================================
INSERT INTO users (id, email, password_hash, full_name, role, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'sarah.jenkins@careflow.ai', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Dr. Sarah Jenkins', 'DOCTOR', CURRENT_TIMESTAMP - INTERVAL '30' DAY),
('22222222-2222-2222-2222-222222222222', 'michael.chen@careflow.ai', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Dr. Michael Chen', 'DOCTOR', CURRENT_TIMESTAMP - INTERVAL '30' DAY),
('33333333-3333-3333-3333-333333333333', 'emily.watson@careflow.ai', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Nurse Emily Watson', 'NURSE', CURRENT_TIMESTAMP - INTERVAL '30' DAY),
('44444444-4444-4444-4444-444444444444', 'james.rivera@careflow.ai', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Nurse James Rivera', 'NURSE', CURRENT_TIMESTAMP - INTERVAL '30' DAY),
('55555555-5555-5555-5555-555555555555', 'maria.rodriguez@careflow.ai', '$2b$12$nNtMHIypAkcXRxdjGq0RHO8wWy1Wh29ACxUyIKJdSu1l39grwK4jm', 'Maria Rodriguez', 'CAREGIVER', CURRENT_TIMESTAMP - INTERVAL '30' DAY);

-- ============================================================================
-- 2. SEED PATIENTS (4 Patients)
-- ============================================================================
INSERT INTO patients (id, mrn, name, dob, gender, contact_info, assigned_doctor_id, assigned_nurse_id, created_at) VALUES
('a1111111-1111-1111-1111-111111111111', 'MRN-1001', 'Eleanor Vance', '1948-03-15', 'Female', '555-0192 | 124 Elm St, Springfield', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '10' DAY),
('a2222222-2222-2222-2222-222222222222', 'MRN-1002', 'Robert Thorne', '1962-07-22', 'Male', '555-0144 | 88 Maple Ave, Oakdale', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '8' DAY),
('a3333333-3333-3333-3333-333333333333', 'MRN-1003', 'Clara Oswald', '1985-11-04', 'Female', '555-0177 | 42 Rose Lane, Riverdale', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '5' DAY),
('a4444444-4444-4444-4444-444444444444', 'MRN-1004', 'Arthur Pendelton', '1955-09-30', 'Male', '555-0188 | 701 Pine Rd, Highland', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- ============================================================================
-- 3. PATIENT ACCESS RELATIONSHIPS
-- ============================================================================
INSERT INTO patient_access (patient_id, user_id, relationship) VALUES
('a1111111-1111-1111-1111-111111111111', '55555555-5555-5555-5555-555555555555', 'PRIMARY_CAREGIVER');

-- ============================================================================
-- 4. DIAGNOSES
-- ============================================================================
INSERT INTO diagnoses (id, patient_id, doctor_id, "condition", notes, diagnosed_at) VALUES
('d1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Congestive Heart Failure (NYHA Class III)', 'Exacerbation with 3+ bilateral pedal edema and dyspnea on minimal exertion.', CURRENT_TIMESTAMP - INTERVAL '10' DAY),
('d1111111-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Type 2 Diabetes Mellitus', 'Currently managed with oral hypoglycemics.', CURRENT_TIMESTAMP - INTERVAL '10' DAY),
('d2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'Post-Op Coronary Artery Bypass Graft (CABG x3)', 'Day 4 post-surgery. Sternal wound intact.', CURRENT_TIMESTAMP - INTERVAL '8' DAY),
('d3333333-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Acute Asthma Exacerbation', 'Triggered by seasonal allergies. Wheezing present bilaterally.', CURRENT_TIMESTAMP - INTERVAL '5' DAY),
('d4444444-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 'COPD Exacerbation with Right Lower Lobe Pneumonia', 'Productive cough, fever, SpO2 dropping below 90% on room air.', CURRENT_TIMESTAMP - INTERVAL '3' DAY);

-- ============================================================================
-- 5. MEDICATIONS & ADMINISTRATIONS
-- ============================================================================
INSERT INTO medications (id, patient_id, prescribed_by, name, dosage, frequency, start_date, end_date, status) VALUES
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Furosemide (Lasix)', '40mg', 'IV Twice Daily', CURRENT_DATE - 10, NULL, 'ACTIVE'),
('b1111111-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'Lisinopril', '10mg', 'Oral Daily', CURRENT_DATE - 10, NULL, 'ACTIVE'),
('b2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'Warfarin', '5mg', 'Oral Every Evening', CURRENT_DATE - 7, NULL, 'ACTIVE'),
('b3333333-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Albuterol Sulfate Nebulizer', '2.5mg/3mL', 'Inhalation Every 4 Hours PRN', CURRENT_DATE - 5, NULL, 'ACTIVE'),
('b4444444-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', 'Ceftriaxone', '1g', 'IV Daily', CURRENT_DATE - 3, NULL, 'ACTIVE');

INSERT INTO medication_administration (id, medication_id, administered_by, administered_at, status, notes) VALUES
('ba111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, 'GIVEN', 'Administered IV push slowly over 2 mins. Patient tolerated well.'),
('ba222222-1111-1111-1111-111111111111', 'b2222222-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '14' HOUR, 'GIVEN', 'Evening dose administered after dinner.'),
('ba444444-1111-1111-1111-111111111111', 'b4444444-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '6' HOUR, 'MISSED', 'Patient was undergoing portable chest X-ray during scheduled infusion time.');

-- ============================================================================
-- 6. VITALS
-- ============================================================================
INSERT INTO vitals (id, patient_id, recorded_by, "type", "value", recorded_at) VALUES
('c1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'BLOOD_PRESSURE', '155/95', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('c1111111-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'OXYGEN', '93%', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),
('c2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'BLOOD_PRESSURE', '128/82', CURRENT_TIMESTAMP - INTERVAL '3' HOUR),
('c3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'OXYGEN', '96%', CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('c4444444-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'OXYGEN', '89%', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
('c4444444-2222-2222-2222-222222222222', 'a4444444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'TEMPERATURE', '38.8C', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE);

-- ============================================================================
-- 7. TIMELINE EVENTS (Includes original & correction event for append-only audit)
-- ============================================================================
INSERT INTO timeline_events (id, patient_id, event_type, description, corrects_event_id, created_by, created_at) VALUES
('e1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 'DIAGNOSIS', 'Diagnosed with NYHA Class III Congestive Heart Failure', NULL, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP - INTERVAL '10' DAY),
('e1111111-2222-2222-2222-222222222222', 'a1111111-1111-1111-1111-111111111111', 'VITAL', 'Blood Pressure recorded as 165/98 mmHg', NULL, '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '3' HOUR),
-- Correction Event referencing previous event 'e1111111-2222-2222-2222-222222222222'
('e1111111-3333-3333-3333-333333333333', 'a1111111-1111-1111-1111-111111111111', 'VITAL', 'Correction: Re-measured Blood Pressure 155/95 mmHg after 15 mins rest (corrected prior 165/98 reading)', 'e1111111-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', CURRENT_TIMESTAMP - INTERVAL '2' HOUR),

('e2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', 'MEDICATION_ADMINISTRATION', 'Given Warfarin 5mg oral dose', NULL, '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '14' HOUR),

('e3333333-1111-1111-1111-111111111111', 'a3333333-3333-3333-3333-333333333333', 'MEDICATION', 'Prescribed Albuterol Sulfate Nebulizer 2.5mg', NULL, '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP - INTERVAL '5' DAY),

('e4444444-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', 'CRITICAL_ALERT', 'SpO2 dropped to 89% on room air with fever of 38.8C. Oxygen therapy started at 2L/min nasal cannula.', NULL, '44444444-4444-4444-4444-444444444444', CURRENT_TIMESTAMP - INTERVAL '30' MINUTE);

-- ============================================================================
-- 8. TASKS
-- ============================================================================
INSERT INTO tasks (id, patient_id, created_by_doctor_id, assigned_nurse_id, title, description, due_at, status, created_at, completed_at) VALUES
('f1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'Assess Pedal Edema & Weight', 'Record daily weight before breakfast and measure bilateral ankle circumference.', CURRENT_TIMESTAMP + INTERVAL '2' HOUR, 'PENDING', CURRENT_TIMESTAMP - INTERVAL '4' HOUR, NULL),
('f2222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'Check Morning INR Lab Results', 'Review INR lab draw for Warfarin dosing adjustment.', CURRENT_TIMESTAMP - INTERVAL '1' HOUR, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '5' HOUR, CURRENT_TIMESTAMP - INTERVAL '1' HOUR),
('f4444444-1111-1111-1111-111111111111', 'a4444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', 'Re-check SpO2 post 2L Oxygen', 'Verify oxygen saturation 15 minutes after initiating nasal cannula.', CURRENT_TIMESTAMP + INTERVAL '15' MINUTE, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL '15' MINUTE, NULL);

-- ============================================================================
-- 9. SHIFT HANDOFFS
-- ============================================================================
INSERT INTO shift_handoffs (id, patient_id, from_nurse_id, to_nurse_id, notes, pending_tasks, observations, completed_tasks, next_shift_instructions, ai_summary, shift_date) VALUES
('fa111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', 'Patient Eleanor Vance reported mild shortness of breath when walking to bathroom.', 'Check daily weight and evaluate pedal edema.', 'Fluid retention resolving slowly with IV Lasix. BP slightly elevated at 155/95.', 'IV Furosemide 40mg administered at 08:00.', 'Keep fluid restriction to 1.5L/day. Report if weight increases by >2lbs.', 'AI Summary: Eleanor Vance (78F) CHF NYHA Class III. Stable post IV Lasix. BP 155/95 requires monitoring.', CURRENT_DATE);

-- ============================================================================
-- 10. AI PROMPT HISTORY
-- ============================================================================
INSERT INTO ai_prompt_history (id, patient_id, user_id, feature_name, prompt, retrieved_context, ai_response, response_status, created_at) VALUES
('ab111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'CLINICAL_COPILOT', 'Summarize recent clinical changes and risk factors for Eleanor Vance over the past 24 hours.', 'Vitals: BP 155/95, SpO2 93%. Meds: Furosemide 40mg IV. Diagnoses: CHF Class III.', 'Based on recent vitals, Eleanor Vance exhibits elevated systolic BP (155 mmHg) and borderline SpO2 (93%). IV Furosemide is taking effect. Recommended action: Monitor urine output, repeat vitals in 2h, maintain fluid restriction.', 'SUCCESS', CURRENT_TIMESTAMP - INTERVAL '1' HOUR);

-- ============================================================================
-- 11. NOTIFICATIONS
-- ============================================================================
INSERT INTO notifications (id, user_id, "type", message, "read", created_at) VALUES
('da444444-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 'CRITICAL_VITAL', 'CRITICAL ALERT: Arthur Pendelton (MRN-1004) SpO2 dropped to 89%.', FALSE, CURRENT_TIMESTAMP - INTERVAL '30' MINUTE),
('da333333-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'TASK_ASSIGNED', 'New Task: Assess Pedal Edema & Weight for Eleanor Vance.', TRUE, CURRENT_TIMESTAMP - INTERVAL '4' HOUR);

-- ============================================================================
-- 12. APPOINTMENTS
-- ============================================================================
INSERT INTO appointments (id, patient_id, doctor_id, scheduled_at, status) VALUES
('ac111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', CURRENT_TIMESTAMP + INTERVAL '1' DAY, 'SCHEDULED'),
('ac222222-1111-1111-1111-111111111111', 'a2222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP + INTERVAL '2' DAY, 'SCHEDULED');
