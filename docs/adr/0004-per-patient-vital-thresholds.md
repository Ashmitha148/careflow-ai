# ADR 0004 — Per-patient vital alert thresholds

- **Status:** Accepted
- **Date:** 2026-09-08
- **Phase:** 5 — Clinical safety configuration

## Context

Critical-vital detection (`VitalService.isCritical`) used hardcoded global
defaults: SpO2 < 90 %, BP > 180/120 mmHg, temperature > 103 °F — and blood
sugar was never evaluated at all. Real clinical practice needs different
limits per patient: a COPD patient routinely sits at 88–91 % SpO2 and a
baseline 190/95 hypertensive must not page the on-call nurse twice a night,
while a brittle diabetic's glucose range must be monitored even though no
global default exists.

Two further requirements shaped the design:

1. **Explainability** — when a CRITICAL_ALERT appears on the timeline, the
   "why was this triggered?" detail must show the *active* threshold, which
   is now patient-specific.
2. **Safety of defaults** — if no override exists, the system must behave
   exactly as before (global clinical defaults). Overrides may only be added
   by doctors, and every change must be audited.

## Decision

- **Storage:** new `vital_thresholds` table (migration `V9`), one row per
  `(patient_id, vital_type)`, with nullable `min_value`, `max_value`,
  `systolic_max`, `diastolic_max`. Blood pressure keeps two independent
  bounds because its value is a "systolic/diastolic" pair. Absence of a row
  means "use global defaults".
- **Resolution:** `VitalThresholdService` resolves the override for the
  vital's patient first and falls back to the hardcoded defaults. A separate
  service (rather than logic inside `VitalService`) keeps the dependency
  graph acyclic: `TimelineService` needs threshold *descriptions* for the
  alert detail, and `VitalService` already depends on `TimelineService`.
- **Temperature units:** `Vital.value` carries no unit column; overrides are
  authored in °F and unqualified values are compared in °F (existing
  convention). Values qualified with `C` are converted to °F first.
- **API:** `GET/PUT/DELETE /clinical/vitals/{patientId}/thresholds/{type}` —
  reads for any authenticated user, writes for `DOCTOR` only
  (`@PreAuthorize`), all writes audit-logged via `AuditLogService`. PUT
  validates that the bound(s) a vital type requires are present (BP needs
  both systolic and diastolic maxima) and rejects unknown vital types with
  400.
- **Explainability:** `describeThreshold` became an instance method that
  renders the override when one exists (e.g. "patient-specific: oxygen
  saturation < 92%") and the global default otherwise, so the timeline
  alert detail always states the rule that actually fired.
- **Error mapping fix:** while wiring the role guard we found
  `@PreAuthorize` denials fell into the `RuntimeException` catch-all of
  `GlobalExceptionHandler` and were reported as 400. A dedicated
  `AccessDeniedException` handler now returns a proper 403.

## Consequences

- **Accepted:** thresholds are per patient + type only — no per-nurse or
  time-windowed escalation tiers. The row-model keeps the first iteration
  simple; tiers can extend the same table later.
- **Accepted:** SUGAR alerts only exist where a doctor configured them.
  This is deliberate — a wrong global glucose default paging every ward
  would be worse than quiet until configured.
- Tests pin the full behavior in
  `backend/src/test/java/com/careflow/ai/Phase5ThresholdIntegrationTest.java`:
  override tightening, override relaxation, SUGAR activation, the
  patient-specific timeline detail, role guards, validation errors, and
  DELETE reverting to defaults.
