# Changelog

All notable changes to the **CareFlow AI** platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.2.0] - 2026-10-12

### Added
- **Optimistic Concurrency Control**: Flyway migration `V12__optimistic_locking_version.sql` adding `@Version` fields to `Patient`, `Task`, and `MedicationAdministration` entities.
- **Frontend 409 Interceptor**: Automatic detection and user notification of concurrent update conflicts.
- **AI Explainability Grounding**: Dedicated UI panel in Copilot side panel displaying timeline grounding event IDs and prompt audit logs.
- **Bucket4j Rate Limiting**: Token-bucket rate limiting filter for `/auth/register` and AI inference endpoints.
- **Role Isolation Test Suite**: End-to-end `SecurityRoleAuthorizationTest` verifying RBAC boundaries for unauthenticated users, caregivers, patients, nurses, and doctors.
- **Frontend Test Suite**: Vitest and React Testing Library setup with unit tests for `ProtectedRoute`.
- **OpenAPI 3 / Swagger UI**: Integrated SpringDoc OpenAPI UI at `/swagger-ui/index.html`.
- **Observability**: Standardized `GlobalExceptionHandler` with correlation IDs, structured access logs, and health probes.

### Changed
- **LTS Java 21 Upgrade**: Aligned backend build and container targets to Java 21 LTS bytecode.
- **PII Scrubbing**: Sanitized patient names and identifiers prior to Gemini LLM prompt generation.
- **Documentation**: Rewrote README with system architecture diagrams, live demo references, and relative documentation links.
- **Privacy Terminology**: Replaced all HIPAA claims with "privacy-scoped access" and explicit synthetic data notices.

### Fixed
- Fixed runtime crash in `TaskWidget.jsx` during mutation error handling.
- Fixed `parseTemperatureToFahrenheit` in `VitalThresholdService` to accurately evaluate Celsius inputs against threshold limits.
- Fixed N+1 query and heap memory pressure in `DashboardService` via bounded page requests.
- Resolved race condition in missed medication caregiver notifications.
