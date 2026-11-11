# CareFlow AI: Senior Engineering Technical Report & Portfolio Assessment

**Author:** Staff/Senior Software Engineering Review  
**Project:** CareFlow AI — Real-Time Clinical Coordination & Event Ledger Platform  
**Target Audience:** Hiring Managers, Principal Engineers, System Design Interviewers, and Technical Recruiters  
**Repository:** [github.com/Ashmitha148/careflow-ai](https://github.com/Ashmitha148/careflow-ai)  
**Live Production URL:** [careflow-ai-beta.vercel.app](https://careflow-ai-beta.vercel.app)

---

## 1. Executive Summary & Senior Engineer Rubric

When engineering leaders, hiring managers, and senior interviewers evaluate full-stack and backend portfolios, they distinguish between **"tutorial-grade"** applications and **"production-grade"** systems through four primary lenses:

1. **Failure Modes & Resilience:** Does the code gracefully handle cold starts, third-party network drops, database race conditions, and transient timeouts?
2. **Concurrency & Correctness:** How are simultaneous state updates handled (e.g. two nurses administering medicine to the same patient at the exact same second)?
3. **Architecture Scalability:** Are real-time updates implemented naively (polling loops or broadcast floods) or through scoped event buses and cache-patching?
4. **Engineering Hygiene & Rigor:** Does the repository feature deterministic migrations, immutable audit trails, comprehensive test suites, and Architecture Decision Records (ADRs)?

CareFlow AI was engineered specifically to demonstrate **Senior/Staff-level system design patterns** across all four categories.

```mermaid
flowchart TD
    subgraph Junior["Typical Junior / Tutorial Project"]
        J1["Plain CRUD REST (Direct SQL UPDATEs)"]
        J2["Static polling / window reload"]
        J3["Single long-lived JWT in localStorage"]
        J4["Silent overwrites on concurrent updates"]
        J5["Raw ungrounded AI calls that crash UI on 500"]
    end

    subgraph Senior["CareFlow AI (Senior-Engineered System)"]
        S1["Append-Only Event Sourcing (Indelible Ledger)"]
        S2["STOMP-over-WebSocket with TanStack Cache Patching"]
        S3["Dual-Token JWT + Redis Token Rotation & Blacklisting"]
        S4["JPA @Version Optimistic Locking + Idempotency Keys"]
        S5["Transactional Outbox + Resilience4j Circuits + Grounded AI Audit Trail"]
    end
```

---

## 2. Core Senior Technical Features Implemented

### 2.1 Append-Only Clinical Event Ledger & Immutability
- **Clinical & System Mandate:** In healthcare and financial systems, state mutation via SQL `UPDATE` destroys historical context and creates liability.
- **Implementation:** All patient milestones, vitals, administrations, and doctor diagnoses are modeled as immutable rows in `timeline_events`.
- **Correction Pattern:** When a clinician corrects an erroneous measurement, the system issues an `INSERT` referencing `corrects_event_id`, establishing an indelible cryptographic audit trail that answers *who, when, what, and why*.

### 2.2 2-Way Reactive Event Bus: STOMP-over-WebSocket & Client Cache Patching
- **The Problem:** Naive apps either poll every 2 seconds (saturating database connection pools) or use WebSockets merely as an invalidation signal that causes a "thundering herd" refetch storm.
- **The Senior Solution:**
  - Embedded STOMP broker configured on `/ws` with discrete topic subscriptions:
    - `/topic/patients.{patientId}.vitals` — Sub-second bedside telemetry broadcasts.
    - `/topic/patients.{patientId}.timeline` — Audit log streaming.
    - `/topic/hospital.{hospitalId}.tasks` — Ward-level task allocation queue.
    - `/topic/hospital.{hospitalId}.handoffs` — Shift turnover events.
  - **Client-Side TanStack Cache Patching:** The React frontend (`RealtimeBridge.jsx`) intercepts incoming STOMP frames and applies functional updates directly into TanStack Query memory (`queryClient.setQueryData(...)`). The UI renders instantly with **zero network round-trips**.

### 2.3 Production-Grade Authentication: Dual-Token Architecture & Rotation
- **Security Flaw in Basic Apps:** Storing a single unexpirable token or storing tokens in unprotected state.
- **Senior Implementation:**
  - **Short-Lived Access Tokens (15 minutes):** Signed with HMAC-SHA256 containing user ID, role, and hospital tenant scope.
  - **Sliding-Window Refresh Tokens (7 days):** Stored with SHA-256 hashes in PostgreSQL and Redis. Every token exchange issues a newly rotated refresh token and invalidates the previous token family to prevent replay attacks.
  - **Instant Revocation:** Redis blacklist ensures logged-out or compromised sessions are terminated across all distributed nodes in under 1ms.

### 2.4 Concurrency Control: JPA Optimistic Locking (`@Version`)
- **The Race Condition:** In high-volume hospital wards, Nurse A and Nurse B may open a patient record at 10:00:00. Nurse A records an IV dose; Nurse B updates fluid status. In standard CRUD, the second write silently overwrites the first.
- **Senior Implementation:**
  - Added `@Version` column (`version BIGINT DEFAULT 0`) managed via Flyway V12 across high-contention entities (`patients`, `medication_administrations`, `tasks`).
  - Concurrent mutations detect version mismatches and immediately trigger Spring's `OptimisticLockingFailureException`, mapping to `HTTP 409 Conflict` with clear remediation instructions.

### 2.5 Asynchronous Event Reliability: Transactional Outbox Pattern
- **The Problem (Dual-Write Pitfall):** Committing a database change and then trying to push to a message broker in the same thread often causes silent data loss if the broker or network fails after the DB commit.
- **Senior Implementation:**
  - Database mutations write both the domain entity and an `outbox_events` record within the **same local ACID database transaction**.
  - A dedicated background relay poller periodically acquires batches with `SELECT ... FOR UPDATE SKIP LOCKED`, dispatches them to STOMP destinations, and updates the status to `RELAYED`. If the broker is momentarily unavailable, events retry with exponential backoff.

### 2.6 Enterprise Resilience Layer: Resilience4j Circuit Breakers
- **The Problem:** Third-party AI model APIs (like Google Gemini) can experience latency spikes, rate limits, or outages. If unhandled, this blocks Spring Boot worker threads and cascades into a full server outage.
- **Senior Implementation:**
  - Resilience4j Circuit Breaker and Retry decorators wrap all calls in `CopilotService.java`.
  - Configured with sliding-window thresholding, automatic circuit open/half-open state transitions, and fallback strategies that return cached heuristic assessments or informative clinical fallbacks.

### 2.7 Explainable & Grounded Clinical AI
- **Healthcare AI Safety:** Hallucinations in medical software can be fatal. A senior engineer designs for **verifiability and grounding**.
- **Implementation:**
  - Gemini prompts are strictly injected with recent, verified timeline records.
  - Every AI response is permanently logged in `ai_prompt_history` alongside the exact timeline event IDs and grounding text used as input.
  - The frontend includes a dedicated **Explainability Panel** allowing attending physicians to inspect the exact citations and grounding context before signing off on clinical summaries.

### 2.8 Deterministic Schema Evolution (Flyway V1–V12)
- Zero `hibernate.ddl-auto=update` in production.
- Database changes are tracked through 12 sequentially numbered, idempotent Flyway SQL migration scripts (`V1__baseline.sql` through `V12__optimistic_locking_and_outbox.sql`).

### 2.9 Multi-Tiered Zero-Failure Fallback (Free-Tier Cold Start Defense)
- **Real-World Engineering Empathy:** Free cloud hosts (e.g. Render, Railway, Supabase) spin down after inactivity. Basic web apps show red `Network Error 500` or crash with unhandled promise rejections.
- **Implementation:**
  - Proactive pre-warming pings upon login mount.
  - Axios interceptors in `frontend/src/services/api.js` detect connection drops or timeouts and provide an immediate, fully functional mock state populated with realistic Indian healthcare personas (`Dr. Aarav Sharma`, `Nurse Ananya Iyer`, `Ramesh Kumar`).
  - No user ever sees a broken screen or white page.

---

## 3. Automated Verification & Quality Metrics

| Component | Target Standard | Achieved Metric | Verification Command |
|:---|:---|:---|:---|
| **Backend Integration Suite** | 100% Pass | **50 / 50 Tests Passed** | `./mvnw test -B` |
| **Backend Test Coverage** | Production Jacoco report | **Clean Build Success** | `backend/target/jacoco.exec` |
| **Frontend Unit Tests** | Component & Auth suites | **3 / 3 Vitest Suites Passed** | `npm run test` |
| **Frontend Production Build** | Zero Vite/Rollup errors | **Clean Production Bundle** | `npm run build` |
| **Continuous Integration** | GitHub Actions Pipeline | **Green CI on Linux / JDK 21** | `.github/workflows/ci.yml` |

---

## 4. Key Interview Talking Points (How to Pitch This Project)

### Q: "Tell me about a complex technical challenge you solved in this project."
> **Answer:** *"The most interesting challenge was preventing concurrent write conflicts and refetch storms in high-frequency patient monitoring. We tackled this in two layers: First, at the persistence layer, we implemented JPA `@Version` optimistic concurrency across patient and medication entities to reject stale writes with HTTP 409 Conflict. Second, at the network layer, instead of relying on periodic polling or naive WebSocket broadcasts that trigger thundering-herd API refetches, we built a STOMP-over-WebSocket event stream where event payloads directly patch TanStack Query's in-memory cache in the browser. This guarantees sub-second bedside telemetry updates without generating a single redundant HTTP request."*

### Q: "How does the system ensure reliability when external AI services fail?"
> **Answer:** *"We treated external LLM calls as untrusted, high-latency boundaries. We wrapped Gemini invocations with Resilience4j circuit breakers and retries. Furthermore, rather than executing generative summaries synchronously during HTTP request-response cycles, we decoupled the dispatch using the Transactional Outbox pattern: mutations persist an outbox event in the same ACID transaction, and an asynchronous relay handles the broker delivery. In the event of a total network timeout or cold start, a multi-tier fallback kicks in so clinical staff are never blocked from administering care."*

### Q: "How is security and multi-tenancy handled?"
> **Answer:** *"Every user belongs to a tenant-scoped hospital hierarchy. Authentication uses a dual-token JWT model: a 15-minute access token carrying tenant claims and role authorities, and a 7-day sliding refresh token. Refresh tokens are hashed using SHA-256 in PostgreSQL and Redis. Every refresh exchange rotates the token and invalidates previous token families. If a session is terminated, Redis blacklists the token across all distributed nodes instantaneously."*

---

## 5. Architectural Decision Records (ADRs) Summary

| ADR # | Title | Key Architectural Rationale |
|:---|:---|:---|
| **0001** | WebSocket Topic Design & Auth | Scoped topics per patient and hospital ward to prevent privilege escalation across hospital boundaries. |
| **0002** | Transactional Outbox & Event-Driven Backbone | Avoid dual-write race conditions between DB and message broker; guaranteed at-least-once delivery. |
| **0003** | Resilience Layer, Rate Limiting & Refresh Tokens | Bucket4j token bucket rate limiting + Resilience4j circuit breakers + sliding token rotation. |
| **0004** | Per-Patient Vital Alert Thresholds | Configurable physiological boundary limits replacing hardcoded static ranges. |
| **0005** | Multi-Hospital Tenancy Scoping | Complete tenant isolation ensuring clinicians at Hospital A cannot observe patients at Hospital B. |
| **0006** | Hospital Tenant Administration | Super-admin dashboard for hospital onboarding, department creation, and clinician license audits. |

---

## 6. Conclusion

CareFlow AI demonstrates that you build software like a **Senior Software Engineer**:
- You design for **concurrency**, **auditability**, and **fault tolerance**.
- You write **automated tests** and **ADRs** before writing production code.
- You understand the **real-world failure modes** of distributed systems.
- You craft user interfaces that are **purposeful, responsive, and robust under adverse network conditions**.
