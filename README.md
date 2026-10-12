# CareFlow AI

[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-61DAFB?style=flat&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.4-646CFF?style=flat&logo=vite&logoColor=white)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%7C%20Flyway-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Google Gemini](https://img.shields.io/badge/Gemini-1.5%20Flash-8E75B2?style=flat&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D?style=flat&logo=swagger&logoColor=black)](https://careflow-ai-beta.vercel.app/api/swagger-ui/index.html)
[![Vercel](https://img.shields.io/badge/Vercel-Deployed-000000?style=flat&logo=vercel&logoColor=white)](https://vercel.com/)

> **CareFlow AI** is an AI-assisted clinical coordination platform for hospitals and care teams — connecting Doctors, Nurses, Caregivers, and Patients in one unified, auditable workspace.

🏥 **[Live Demo Application](https://careflow-ai-beta.vercel.app)** — Click **"Enter Demo"** on the login page for instant role-based access.  
📖 **[Interactive Swagger API Docs](https://careflow-ai-beta.vercel.app/api/swagger-ui/index.html)** — Explore and execute REST endpoints live.

---

> [!NOTE]
> **Synthetic Demo Data Only**: CareFlow AI is an engineering portfolio demonstration with **privacy-scoped access control**. All pre-seeded clinical data, MRNs, vitals, and user accounts are synthetic. This application is not a certified healthcare record system.

---

## The Clinical & Engineering Challenge

Hospital care runs on handoffs — physician to triage nurse, day shift to night shift, hospital discharge to family caregiver. Over **80% of preventable adverse events in healthcare stem from miscommunication during care transitions**. Most hospitals rely on siloed EHR software where updates are buried in unstructured text notes, or oral phone calls that leave zero audit trail.

CareFlow solves this with a cohesive architecture:
1. **Append-Only Clinical Ledger**: Medical records are immutable by design. Rather than mutating past rows, corrections append a new record referencing `corrects_event_id`, preserving auditability.
2. **Real-time Event Stream**: STOMP-over-WebSocket pushes abnormal vitals, task assignments, and shift handoffs to role-scoped subscriber destinations in sub-second latency, patching TanStack Query caches directly without refetch storms.
3. **Resilient AI Copilot with Explainability**: Google Gemini 1.5 Flash generates clinical timeline summaries and nurse shift reports. If external AI fails, Resilience4j circuit breakers fail fast and degrade gracefully without blocking patient care. An **Explainability Panel** displays the exact historical events Gemini used for grounding.
4. **Optimistic Concurrency Control**: Double submissions or concurrent nurse administrations are guarded via `@Version` optimistic locking (Flyway V12).

---

## System Architecture

```mermaid
flowchart TD
    subgraph Clients["Frontend Clients (React 18 + Vite)"]
        D["Doctor Workspace"]
        N["Nurse Dashboard"]
        C["Caregiver Portal"]
        P["Patient App"]
    end

    subgraph Edge["Gateway & CDN"]
        Vercel["Vercel SPA Edge / Reverse Proxy (/api/*)"]
    end

    subgraph Core["Spring Boot 3 REST & Realtime Service (Java 21)"]
        Sec["Spring Security + JWT Filter + CorrelationIdFilter"]
        Rate["Bucket4j Token Bucket Rate Limiter"]
        
        subgraph Controllers["REST & WebSocket Controllers"]
            AuthC["AuthController"]
            ClinC["ClinicalController"]
            CopilotC["CopilotController"]
            SocketC["STOMP WebSocket Broker (/ws)"]
        end

        subgraph Resilience["Resilience & Concurrency Layer"]
            R4J["Resilience4j Circuit Breaker + Retry"]
            OptLock["JPA @Version Optimistic Locking"]
            Outbox["Transactional Outbox Relay"]
        end

        subgraph Services["Domain Services Layer"]
            PatSvc["PatientService"]
            VitSvc["VitalThresholdService"]
            MedSvc["MedicationAdministrationService"]
            AiSvc["CopilotService"]
        end
    end

    subgraph Data["Persistence & External Services"]
        PG[(PostgreSQL 16\nFlyway V1-V12)]
        Redis[(Redis Cache)]
        Gemini["Google Gemini 1.5 Flash API"]
        Cloudinary["Cloudinary Video Storage"]
    end

    Clients -->|HTTPS + JWT Bearer| Vercel
    Vercel -->|Proxied REST| Sec
    Clients <-->|WSS STOMP frames| SocketC
    Sec --> Rate --> Controllers
    Controllers --> Resilience --> Services
    Services --> PG
    Services -.-> Redis
    R4J <--> Gemini
    MedSvc -.-> Cloudinary
    Outbox -->|Background Relay| SocketC
```

---

## Role-Based Workspaces & Screenshots

| Doctor Workspace | Nurse Shift Queue | Patient Care Dashboard |
|:---:|:---:|:---:|
| ![Doctor Dashboard](docs/images/doctor-overview.png) | ![Nurse Dashboard](docs/images/nurse-dashboard.png) | ![Patient Dashboard](docs/images/patient-dashboard.png) |
| *Patient workspaces, diagnoses, prescriptions, AI summaries* | *Shift queue, vitals entry, medication administration, AI handoffs* | *Personal care dashboard, medication schedules, video verification* |

---

## One-Click Demo Accounts

All demo accounts use password **`password123`**. On the login screen, click **"Enter Demo"** to authenticate via the real JWT flow instantly.

| Role | Email | Landing Workspace | Permissions |
|:---|:---|:---|:---|
| **Doctor** | `doctor1@careflow.ai` | `/overview` | Prescribe medications, create diagnoses, set vital alert thresholds, trigger AI summaries |
| **Nurse** | `nurse1@careflow.ai` | `/nurse-dashboard` | Record vitals, administer medications, claim tasks, create AI shift handoffs |
| **Caregiver** | `caregiver@careflow.ai` | `/family` | Privacy-scoped visibility into assigned family member's timeline and care plan |
| **Patient** | `patient@careflow.ai` | `/my-care` | View personal timeline, upcoming prescriptions, and video medication verification |
| **Admin** | `admin@careflow.ai` | `/audit` | System audit logs, hospital tenant administration, staff reassignments |

---

## Quickstart (Under 4 Commands)

### Option A: Local Full Stack via Docker Compose
```bash
# 1. Clone repository
git clone https://github.com/Ashmitha148/careflow-ai.git && cd careflow-ai

# 2. Boot PostgreSQL, Spring Boot backend, and React frontend
docker-compose up --build
```
* Frontend available at: `http://localhost:3000`
* Backend REST API at: `http://localhost:8080/api`
* Swagger UI at: `http://localhost:8080/api/swagger-ui/index.html`

### Option B: Local Development
```bash
# 1. Start Backend (Uses H2 in PostgreSQL compatibility mode automatically)
cd backend && ./mvnw spring-boot:run

# 2. In a separate terminal, start Frontend
cd frontend && npm install && npm run dev
```

---

## Key Architectural Decisions

* **Why an Append-Only Timeline?** Clinical safety mandates an indelible audit trail. CareFlow models clinical events as immutable entries in `timeline_events`. Corrections append a new event referencing `corrects_event_id` rather than issuing SQL `UPDATE`s on medical history.
* **Why Transactional Outbox for AI Summaries?** Asynchronous generation of AI shift summaries should never hold open an active HTTP thread or database transaction. Mutations write an `outbox_events` record in the same atomic transaction as the clinical record, and an asynchronous relay executes retries with exponential backoff.
* **Why Client-Side TanStack Query Patching via STOMP?** Rather than using WebSockets merely as an invalidation notification that triggers refetch storms, incoming WebSocket frames carry the new entity payload and patch `queryClient.setQueryData(...)` in-memory.
* **Why Explainability for Clinical AI?** Generative outputs in clinical environments require grounding. Every prompt sent to Gemini is recorded in `ai_prompt_history` alongside the exact timeline events used in the prompt context. Clinicians can view the Explainability panel to verify evidence before signing off.
* **Why Optimistic Locking (`@Version`)?** Concurrent medication administrations by multiple care team members fail fast with HTTP 409 Conflict instead of allowing silent data overwrites.

---

## Documentation Index

* [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Comprehensive technical architecture and data flow.
* [`docs/API.md`](docs/API.md) — REST endpoint specifications, request/response models, and auth contracts.
* [`SECURITY.md`](SECURITY.md) — Privacy-scoped access control model, token rotation, rate limits, and vulnerability reporting.
* [`CONTRIBUTING.md`](CONTRIBUTING.md) — Engineering standards, commit conventions, and branch policies.
* [`CHANGELOG.md`](CHANGELOG.md) — Detailed version and migration history.
* [`INTERVIEW_NOTES.md`](INTERVIEW_NOTES.md) — 2-minute elevator pitch and 10 principal engineering interview questions with answers.
* **Architecture Decision Records (ADRs)**:
  * [ADR 0001: WebSocket Topic Design & Authorization Model](docs/adr/0001-websocket-topic-design.md)
  * [ADR 0002: Transactional Outbox & Event-Driven Backbone](docs/adr/0002-caching-and-performance-strategy.md)
  * [ADR 0003: Resilience Layer, Rate Limiting & Refresh-Token Rotation](docs/adr/0003-resilience-and-session-security.md)
  * [ADR 0004: Per-Patient Vital Alert Thresholds](docs/adr/0004-per-patient-vital-thresholds.md)
  * [ADR 0005: Multi-Hospital Tenancy Scoping](docs/adr/0005-multi-hospital-tenancy.md)
  * [ADR 0006: Hospital (Tenant) Administration](docs/adr/0006-hospital-administration.md)

---

## License

This project is licensed under the [MIT License](LICENSE) — built by [Ashmitha148](https://github.com/Ashmitha148).
