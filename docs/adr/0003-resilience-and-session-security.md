# ADR 0003 — Resilience Layer: Circuit Breaker, Retry, Rate Limiting, and Refresh-Token Rotation

- **Status:** Accepted
- **Date:** 2026-09-08
- **Deciders:** CareFlow AI engineering

## Context

CareFlow AI depends on external services on its hot paths: Gemini for AI-generated handoff summaries and copilot chat, Cloudinary for clinical document uploads. Every external dependency is a latency and availability liability — an outage or rate limit upstream must not cascade into the clinical UI, and repeated slow AI calls must not pin backend threads. At the same time, the auth surface (`/auth/login`, `/auth/refresh`) is a credential-stuffing target, and long-lived refresh tokens are a session-hijacking prize in a healthcare context.

Requirements:
1. AI calls must degrade gracefully: bounded retries for transient failures, fail-fast once the upstream is demonstrably down, and a hard timeout so requests never hang.
2. Sensitive endpoints need per-client throttling that cannot be bypassed by a single rogue caller.
3. Stolen refresh tokens must be detectable and revocable without breaking legitimate sessions that never present a reused token.

## Decision

1. **Resilience4j pipeline around the Gemini client** (`ResilientAiClient`), applied innermost to outermost *retry → circuit breaker → time limiter*, configured entirely from `application.yml` (`gemini` instance; a `cloudinary` instance mirrors it for uploads):
   - **Retry:** 3 attempts, exponential backoff from 500 ms.
   - **Circuit breaker:** sliding window of 10, minimum 5 calls, 50% failure threshold, 30 s open-state wait, 2 half-open probes. When OPEN, calls fail fast with `CallNotPermittedException`.
   - **Time limiter:** 30 s hard ceiling per composed invocation.
   - Any exhausted failure mode surfaces as `AiUnavailableException`, which callers map to a graceful "AI summary unavailable" response — never a raw 500.
   - The retry chain is wrapped by the breaker, so one composed invocation counts once against the breaker statistics.

2. **Bucket4j per-IP token-bucket rate limiting** (`RateLimitingFilter`):
   - `POST /auth/login`, `POST /auth/refresh` → auth bucket (10/min); `/copilot/**`, `/ai/**` → AI bucket (30/min).
   - Budgets read the same `resilience4j.ratelimiter.instances.*.limit-for-period` properties (env-overridable via `RATE_LIMIT_AUTH` / `RATE_LIMIT_AI`).
   - Buckets are created lazily per client IP (honoring `X-Forwarded-For`) and held in bounded-scope `ConcurrentHashMap`s.
   - Disabled in the `dev` profile used by tests; enforced via the `prod` profile.

3. **Refresh-token rotation with reuse detection** (Flyway V8):
   - Every refresh issues a new token bound to a token *family*; the presented token is marked used atomically.
   - Presenting an already-used token is treated as a theft signal: the **entire family is revoked** and every member token is rejected.
   - The revocation is committed in a **`REQUIRES_NEW` transaction** (programmatic `TransactionTemplate`) because the reuse path immediately throws and rolls back the request transaction — without the independent transaction, the revocation itself would be rolled back with it and reuse detection would silently fail open. This failure mode is pinned by an integration test (`Phase4ResilienceTest.refreshTokenReuseRevokesEntireFamily`).

4. **Test isolation for outbox/AI race tests:** test contexts get per-context in-memory H2 databases (`careflow_${random.uuid}`), so background relays from one cached Spring context can no longer claim rows belonging to another context's test.

## Consequences

**Positive**
- A dead Gemini upstream degrades to fast, graceful unavailability instead of piled-up threads and 500s.
- Credential stuffing and AI-abuse budgets are capped per client IP.
- Stolen refresh-token replay kills the whole session family, while honest clients are unaffected by rotations.
- Exhausted AI failures surface as a typed, handled exception with a stable API contract.

**Negative / Accepted Trade-offs**
- A genuinely open circuit drops AI summaries for up to 30 s after recovery; acceptable versus thread-pool exhaustion.
- The `Transactional` + `REQUIRES_NEW` split adds a second connection acquire on the (rare) reuse path only.
- Per-IP buckets keyed on `X-Forwarded-For` trust the front proxy; fine under the current Vercel/Render topology, worth revisiting if deployed behind untrusted proxies.

## References
- Config: `backend/src/main/resources/application.yml` (`resilience4j` block)
- Wrapper: `backend/src/main/java/com/careflow/ai/config/ResilientAiClient.java`
- Filter: `backend/src/main/java/com/careflow/ai/resilience/RateLimitingFilter.java`
- Rotation: `backend/src/main/java/com/careflow/ai/service/RefreshTokenService.java`
- Migration: `backend/src/main/resources/db/migration/V8__refresh_token_rotation.sql`
- Tests: `backend/src/test/java/com/careflow/ai/Phase4ResilienceTest.java`
