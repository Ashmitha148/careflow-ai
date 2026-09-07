# ADR 0002 — Redis Aggregate Read-Through Caching, Database Tuning, and Cursor Pagination

- **Status:** Accepted
- **Date:** 2026-09-07
- **Deciders:** CareFlow AI engineering

## Context

CareFlow AI serves multi-role clinical teams (doctors, nurses, admins, patients). As clinical volume grows, frequent dashboard requests (`/dashboard/summary`) and unbounded timeline queries can strain database resources and degrade responsiveness.

At the same to preserve healthcare auditability and compliance:
1. The append-only clinical timeline (`timeline_events`) must remain strictly immutable and un-cached to avoid stale clinical feed reads or cache invalidation drift.
2. Dashboard aggregate views (active patients, pending tasks, upcoming appointments, handoffs) require high throughput, low latency reads, and immediate invalidation on state changes.
3. Unbounded `findAll()` database queries must be eliminated across list endpoints.

## Decision

1. **Redis Read-Through Aggregate Caching:**
   - Spring Cache (`@EnableCaching`) configured with a `RedisCacheManager` setting a short 15-second TTL on dashboard aggregate views (`dashboardSummary`).
   - Per-user isolation in cache keys (`resolveCurrentUserCacheKey()`) ensures role and assignment security boundaries are strictly preserved.
   - Explicit invalidation (`@CacheEvict(allEntries = true)`) applied to state-mutating operations across `TaskService`, `AppointmentService`, `ShiftHandoffService`, and `PatientService`.
   - In-memory `ConcurrentMapCacheManager` fallback provided for testing and environments where Redis is temporarily absent.

2. **Strict Timeline Non-Caching Invariant:**
   - The clinical timeline (`TimelineService.getTimeline`) is **never cached**. Timeline reads execute directly against indexed database tables or stream live via WebSockets.

3. **HikariCP Connection Pool & Slow-Query Logging:**
   - Explicit pool tuning per environment in `application.yml`:
     - **Dev:** `maximum-pool-size: 10`, `minimum-idle: 2`, `idle-timeout: 30000ms`, `connection-timeout: 20000ms`.
     - **Prod:** `maximum-pool-size: 30`, `minimum-idle: 10`, `idle-timeout: 600000ms`, `connection-timeout: 30000ms`, `max-lifetime: 1800000ms`.
   - Slow-query threshold logging enabled via Hibernate (`LOG_QUERIES_SLOWER_THAN_MS: 200`) and Logback WARN logging on `org.hibernate.SQL_SLOW`.

4. **Composite Indexes (Flyway V7):**
   - High-frequency query patterns indexed via `V7__composite_indexes.sql`:
     - `idx_timeline_events_patient_created` (`timeline_events(patient_id, created_at DESC)`)
     - `idx_tasks_nurse_status_due` (`tasks(assigned_nurse_id, status, due_at ASC)`)
     - `idx_vitals_patient_recorded` (`vitals(patient_id, recorded_at DESC)`)
     - `idx_vitals_patient_type_recorded` (`vitals(patient_id, type, recorded_at DESC)`)
     - `idx_audit_logs_user_created` (`audit_logs(user_id, created_at DESC)`)
     - `idx_audit_logs_entity_created` (`audit_logs(entity_type, entity_id, created_at DESC)`)

5. **Cursor-Based Pagination for List Endpoints:**
   - Introduced `CursorPageResponse<T>` (`items`, `nextCursor`, `hasMore`, `pageSize`).
   - Updated `timeline`, `vitals`, and `audit_logs` controllers to support `cursor` and `limit` (max 100) parameters, eliminating unbounded `findAll()` queries.

## Consequences

**Positive**
- Dashboard aggregate response times drop from tens of milliseconds to sub-millisecond Redis lookups under load.
- Database connection pool exhaustion risks are eliminated via HikariCP bounds and slow-query logging visibility.
- Key-set cursor pagination prevents deep OFFSET performance degradation on large clinical timelines.

**Negative / Accepted Trade-offs**
- Short TTL (15s) means slight Redis write invalidation overhead across task/appointment mutations; acceptable given clinical state change frequency.

## References
- Flyway Migration: `backend/src/main/resources/db/migration/V7__composite_indexes.sql`
- Cache Config: `backend/src/main/java/com/careflow/ai/config/CacheConfig.java`
- ERD Documentation: `ERD.md`
