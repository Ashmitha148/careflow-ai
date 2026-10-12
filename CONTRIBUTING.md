# Contributing to CareFlow AI

Thank you for your interest in contributing to CareFlow AI! We welcome bug fixes, documentation improvements, architectural proposals, and performance optimizations.

---

## 1. Development Standards

- **Backend**: Java 21 LTS with Spring Boot 3.4+. Follow clean architecture practices with separation across controllers, services, repositories, and domain models.
- **Frontend**: React 18, Vite, and Tailwind CSS. Ensure all components provide loading, empty, and error boundaries.
- **Testing**: Write unit tests for new business logic (Vitest for frontend, JUnit 5 / MockMvc for backend). Verify tests pass before opening a PR.
- **Database**: Never alter existing Flyway migrations (`V1` to `V12`). Always create a new sequential migration script (`V13__...sql`).

---

## 2. Commit Message Guidelines

CareFlow AI adheres to the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` Adds a new user-facing capability or API feature.
- `fix:` Patches a bug or addresses a defect.
- `docs:` Documentation additions or updates.
- `test:` Adds or modifies unit, integration, or end-to-end tests.
- `refactor:` Code improvements without behavioral changes.
- `chore:` Dependency bumps, CI workflow changes, or build maintenance.

---

## 3. Pull Request Process

1. Fork the repository and create a feature branch (`git checkout -b feat/my-feature`).
2. Run automated verification:
   - Backend: `./mvnw test`
   - Frontend: `npm run lint` && `npm run test` && `npm run build`
3. Commit your changes with descriptive conventional commit messages.
4. Open a Pull Request detailing the problem solved, testing performed, and screenshots for visual changes.
