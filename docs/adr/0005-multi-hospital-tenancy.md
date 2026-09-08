# ADR-0005: Multi-Hospital Tenancy

**Status:** Accepted
**Date:** 2026-09-08
**Phase:** 7

## Context

CareFlow started as a single-facility deployment: every user, patient, and
clinical record lived in one global namespace. Real deployments serve hospital
groups and networks, where data isolation between facilities is a hard
requirement — a Riverside clinician must never see (or mutate) CareFlow
General's patients, even by guessing IDs.

## Decision

### 1. Shared-schema, discriminator-column tenancy

A single database with a `hospitals` table and a `hospital_id` foreign key on
`users` and `patients` (migration `V10__multi_hospital_tenancy.sql`). Rows are
scoped by a discriminator column rather than schema-per-tenant or
database-per-tenant.

Why: the existing H2/Postgres stack, Flyway migrations, and entity graph stay
unchanged; adding a database per tenant would multiply connection pools,
migrations, and operational cost for a feature whose isolation need is
application-level. A discriminator column plus enforced query scoping gives
the same correctness at a fraction of the operational surface.

### 2. Scoping rules (enforced in `PatientService`)

- **Listings** (`/patients/my`, admin listings) filter by
  `patientRepository.findByHospitalId(actor.getHospital().getId())`.
- **Point reads and writes** pass through `ensureAccess`, which applies the
  tenant boundary FIRST — cross-hospital access is rejected with
  "Access denied … different hospital" before any assignment/access-grant
  logic runs.
- **Creation** inherits the creating user's hospital; a client cannot choose
  the tenant of a new patient.
- **NULL hospital = unscoped legacy row.** Seeded data predating tenancy keeps
  working: users/patients with `hospital_id IS NULL` are visible to everyone
  and never trip the boundary check. This makes the migration zero-downtime
  and backward compatible; V10 back-fills seeded users/patients to their
  facilities.

### 3. Hospital data must survive the security session

The `User` entity becomes the JWT security principal (`CustomUserDetails`),
which outlives the authentication-time persistence session. Tenancy checks
read `principal.getHospital()` on that *detached* entity — with a lazy
`@ManyToOne` this explodes with `could not initialize proxy … no Session`.

`UserRepository.findByEmail` therefore uses `@EntityGraph(attributePaths =
"hospital")` so the principal carries real tenant data, not an uninitialized
proxy. This is the subtlest part of the design and is pinned by the
integration tests.

### 4. Tenant data in API responses

`PatientResponse` exposes `hospitalId` / `hospitalName` so the frontend can
badge patients per facility; the DTO mapping is done in
`PatientService.toResponse` off the eagerly joinable association.

## Consequences

- Cross-tenant isolation is enforced server-side at the service layer; adding
  a new patient-scoped endpoint means remembering `ensureAccess` (single choke
  point for point operations).
- Hospital-scoped admins: an admin with a hospital sees only their facility;
  the seeded unscoped admin keeps global visibility.
- Future work: hospital management UI, tenant-aware caching keys (Phase 3
  cache entries are currently patient-scoped, which is already tenant-safe),
  and scoping of timeline/notification fan-out queries if facilities ever
  share patient care.
