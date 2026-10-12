# CareFlow AI - API Reference & Contract Guide

The CareFlow AI REST API conforms to OpenAPI 3.0 standards and exposes interactive documentation at `/swagger-ui/index.html`.

---

## 1. Interactive Swagger UI

When running the backend locally or in production:
- **Swagger UI**: [`/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Schema (JSON)**: [`/v3/api-docs`](http://localhost:8080/v3/api-docs)

---

## 2. Global Error Envelope

All API endpoints return errors in a standardized JSON format:

```json
{
  "timestamp": "2026-10-12T10:00:00.000Z",
  "status": 403,
  "code": "FORBIDDEN",
  "error": "Forbidden",
  "message": "Access denied for role PATIENT on target resource",
  "path": "/tasks",
  "traceId": "9b3c4a1e-87ef-4e4b-9721-a58d84a77df1",
  "fieldErrors": []
}
```

---

## 3. Core Resource Endpoints

### 3.1 Authentication & Session
- `POST /auth/login`: Authenticates clinical users and returns a JWT token with HttpOnly refresh cookie.
- `POST /auth/refresh`: Rotates refresh tokens and issues fresh access tokens.
- `POST /auth/logout`: Invalidates session and clears credentials.

### 3.2 Clinical Operations
- `GET /patients`: Retrieves patient roster scoped to the authenticated clinician. Supports `cursor`, `limit`, and `status`.
- `GET /patients/{id}`: Detailed patient profile, active care team, and vital thresholds.
- `POST /clinical/vitals`: Ingests vital sign records. Triggers threshold evaluation and outbox alert generation.
- `POST /clinical/medications`: Prescribes medication regimens (Doctor/Admin only).
- `POST /tasks`: Dispatches nursing care tasks with optimistic locking.

### 3.3 Clinical Copilot & AI
- `POST /api/copilot/summarize`: Synthesizes clinical timeline into an actionable shift summary.
- `GET /api/copilot/patient/{id}/history`: Returns prompt history and timeline grounding event IDs for AI explainability.

### 3.4 Administration & Observability
- `GET /audit-logs`: Retrieves immutable audit trail (Admin only). Supports cursor pagination.
- `GET /actuator/health`: Spring Actuator liveness and readiness probe status.
