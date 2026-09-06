# ADR 0001 — WebSocket Topic Design, Auth, and Authorization Model

- **Status:** Accepted
- **Date:** 2026-09-06
- **Deciders:** CareFlow AI engineering

## Context

Clinical coordination is inherently real-time: a nurse recording an abnormal
vital needs a doctor to see it *now*, not on the next poll. The clinical state
lives in the append-only `timeline_events` table and related clinical tables,
and the frontend already uses TanStack Query as the single source of client
state. Any real-time layer must therefore (a) not fight the REST + Query
architecture, (b) respect the multi-role access model (DOCTOR / NURSE /
CAREGIVER / PATIENT / ADMIN with per-patient access grants), and (c) never leak
one patient's clinical data to an unauthorized subscriber — a HIPAA-grade
requirement, not a nice-to-have.

Constraints that shaped the decision:

- Browsers cannot set custom headers on a WebSocket upgrade, so the existing
  `Authorization: Bearer` flow does not transfer directly.
- Spring Security's filter chain sees the upgrade as a plain HTTP request; the
  STOMP CONNECT frame arrives *after* the upgrade is complete.
- The deployment mix (Vercel frontend, proxied backends, restrictive hospital
  networks) means raw WebSocket upgrades are not always available.

## Decision

1. **Transport: STOMP over WebSocket** (`spring-boot-starter-websocket`) at
   `/ws`, with SockJS fallback enabled. STOMP gives us subscriptions as a
   first-class concept — the unit of authorization is the subscription, which
   maps perfectly onto "who may watch which patient".

2. **Auth on the handshake.** `JwtHandshakeInterceptor` validates the JWT from
   the `?token=` query parameter, builds an immutable
   `WebSocketPrincipal(userId, role, fullName)`, and rejects the upgrade with
   `401 Unauthorized` when the token is missing, expired, or of the wrong type.
   `/ws/**` is `permitAll` in `SecurityConfig` *only* so the upgrade reaches the
   interceptor — no STOMP session can exist without a valid JWT because the
   interceptor never attaches a principal otherwise. JWT-in-query-string is
   acceptable here because (a) the alternative (cookie auth) drags CSRF back
   into a stateless API, and (b) upgrade URLs are short-lived, TLS-protected,
   and never logged by our own code.

3. **Role-scoped topics, never a firehose:**

   | Destination | Scope | Who may subscribe |
   |---|---|---|
   | `/topic/patient/{id}/timeline` | new timeline events | users with an access grant for that patient (any clinical role) |
   | `/topic/patient/{id}/vitals` | live vitals stream | users with an access grant for that patient |
   | `/topic/nurse/{nurseId}/queue` | shift queue + task assignments | that nurse only (admins get read access) |
   | `/user/queue/notifications` | private notifications | the owning user (Spring resolves `/user` from the session principal) |

4. **Authorize every SUBSCRIBE, then rewrite.** `WebSocketTopicAuthorizer`
   intercepts SUBSCRIBE frames on the inbound client channel: it checks the
   caller's role and `PatientAccess` grants against the requested destination,
   and on success rewrites the destination to a server-private equivalent where
   applicable. Unauthorized SUBSCRIBE frames are dropped — the client never
   receives the subscription's messages. Authorization failures are logged with
   the user id and destination for audit review.

5. **Presence via the same connection.** A lightweight app-level
   (`/app/presence/{patientId}` join/leave) protocol plus a disconnect listener
   maintains a per-patient viewer set in memory and broadcasts join/leave to
   the patient's timeline topic. Presence is ephemeral — it is deliberately
   *not* persisted to the timeline.

6. **Frontend: one client, cache-patching, exponential backoff.**
   `SocketContext` provides a single `RealtimeSocketClient` per session that:
   - reconnects with exponential backoff (capped), re-issuing all desired
     subscriptions on reconnect;
   - maps topics to TanStack Query keys (`realtimeHandlers`) and patches the
     cache with `setQueryData` — events carry the new record, so no refetch is
     needed;
   - is built on `@stomp/stompjs` with a dependency-free SockJS-transport shim
     (`sockJsWebSocket.js`) that speaks the SockJS WebSocket framing directly,
     tolerating both Spring's raw-string datagrams and spec-style JSON arrays.

## Consequences

**Positive**

- Delivery latency for clinical events drops from poll-interval to
  milliseconds, and the REST API no longer absorbs polling traffic.
- Topic-level authorization is enforced in one auditable place; a client that
  guesses a destination gets silence, not data.
- The cache-patch approach means reconnects are self-healing: TanStack Query's
  stale-while-revalidate fetches the gap once on reconnect.

**Negative / accepted trade-offs**

- The in-memory broker (`simple broker`) binds presence and delivery to a
  single backend instance. Horizontal scaling will require switching to a
  STOMP relay (RabbitMQ) — deferred until the event-driven backbone (Phase 2)
  picks the message broker, so the two decisions share one ADR.
- The SockJS shim is custom (~150 lines). It exists because the full
  `sockjs-client` library drags in legacy polyfills; the smoke tests in
  `frontend/scripts/sockjs-smoke.mjs` and `ws-live-smoke.mjs` pin its behavior
  against both mock and live servers.
- JWT-in-query-string slightly increases token exposure surface vs. headers;
  mitigated by short access-token TTL and TLS-only deployment.

## Alternatives considered

- **Raw WebSocket + custom routing** — rejected: we would reimplement
  subscription bookkeeping, and every custom routing rule becomes an
  authorization liability.
- **Server-Sent Events** — rejected: one-way only; presence and future
  collaborative features (acknowledgments) need client→server frames.
- **Long-polling only** — rejected: unacceptable latency for critical vitals
  and higher server load than a persistent connection.
- **sockjs-client library** — rejected for now: unmaintained and polyfill-heavy
  for a transport we only use as a fallback; revisit if non-WebSocket fallback
  transports become a real requirement.

## References

- Topic map: `frontend/src/services/websocket/realtimeChannels.js`
- Server auth: `backend/src/main/java/com/careflow/ai/websocket/WebSocketTopicAuthorizer.java`
- Handshake: `backend/src/main/java/com/careflow/ai/websocket/JwtHandshakeInterceptor.java`
- Live verification: `frontend/scripts/ws-live-smoke.mjs`
