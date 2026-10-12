# CareFlow AI — Principal Engineer Interview Notes

> **Target Audience**: Technical Recruiters, Engineering Managers, Principal/Staff System Architects.  
> **Topic**: Production-grade Spring Boot 3 / Java 21 & React 18 Healthcare Continuity Platform.

---

## 1. Two-Minute Elevator Pitch

"CareFlow AI is an AI-assisted clinical coordination platform designed to eliminate the single most dangerous failure point in modern healthcare: **care transitions and clinical handoffs**. Industry data shows that over 80% of preventable adverse medical events happen during patient transfers between doctors, nurses, and family caregivers because clinical notes live in fragmented silos.

To solve this, I designed CareFlow around four foundational engineering principles:

1. **An Immutable, Append-Only Clinical Ledger**: Medical records must never be destructively overwritten. Corrections append a new event referencing the historical event ID, preserving legal and clinical auditability.
2. **Sub-second Event Streaming without Cache Thrashing**: Using STOMP-over-WebSocket with role-scoped destination checks, incoming abnormal vitals and task updates patch TanStack Query client caches directly in memory rather than triggering heavy refetch storms.
3. **Resilient AI with Explainability**: Shift handoffs and patient summaries are generated via Google Gemini 1.5 Flash behind Resilience4j circuit breakers, retries, and timeouts. Crucially, every AI call is de-identified, and all retrieved timeline events used for grounding are persisted and exposed in a clinician Explainability Panel.
4. **Zero-Trust Multi-Role Scoping & Optimistic Concurrency**: Doctors, Nurses, Caregivers, and Patients each operate within strictly authorized domains. We prevent concurrent medication administration race conditions using database-backed `@Version` optimistic locking (Flyway V12), failing fast with HTTP 409 Conflict.

The application is deployed live with a decoupled Vercel SPA and containerized backend, complete with interactive OpenAPI/Swagger documentation, Testcontainers integration testing, and structured correlation tracing."

---

## 2. Top 10 Senior / Principal Architecture Questions & Answers

### Q1: Why did you choose an append-only timeline instead of traditional CRUD updates on clinical events?
**Answer:**
"In healthcare and high-reliability systems, destroying historical data is a severe clinical safety risk and regulatory liability. If a physician enters an initial diagnosis of 'Bacterial Pneumonia' and later discovers the patient actually has 'Pulmonary Embolism', updating or deleting the row destroys the context of why previous medications (e.g. antibiotics) were administered.

In CareFlow AI, all clinical events are appended to `timeline_events`. A correction is simply a new event containing `corrects_event_id` and the reason for correction. When queries reconstruct the timeline, clients see both the original event and the subsequent correction annotation. This gives us immutable audit compliance, idempotent replays, and complete forensic traceability."

---

### Q2: How did you implement and test optimistic locking, and why is it critical for medication administration?
**Answer:**
"In hospital wards, multiple nurses or doctors often work on the same patient simultaneously. If Nurse A and Nurse B both attempt to administer a scheduled dose at 08:00, a traditional application without concurrency controls might execute two database writes, leading to double medication administration.

We solved this in Flyway migration `V12__optimistic_locking_version.sql` by adding a `version BIGINT DEFAULT 0` column to `medication_administration`, `tasks`, and `patients`. JPA entities utilize `@Version private Long version;`. If two transactions attempt to update the same record concurrently, Hibernate detects that the entity version in memory is stale and throws an `OptimisticLockException`. In `GlobalExceptionHandler`, this is mapped to HTTP 409 Conflict with code `CONCURRENT_MODIFICATION_CONFLICT`. On the frontend, Axios intercepts 409 responses and informs the clinician that the record was updated concurrently, prompting a clean UI refresh."

---

### Q3: How do you handle AI failures and guarantee clinical uptime when Google Gemini is unavailable?
**Answer:**
"A fundamental rule in clinical software is: **Never block a patient workflow on an external AI model.** AI is an assistive tool, not a hard dependency.

We wrapped all Gemini API calls inside `ResilientAiClient`, which composes three Resilience4j primitives configured in `application.yml`:
1. **TimeLimiter**: Caps latency at 30 seconds to prevent thread starvation.
2. **Retry with Exponential Backoff**: 3 attempts with 500ms initial wait for transient network hiccups.
3. **CircuitBreaker**: Monitors failure rates; if Gemini errors exceed 50% over a sliding window, the breaker opens, immediately short-circuiting calls with `AiUnavailableException`.

In `CopilotService`, if an `AiUnavailableException` is caught, we degrade gracefully by returning the raw underlying timeline data with a clear notification: *'AI summary unavailable: The AI service is temporarily down. Please review raw documented events.'* The HTTP status remains 200 OK so clinicians are never locked out of vital patient data."

---

### Q4: How does CareFlow address AI hallucinations and provide clinical explainability?
**Answer:**
"LLMs are prone to hallucinations if ungrounded. We tackle this with a three-layer defense:
1. **Strict Grounding System Prompt**: Our system instructions explicitly state: *'Use ONLY the patient information supplied in the context. Do not invent diagnoses, medications, or measurements. If context is insufficient, explicitly state so.'*
2. **Context Persistence in `ai_prompt_history`**: Every call persists the exact feature name, prompt text, response, and the exact serialized timeline events retrieved from PostgreSQL.
3. **Clinician Explainability Panel**: In the UI Copilot side drawer, clinicians can click the **'Explainability'** tab. This retrieves `/api/copilot/patient/{id}/history`, allowing doctors and nurses to inspect the exact timeline events Gemini used as factual grounding for that specific summary."

---

### Q5: How do you ensure patient data privacy before sending prompts to third-party LLMs?
**Answer:**
"Even in demo environments with synthetic data, clinical systems must enforce privacy-by-design. Before prompts are dispatched to Google Gemini:
1. **PII De-Identification**: Patient names are stripped; prompts only include the synthetic internal UUID (`De-identified Patient ID: ...`).
2. **Author Role Anonymization**: Clinician author names in timeline records are sanitized to prevent employee identity leakage.
3. **Disclaimers**: All frontend views displaying AI summaries render a prominent amber badge: `AI-generated • Requires clinician review`."

---

### Q6: How does the real-time STOMP WebSocket architecture scale without causing client refetch storms?
**Answer:**
"Many applications implement WebSockets simply by sending a 'data changed' ping, causing every connected client to fire multiple GET requests to the REST API simultaneously — creating an avalanche known as a **refetch storm**.

In CareFlow:
1. **Payload-Bearing Messages**: Our Spring Boot broker publishes the updated entity directly inside the STOMP message payload on topic `/topic/patient/{patientId}/vitals` or `/tasks`.
2. **TanStack Query In-Memory Patching**: `RealtimeBridge.jsx` listens to the active subscription. When a new vital arrives, instead of calling `queryClient.invalidateQueries()`, it invokes `queryClient.setQueryData(QK.patientVitals(id), (old) => [newVital, ...old])`.
3. **Role-Scoped Authorization**: STOMP `SUBSCRIBE` frames are intercepted by `JwtHandshakeInterceptor` and `PatientAccessChecker` to verify that the subscribing user has access to that specific patient."

---

### Q7: What is your JWT authentication and refresh-token strategy, especially across decoupled domains?
**Answer:**
"We use short-lived access tokens (15 minutes) signed with HMAC-SHA512 and long-lived refresh tokens (7 days).
1. **Token Discrimination**: `JwtTokenProvider` embeds a `token_type` claim (`access` vs `refresh`). A refresh token cannot be passed in an `Authorization: Bearer` header to invoke clinical endpoints.
2. **Refresh Family Rotation**: Each refresh token belongs to a rotation family. When exchanged, the old refresh token is invalidated and a new one is issued. If a previously used refresh token is presented again (indicating token theft), the entire family is immediately revoked.
3. **Cross-Domain vs Proxy Alignment**: For decoupled Vercel SPA and Render backend setups, Vercel rewrites proxy `/api/*` requests same-origin, eliminating cross-domain cookie restrictions. We also configure Axios with `withCredentials: true` and CORS `allowedHeaders` including `X-Correlation-Id` and `Idempotency-Key`."

---

### Q8: How did you optimize database queries and prevent N+1 performance bottlenecks?
**Answer:**
"During our performance audit, we identified and resolved several query risks:
1. **Dashboard Activity Query**: `DashboardService.resolveRecentActivity` previously queried all historical timeline events for accessible patients and truncated in memory. We updated `TimelineEventRepository` to accept a `Pageable` and applied database-level `PageRequest.of(0, 5)` to eliminate heap bloat.
2. **Entity Graphs**: On `PatientRepository.findAll` and search queries, we declared `@EntityGraph(attributePaths = {"assignedDoctor", "assignedNurse", "hospital"})`, fetching relationships in a single `LEFT JOIN` instead of firing 3 queries per patient row.
3. **Deterministic Cursor Pagination**: Replaced arbitrary UUID cursor comparisons with compound `(createdAt, id)` criteria queries, ensuring deterministic results across billions of events without table scans."

---

### Q9: How do you handle cold starts on serverless or idle container deployments?
**Answer:**
"When hosting microservices on free tiers (like Render), inactive containers spin down to save resources, requiring 30–60 seconds to cold start upon receiving a connection.
Rather than allowing the frontend to crash with an uninformative `Network Error` or generic timeout:
1. Our Axios response interceptor catches connection aborts and timeout codes.
2. In `Login.jsx`, we surface a proactive status notice: *'The demo backend is waking up from idle (free tier). Please wait ~30 seconds and try again!'*
3. The root application is wrapped in a custom `ErrorBoundary.jsx` component that isolates crashes and provides a 1-click reload button rather than a blank white screen."

---

### Q10: What is the testing strategy for CareFlow AI?
**Answer:**
"We employ the testing pyramid:
1. **Unit Tests (Mockito + JUnit 5)**: Isolated testing of domain services (`VitalThresholdService` unit conversion, `CopilotService` de-identification, `AuthService` token generation) targeting 70%+ branch coverage via JaCoCo.
2. **Integration Tests (Testcontainers + Spring Boot Test)**: Verifying full Spring Security filters, Flyway migrations, and JPA query specifications against real containerized PostgreSQL instances rather than H2 in-memory mocks.
3. **E2E Tests (Playwright)**: Automating the critical user journeys: Doctor logging in and prescribing medication, Nurse recording vitals and creating a shift handoff, and Patient viewing their care dashboard."
