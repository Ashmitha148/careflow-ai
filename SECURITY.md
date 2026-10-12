# Security & Privacy Policy

CareFlow AI is engineered following defence-in-depth principles with privacy-scoped access controls and resilient credential management.

---

## 1. Synthetic Data & Demo Environment Notice

> [!IMPORTANT]
> **Synthetic Data Disclaimer**: CareFlow AI is a technical demonstration and clinical intelligence prototype. All patient records, clinician profiles, physiological metrics, and timeline histories are 100% synthetically generated. This system is not a certified healthcare record solution and must not be used to store genuine Protected Health Information (PHI).

---

## 2. Core Security Controls

### 2.1 Privacy-Scoped Access & Multi-Tenancy
- Method-level authorization via `@PreAuthorize` prevents horizontal privilege escalation.
- Patients and caregivers are restricted to assigned clinical workflows (`/my-care`, `/family`).
- Administrative actions (audit logs, hospital onboarding) require `ROLE_ADMIN`.

### 2.2 Token Management & Session Security
- Short-lived JSON Web Tokens (JWT) for stateless request authorization.
- Refresh tokens are handled via `HttpOnly`, `Secure`, `SameSite=None` or reverse-proxy cookies.
- Server-side revocation and rotation on every refresh request.

### 2.3 AI Safety & Grounding
- Patient identifiers and clinician details are scrubbed prior to dispatching prompts to LLM endpoints.
- AI suggestions feature persistent audit trails in `ai_prompt_history`.
- User interfaces explicitly mark all LLM output with `"AI-generated • Requires clinician review"`.

### 2.4 Rate Limiting & Denial of Service Protection
- Bucket4j token bucket rate limiting on sensitive authentication (`/auth/*`) and AI endpoints.
- Strict Cross-Origin Resource Sharing (CORS) configured from environment variables.
- Standard security headers: `X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`, and strict cache control on authenticated resources.

---

## 3. Reporting a Vulnerability

If you discover a potential security vulnerability within this repository, please do not disclose it publicly. Contact the project maintainers directly via repository issue marked `[SECURITY PRIVATE]` or via email.
