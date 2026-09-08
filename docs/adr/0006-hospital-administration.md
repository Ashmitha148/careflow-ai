# ADR-0006: Hospital (Tenant) Administration

**Status:** Accepted
**Date:** 2026-09-08
**Phase:** 8

## Context

ADR-0005 introduced multi-hospital tenancy but left hospitals as a
seed-only concept: the `hospitals` table was backfilled by migration V10, and
there was no way to onboard a new facility or move staff between facilities
without direct database access. Real deployments need a tenant-admin lever —
a facility operator must be able to create facilities and reassign staff
through the API.

## Decision

### 1. Admin-only hospital administration API

A `HospitalController` with four endpoints, all gated by `@PreAuthorize("hasRole('ADMIN')")`:

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/hospitals` | List facilities with patient/staff counts |
| `POST` | `/hospitals` | Onboard a new facility |
| `GET` | `/hospitals/{id}/staff` | List a facility's staff roster |
| `PUT` | `/hospitals/{id}/staff/{userId}` | Reassign a staff member (tenant-admin lever) |

Why ADMIN-only: facility onboarding and staff reassignment are governance
operations, not clinical ones. The seeded `admin@careflow.ai` is the tenant
admin; caregivers (DOCTOR/NURSE/PATIENT) get 403.

### 2. Staff reassignment = tenant move

`PUT /hospitals/{id}/staff/{userId}` sets the user's `hospital` to the
target facility. Because Phase 7's tenancy guard scopes every patient query
by the user's hospital, the move is immediately effective:

- The user's patient list becomes new-facility-only.
- Access to the old facility's patients is cut at the next query (no
  explicit revocation needed — the discriminator column does it).
- The user's existing data (timeline events, audit logs, refresh tokens)
  stays with them; only the tenant boundary moves.

### 3. Validation and error handling

- Blank `name` or `code` → 400 (bean validation on `@Valid @RequestBody`).
- Duplicate `code` (case-insensitive) or duplicate `name` → 400
  (`RuntimeException` → global handler).
- Unknown hospital or user on reassignment → 400.
- Cross-hospital patient access → 403 (tenancy guard from Phase 7).

### 4. Audit logging

Every `CREATE_HOSPITAL` and `ASSIGN_HOSPITAL_STAFF` mutation is logged via
`AuditLogService` with the actor's ID, the target entity, and a human-readable
description (e.g. `user=riverside.doctor@careflow.ai, from=Riverside Medical,
to=Test General`).

## Consequences

- **Positive:** Facility onboarding is now a self-service API operation;
  no database access required. Staff moves are immediate and auditable.
- **Negative:** ADMIN is a global role — any admin can see and modify any
  facility. Scoped tenant-admin (admin of facility X only) is deferred.
- **Migration:** none — the `hospitals` table already exists from V10; this
  ADR adds the administration API on top.
