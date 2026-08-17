# CareFlow AI

> An AI-assisted clinical coordination platform for hospitals and care teams — connecting Doctors, Nurses, Caregivers, and Patients in one unified workspace.

**Live Demo → https://careflow-ai-beta.vercel.app/**

---

## What is CareFlow?

CareFlow is a full-stack healthcare coordination platform that brings patient information, clinical workflows, and care-team communication into one workspace.

It provides role-specific workflows, an AI copilot for clinical summaries and shift handoffs, and an append-only patient timeline for an auditable history of care.

---

## Key Features

- **Role-based workflows** for Doctors, Nurses, Caregivers, Patients, and Admins
- **Patient timeline** with append-only clinical events
- **Vitals & medication management**
- **Diagnoses, appointments, and tasks**
- **Shift handoffs** and AI-generated summaries
- **AI Copilot** powered by Google Gemini
- **Medication verification** with video/file uploads
- **Audit trail** for administrative visibility
- **JWT authentication** with access + refresh tokens

---

## One-Click Demo

Click **Enter Demo** on the login page and choose a role.

| Role          | Main Workflow                                            |
| ------------- | -------------------------------------------------------- |
| **Doctor**    | Patients, diagnoses, medications, AI summaries, tasks    |
| **Nurse**     | Shift queue, vitals, medication administration, handoffs |
| **Caregiver** | Care plan, timeline, medications, appointments           |
| **Patient**   | Personal care dashboard and appointments                 |
| **Admin**     | Audit trail                                              |

> All demo data is synthetic. No real patient information is used.

---

## Tech Stack

### Backend

- **Java 17**
- **Spring Boot 3.3**
- **Spring Security + JWT (HS512)**
- **Spring Data JPA / Hibernate**
- **PostgreSQL / H2**
- **Flyway**
- **Google Gemini 1.5 Flash**
- **Cloudinary**

### Frontend

- **React 18 + Vite**
- **React Router v7**
- **TanStack Query v5**
- **Axios**
- **Zustand**
- **Tailwind CSS**
- **Lucide React**

### Infrastructure

- **Docker + Docker Compose**
- **Vercel**
- **Render**

---

## Architecture

```text
React + Vite
     |
     | HTTPS + JWT
     v
Spring Boot REST API
     |
     ├── Spring Security
     ├── Controllers
     ├── Services
     ├── Repositories
     └── Gemini AI
     |
     v
PostgreSQL
  (Flyway)
     |
     └── Cloudinary
```

---

## Authentication

```text
Login
  ↓
JWT access + refresh tokens
  ↓
Axios attaches Bearer token
  ↓
Protected API
  ↓
401 → refresh token → retry request
```

Authentication is stateless, with short-lived access tokens and refresh tokens.

---

## Role-Based Access

```text
DOCTOR     → /overview
NURSE      → /nurse-dashboard
CAREGIVER  → /family
PATIENT    → /my-care
ADMIN      → /audit
```

Access is enforced through Spring Security and protected frontend routes.

---

## Database

CareFlow uses a **15-table relational schema** with PostgreSQL in production and H2 for development.

The `timeline_events` table acts as an **append-only clinical timeline**. Corrections reference previous events instead of silently overwriting history.

Database schema and demo data are managed through **Flyway migrations**.

See [`ERD.md`](./ERD.md) for the database design.

---

## Local Development

### Prerequisites

- Java 17+
- Maven
- Node.js 18+
- npm
- Docker (optional)

### Backend

```bash
cd backend
cp ../.env.example .env
./mvnw spring-boot:run
```

API:

```text
http://localhost:8080/api
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:5173
```

### Environment Variables

```env
JWT_SECRET=your_long_random_secret
GEMINI_API_KEY=your_gemini_key
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## Demo Accounts

Password for all demo accounts:

```text
password123
```

| Email                   | Role      |
| ----------------------- | --------- |
| `doctor1@careflow.ai`   | DOCTOR    |
| `doctor2@careflow.ai`   | DOCTOR    |
| `nurse1@careflow.ai`    | NURSE     |
| `nurse2@careflow.ai`    | NURSE     |
| `caregiver@careflow.ai` | CAREGIVER |
| `patient@careflow.ai`   | PATIENT   |
| `admin@careflow.ai`     | ADMIN     |

---

## Key Design Decisions

- **Append-only timeline** — preserves an auditable clinical history.
- **JWT + refresh tokens** — provides stateless authentication.
- **Flyway migrations** — keeps database changes version-controlled.
- **TanStack Query** — handles server-state caching and refetching.

---

## License

MIT
