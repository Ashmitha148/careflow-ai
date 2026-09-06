# CareFlow AI

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Flyway-4169E1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Gemini](https://img.shields.io/badge/Gemini-1.5%20Flash-8E75B2?style=flat&logo=googlegemini&logoColor=white)](https://ai.google.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![Vercel](https://img.shields.io/badge/Vercel-Deployed-000000?style=flat&logo=vercel&logoColor=white)](https://vercel.com/)

> An AI-assisted clinical coordination platform for hospitals and care teams — connecting Doctors, Nurses, Caregivers, and Patients in one unified workspace.

**[Live Demo](https://careflow-ai-beta.vercel.app)** — click "Enter Demo" on the login page, no signup needed.

---

## Problem & Engineering Challenge

Hospitals run on handoffs — doctor to nurse, day shift to night shift, hospital to family. Every handoff is a place where information gets lost, because most systems either lock data in role-specific silos or rely on someone remembering to make a phone call. CareFlow solves this with a single system where every action — a diagnosis, a dose given, a vital sign taken — writes to one shared, append-only timeline that every authorized role can see through their own lens.

The hardest engineering problems weren't the CRUD screens. They were: designing a clinical data model where corrections never destroy history (required for any real medical record system), building a JWT refresh flow that's transparent to the user even when a token silently expires mid-session, and getting five different role-based dashboards to all read from the same source of truth without duplicating logic.

---

## Screenshots

### Doctor Dashboard
![Doctor Dashboard](doctor-overview.png)

### Nurse Dashboard
![Nurse Dashboard](nurse-dashboard.png)

### Patient Care Dashboard
![Patient Dashboard](patient-dashboard.png)

---

## What is CareFlow?

CareFlow is a full-stack healthcare coordination system built to solve a problem every care team faces: information lives in silos. Nurses don't know what the doctor decided. Caregivers don't know what happened on the night shift. Patients have no window into their own care.

CareFlow brings everyone into the same room — with role-specific views, an AI copilot for clinical summaries and shift handoffs, and an append-only patient timeline that makes every action fully auditable.

---

## One-Click Demo

You don't need credentials to explore the app. On the login page, click **"Enter Demo"** and pick a role. You will be logged in immediately using a real seeded account through the standard JWT auth flow.

| Role | What you can explore |
|---|---|
| **Doctor** | Patient workspaces, diagnoses, medication prescriptions, AI summaries, task management |
| **Nurse** | Shift queue, vitals recording, medication administration, AI shift handoff reports |
| **Caregiver** | Family member care plan, timeline, medication schedule, appointments |
| **Patient** | Personal care dashboard, my medications, upcoming appointments, video verification |

> Demo accounts use pre-seeded synthetic data and real JWT authentication. No real patient information is ever used.

---

## Features

**Append-only clinical timeline** — Every diagnosis, medication, vital sign, task, and handoff writes to one immutable event log per patient. Corrections reference the original event instead of overwriting it, preserving a complete, audit-safe history.

**Role-based workspaces** — Doctors, Nurses, Caregivers, Patients, and Admins each land on a purpose-built dashboard, enforced at the route level, with permissions scoped to what that role should see and do.

**AI clinical copilot** — Google Gemini 1.5 Flash generates clinical summaries, nurse-to-nurse shift handoff notes, and medication alerts, with every prompt and response logged for audit review.

**JWT auth with silent refresh** — Short-lived (15 min) access tokens limit exposure if intercepted; a 7-day refresh token keeps sessions alive. An Axios interceptor transparently refreshes and retries failed requests on 401 without interrupting the user.

**Real-time clinical event stream** — A STOMP-over-WebSocket channel (`/ws`, SockJS fallback) pushes timeline events, vitals, task assignments, and notifications to connected clients instantly. Topics are role-scoped and authorization-checked per subscription (see [ADR 0001](docs/adr/0001-websocket-topic-design.md)); the frontend wires incoming events straight into the TanStack Query cache — no refetch storms. A live presence indicator shows who is viewing each patient workspace.

**Medication verification with video proof** — Nurses and caregivers can attach short video clips as proof of medication administration, stored via Cloudinary.

**Full audit trail** — Every AI call and every system action is logged for admin review — critical for a system handling clinical data.

**One-command local setup** — Flyway migrations seed the entire database (users, patients, clinical history) automatically on startup. No manual SQL, no seed scripts to remember to run.

---

## Tech Stack

### Backend
- **Java 21 + Spring Boot 3** — REST API with `context-path: /api`
- **Spring Security + JWT (HS512)** — stateless auth with separate access (15 min) and refresh (7 day) tokens; a token-type claim prevents refresh tokens being used as access tokens
- **PostgreSQL** (prod) / **H2** (dev, PostgreSQL-mode) — identical schema, zero config for local dev
- **Flyway** — versioned SQL migrations (V1 schema → V5 demo data)
- **Google Gemini 1.5 Flash** — AI copilot for clinical summaries, shift handoffs, medication alerts
- **Cloudinary** — video/file attachments (up to 10 MB) for medication verification

### Frontend
- **React 18 + Vite** — fast HMR in dev, optimized production bundle
- **React Router v7** — role-based `ProtectedRoute` guard on every route
- **TanStack Query v5** — server state, caching, background refetching
- **Axios** — request interceptor attaches the Bearer token; response interceptor handles silent JWT refresh on 401
- **Zustand** — lightweight client state
- **Tailwind CSS v3** — utility-first styling with a custom CSS-variable design system (light + dark themes)
- **Lucide React** — icon library

### Infrastructure
- **Docker + Docker Compose** — `docker-compose up` runs the entire stack locally
- **Vercel** — frontend deployment, zero-config SPA routing
- **GitHub Actions** — CI/CD pipeline

---

## Architecture Overview

```
Browser / Client
  React 18 + Vite  --  TanStack Query  --  Zustand
  React Router v7  --  Axios (interceptors)
  Tailwind CSS     --  Lucide React
         |
         | HTTPS + JWT Bearer
         v
Spring Boot 3 REST API (/api/*)
  AuthController     --  Spring Security  --  JwtTokenProvider
  ClinicalController --  Service Layer    --  Repository Layer
  AuditController    --  Gemini AI Client
         |
    _____|_____
   |           |
   v           v
PostgreSQL   External Services
(Flyway)     - Google Gemini 1.5 Flash (AI)
             - Cloudinary (file storage)
```

### JWT Auth Flow

```
1.  POST /api/auth/login  { email, password }
2.  Backend validates -> returns { token, refreshToken, user }
3.  Frontend stores tokens in localStorage
4.  Every API request attaches Authorization: Bearer {token}
5.  On 401 -> Axios interceptor calls POST /api/auth/refresh (once, deduplicated)
6.  If refresh succeeds -> retry original request transparently
7.  If refresh fails -> clear tokens, redirect to /login
```

### Role-Based Access

Once logged in, the `/` route checks your role and redirects you to the right place. The `ProtectedRoute` component enforces this at the route level.

```
DOCTOR     ->  /overview          (full clinical access)
NURSE      ->  /nurse-dashboard   (shift queue + assigned patients)
CAREGIVER  ->  /family            (read-only care visibility)
READ_ONLY  ->  /family            (same as caregiver, no write)
PATIENT    ->  /my-care           (personal care dashboard only)
ADMIN      ->  /audit             (system audit log)
```

---

## Database Schema

The heart of CareFlow is the **`timeline_events` table** — an append-only clinical log. Every action (diagnosis, medication, vital sign, handoff, task) writes to the timeline. Corrections reference the original event via `corrects_event_id` so nothing is ever deleted.

**15 tables total:**

| Table | Purpose |
|---|---|
| `users` | All accounts — Doctors, Nurses, Caregivers, Patients, Admins |
| `patients` | Patient records with assigned doctor/nurse |
| `patient_access` | Fine-grained caregiver/family access per patient |
| `timeline_events` | Append-only clinical event log (the source of truth) |
| `diagnoses` | Doctor-written diagnoses linked to a patient |
| `medications` | Prescriptions (ACTIVE / DISCONTINUED / COMPLETED) |
| `medication_administration` | Per-dose administration records with optional video |
| `vitals` | Blood pressure, sugar, temperature, oxygen readings |
| `shift_handoffs` | Nurse-to-nurse shift summaries with AI-generated notes |
| `tasks` | Doctor-assigned tasks for nurses with status tracking |
| `appointments` | Scheduled appointments between doctor and patient |
| `file_attachments` | Cloudinary-hosted files (video proof of medication) |
| `ai_prompt_history` | Full audit trail of all AI calls (prompt + response) |
| `notifications` | In-app alerts for critical vitals, missed meds, etc. |
| `audit_logs` | System-wide action log for ADMIN review |

Full entity-relationship diagram: see [`ERD.md`](./ERD.md)

---

## Flyway Migrations

All database setup happens automatically via Flyway on startup. Never run SQL manually.

| Migration | What it does |
|---|---|
| `V1__init_schema.sql` | Creates all 15 tables with constraints and indexes |
| `V2__seed_data.sql` | Seeds 5 clinical users + 4 patients + full synthetic clinical data |
| `V3__verification_additions.sql` | Adds video attachment support for medication administration |
| `V4__patient_supervision_fields.sql` | Adds remote supervision flags to patients table |
| `V5__patient_role_and_demo_data.sql` | Adds PATIENT + ADMIN + READ_ONLY roles, updates demo emails, adds more clinical data |

---

## Project Structure

```
CareFlow/
├── backend/
│   └── src/main/
│       ├── java/com/careflow/ai/
│       │   ├── controller/       AuthController, ClinicalController, AuditController
│       │   ├── dto/              LoginRequest, AuthResponse, DashboardDto, PatientDto
│       │   ├── entity/           User, Patient, Medication, Task, ShiftHandoff
│       │   ├── repository/       Spring Data JPA repositories
│       │   ├── security/         JwtTokenProvider, SecurityConfig, JwtAuthFilter
│       │   ├── websocket/        STOMP config, JWT handshake, topic authorizer, presence
│       │   └── service/          AuthService, PatientService, AiService
│       └── resources/
│           ├── application.yml
│           └── db/migration/     V1 to V5 Flyway SQL files
│
├── frontend/
│   └── src/
│       ├── App.jsx               Root router
│       ├── context/              AuthContext (auth) + SocketContext (real-time)
│       ├── services/
│       │   ├── api.js            Axios instance + JWT interceptors
│       │   ├── auth.js           Login / register / refresh / logout
│       │   ├── queryClient.js    TanStack Query client + cache-patch helpers
│       │   └── websocket/        Socket client, SockJS shim, topic map, realtime handlers
│       ├── pages/
│       │   ├── Login.jsx         Login + one-click Demo Mode
│       │   ├── Overview.jsx      Doctor home
│       │   ├── PatientWorkspace  Full per-patient clinical workspace
│       │   ├── MedicationsPage
│       │   ├── TasksPage
│       │   ├── ShiftHandoffsPage
│       │   └── dashboards/
│       │       ├── NurseDashboard
│       │       ├── FamilyDashboard
│       │       ├── PatientDashboard
│       │       └── AuditPanel
│       └── components/
│           ├── auth/ProtectedRoute
│           ├── layout/AppShell
│           └── clinical/
│
├── docker-compose.yml
├── ERD.md
└── README.md
```

---

## Local Development

### Prerequisites
- Java 21+, Maven
- Node.js 18+, npm
- Docker (optional)

### 1. Backend
```bash
cd backend
cp ../.env.example .env
# Edit .env -> set JWT_SECRET (any 64+ char string for dev)

# Dev profile uses H2 in-memory DB automatically
./mvnw spring-boot:run
# API available at http://localhost:8080/api
```

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:5173
```

### 3. Docker (everything at once)
```bash
# From project root
docker-compose up --build
```

### Environment Variables
Copy `.env.example` to `.env`:
```
# Required
JWT_SECRET=your_64_char_secret_here

# Optional (has dev defaults)
JWT_EXPIRATION_MS=900000            # 15 min access token
JWT_REFRESH_EXPIRATION_MS=604800000 # 7 day refresh token

# Required for AI features
GEMINI_API_KEY=your_gemini_key

# Required for file uploads
CLOUDINARY_URL=cloudinary://api_key:api_secret@cloud_name
```

---

## Vercel Deployment

The `vercel.json` at `frontend/vercel.json` handles SPA routing — all paths rewrite to `index.html`.

```bash
cd frontend
npm run build   # builds to frontend/dist/
```

Set `VITE_API_BASE_URL` in Vercel environment variables to point to your deployed Spring Boot backend URL (including the `/api` context path, e.g., `https://careflow-backend.example.com/api`). Do not treat the Vercel SPA rewrite as a backend proxy.

---

## Demo Accounts

All demo accounts use password **`password123`**. Created by Flyway V2 and V5 migrations.

| Email | Role | Landing page |
|---|---|---|
| doctor1@careflow.ai | DOCTOR | /overview |
| doctor2@careflow.ai | DOCTOR | /overview |
| nurse1@careflow.ai | NURSE | /nurse-dashboard |
| nurse2@careflow.ai | NURSE | /nurse-dashboard |
| caregiver@careflow.ai | CAREGIVER | /family |
| patient@careflow.ai | PATIENT | /my-care |
| admin@careflow.ai | ADMIN | /audit |

---

## Key Design Decisions

**Why append-only timeline?** Clinical records are legally required to be immutable. Instead of editing records, CareFlow adds correction events that reference the original. Full history is always preserved.

**Why JWT with a refresh token?** Short-lived access tokens (15 min) limit the blast radius if a token is intercepted. The 7-day refresh token keeps sessions alive without requiring re-login. The backend validates a `type` claim on every token to prevent token misuse.

**Why Flyway instead of a Java DataSeeder?** SQL migrations are version-controlled and reproducible. Every environment gets exactly the same database state in exactly the same order — no "works on my machine" data issues.

**Why TanStack Query?** Clinical data changes frequently. Background refetching, stale-while-revalidate, and built-in loading/error states remove the need for custom data-fetching boilerplate across every component.

**Why STOMP over WebSocket with per-topic authorization?** Real-time clinical data is a safety feature — a nurse needs the abnormal vital the moment it is recorded. Sockets are scoped by role/patient (never a global firehose): the JWT is validated on the handshake, and every SUBSCRIBE is re-checked server-side against the caller's role and patient-access grants, then rewritten to a private destination. See [ADR 0001](docs/adr/0001-websocket-topic-design.md).

**Why push into the TanStack Query cache instead of refetching?** A socket event carries the new record; patching `queryClient.setQueryData` updates every mounted component instantly with zero extra HTTP load. Sockets that only trigger refetches would just duplicate the REST path.

---

## What's Next

- Event-driven backbone (queue + transactional outbox) for AI summaries and notification fan-out
- Mobile-responsive caregiver app
- Configurable AI alert thresholds per patient
- Multi-hospital/tenant support

---

## License

MIT — built by [Ashmitha148](https://github.com/Ashmitha148)
