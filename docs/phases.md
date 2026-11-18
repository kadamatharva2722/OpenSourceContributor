# OpenSourceContributor — Development Phases

**Approach:** Incremental delivery with design, architecture, implementation, and verification at each stage.

## Phase 0 — Project preparation
- Inspect the existing repository.
- Preserve existing files and work.
- Establish root-level `frontend/`, `backend/`, `infrastructure/`, and `docs/` boundaries.
- Verify Node.js, npm, Java, and Maven availability as needed.
- Confirm Stitch MCP availability by asking Antigravity to list accessible Stitch tools.
- Do not expose API keys in chat or commit them.

**Exit criteria:** Repository structure is clear; existing work is preserved; toolchain is verified.

## Phase 1 — Frontend foundation
- Initialize or verify React + Vite + JavaScript.
- Configure Tailwind CSS.
- Install React Router, Lucide React, and Motion only as needed.
- Create the feature-oriented frontend structure.
- Configure fonts and global design tokens.
- Build shared navigation, footer, buttons, inputs, labels, and layouts.
- Configure all required routes.

**Exit criteria:** App runs; all routes resolve; shared design system is usable.

## Phase 2 — UI implementation from Stitch
Recommended order:
1. Home
2. Explore Issues
3. Issue Details
4. Login
5. How It Works
6. Dashboard
7. Saved Issues
8. My Contributions
9. Settings
10. About
11. Contact
12. OAuth Callback
13. Privacy
14. Terms
15. Cookie Preferences
16. Thank You
17. 404

- Match the approved designs closely.
- Use clearly identified mock data where backend data is not yet available.
- Do not present mock data as live GitHub data.
- Implement desktop, tablet, and mobile states.

**Exit criteria:** All 17 routes have designed pages and navigation works.

## Phase 3 — Frontend quality
- Responsive testing.
- Accessibility and keyboard checks.
- Loading, empty, error, and success states.
- Form validation.
- Link and button verification.
- Browser console cleanup.
- Basic frontend tests.

**Exit criteria:** UI is stable and ready for API integration.

## Phase 4 — HLD and LLD
Before substantial backend coding:
- Confirm service boundaries.
- Draw context/container/component diagrams.
- Define user journeys and sequence diagrams.
- Define API contracts.
- Design database entities and relationships.
- Identify sync vs async communication.
- Define failure handling, retries, idempotency, and security.
- Document key design patterns and trade-offs.

**Exit criteria:** Reviewed architecture and implementation plan.

## Phase 5 — Backend foundation
- Initialize Java/Spring Boot backend structure.
- Set up build, configuration profiles, logging, validation, and error handling.
- Establish API conventions.
- Set up database connection after confirming the database choice.
- Add migrations and tests.
- Keep secrets outside source control.

**Exit criteria:** Backend starts reliably and exposes tested foundation endpoints.

## Phase 6 — Authentication
- Implement GitHub OAuth only.
- Validate state and callback.
- Establish secure session/token strategy.
- Map GitHub identity to application user.
- Test success, denial, cancellation, and failure paths.
- Keep public browsing available without login.

**Exit criteria:** Secure GitHub sign-in and sign-out work.

## Phase 7 — AI search
- Integrate Spring AI with Groq.
- Define strict structured filter schema.
- Validate model responses.
- Handle missing/ambiguous user requirements without guessing.
- Add timeout, retry, and provider error handling.
- Test representative prompts across multiple languages and domains.

**Exit criteria:** Prompts reliably produce validated filters.

## Phase 8 — GitHub issue discovery
- Integrate GitHub search and repository endpoints.
- Handle pagination and rate limits.
- Retrieve and normalise issue/repository metadata.
- Add caching with TTL where useful.
- Verify source URLs and retrieval timestamps.
- Add ranking and match explanations.

**Exit criteria:** Search returns real GitHub issues with transparent matching.

## Phase 9 — User features
- Save and remove issues.
- Store user preferences.
- Track contributions and user-managed statuses.
- Enforce ownership and authorization.
- Add dashboard data.

**Exit criteria:** Authenticated user workflows work end-to-end.

## Phase 10 — Distributed infrastructure
Introduce only when justified:
- API Gateway.
- Separate deployable services.
- Kafka for appropriate asynchronous workflows.
- Redis for distributed cache/rate limiting.
- Docker Compose for local multi-service development.

Define event contracts, idempotency, retries, dead-letter handling, and observability.

**Exit criteria:** Service boundaries and distributed workflows are tested and documented.

## Phase 11 — Production readiness
- HTTPS and security headers.
- Rate limiting and abuse controls.
- Structured logging and monitoring.
- Database indexes based on query plans.
- Backups and recovery.
- Load testing.
- CI/CD and deployment.
- Privacy, terms, and cookie review.
- Core Web Vitals and broken-link checks.

**Exit criteria:** Operational, security, performance, and recovery checks are documented and tested.

## Working rule

Do not skip ahead because a tool can generate code quickly. Each phase must have a working result and explicit verification before the next major phase begins.
