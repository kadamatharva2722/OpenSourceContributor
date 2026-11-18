# OpenSourceContributor — Architecture

**Status:** Proposed target architecture
**Version:** 0.1

## 1. Technology decisions

### Frontend
- React
- Vite
- JavaScript (not TypeScript)
- Tailwind CSS for layout, spacing, responsive utilities, and common styling
- Custom CSS for design tokens, typography, and distinctive visual treatments
- React Router
- Lucide React
- Motion for React where animation is useful

### Backend
- Java
- Spring Boot
- Spring AI for model integration
- Groq as the initial LLM provider, subject to current model/API availability
- GitHub REST API as the source of truth for issue and repository data

### Data and infrastructure
- SQL is the query language; PostgreSQL is the recommended relational database management system.
- PostgreSQL is recommended but should be treated as a decision to confirm before implementation.
- Redis may be used for caching, distributed rate limits, or short-lived data when justified.
- Kafka may be used for asynchronous workflows when there is a real event-processing requirement.
- Docker for local development and deployment packaging.
- GitHub Actions or equivalent CI/CD.

## 2. Architecture style

The intended target is a microservice architecture with clearly bounded business capabilities.

Do not create many independently deployed services merely to claim microservices. Start with clear module boundaries and extract/deploy services incrementally when the interfaces, ownership, scaling, or reliability requirements justify it.

Keep the frontend and backend in separate root directories.

## 3. Proposed repository layout

```text
OpenSourceContributor/
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── features/
│   │   ├── layouts/
│   │   ├── pages/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── utils/
│   │   └── styles/
│   ├── package.json
│   └── vite.config.js
├── backend/
│   ├── api-gateway/
│   └── services/
│       ├── auth-service/
│       ├── user-service/
│       ├── discovery-service/
│       ├── ai-search-service/
│       ├── issue-service/
│       └── contribution-service/
├── infrastructure/
│   ├── docker/
│   └── kafka/
├── docs/
│   ├── HLD/
│   ├── LLD/
│   ├── database-design/
│   └── api-contracts/
├── .gitignore
└── README.md
```

This is a target layout. Create only the directories and services needed for the current phase. Do not generate empty microservices prematurely.

## 4. Logical service responsibilities

### API Gateway
- Single public backend entry point.
- Route requests to appropriate services.
- Apply cross-cutting policies such as rate limiting and request correlation.
- Do not put core business logic in the gateway.

### Auth Service
- GitHub OAuth authorization and callback.
- State validation and secure session/token handling.
- Authentication-related user identity mapping.
- No password or Google authentication.

### User Service
- User profile and preferences.
- Preferred languages, frameworks, experience, and contribution interests.

### AI Search Service
- Use Spring AI to call Groq.
- Convert natural-language prompts into validated structured search filters.
- Generate concise match explanations where appropriate.
- Never treat LLM output as trusted GitHub facts.
- Validate structured output before passing it to other services.

### Issue Service
- Integrate with GitHub APIs.
- Search issues and retrieve repository metadata.
- Normalize external responses.
- Handle pagination, caching, rate limits, and GitHub errors.
- Preserve source URLs and retrieval timestamps.

### Discovery Service
- Orchestrate search criteria, issue retrieval, filtering, and ranking.
- Return results with transparent match reasons.
- Keep ranking logic testable and configurable.

### Contribution Service
- Save and remove issues.
- Track user-managed contribution statuses.
- Store optional pull-request references.
- Enforce ownership/authorization.

The boundaries above are logical proposals and can be adjusted after HLD/LLD review.

## 5. Main search flow

1. React sends a search prompt to the backend.
2. Gateway routes the request to Discovery Service.
3. Discovery Service requests structured filters from AI Search Service.
4. AI Search Service calls Groq through Spring AI.
5. AI output is parsed, schema-validated, and normalized.
6. Discovery Service calls Issue Service with validated criteria.
7. Issue Service queries GitHub and retrieves needed repository metadata.
8. Discovery Service filters and ranks actual GitHub results.
9. The frontend receives verified issue data plus clearly labelled estimates and explanations.

If AI is unavailable, consider allowing structured/manual filters as a fallback.

## 6. External integrations

### GitHub
- Source of truth for issues, repositories, labels, states, and available metadata.
- Respect API rate limits and terms.
- Do not expose tokens or secrets to the browser.
- Do not request write scopes for read-only discovery.

### Groq / Spring AI
- Used for natural-language interpretation and optional explanations.
- Keep API keys server-side.
- Validate all model output.
- Handle timeouts, malformed output, provider errors, and rate limits.
- Model names and provider capabilities must be verified at implementation time.

## 7. Data ownership and storage

Proposed relational data areas:
- Users and linked GitHub identity.
- User preferences.
- Saved issues.
- Contribution tracking records.
- Search history only if product requirements and privacy policy justify it.
- Cached issue/repository metadata only with an explicit TTL and refresh strategy.

Avoid storing GitHub data unnecessarily. Record external identifiers and source URLs so records can be revalidated.

## 8. Communication

- Synchronous HTTP APIs for user-facing request/response flows.
- Kafka only for workflows that benefit from asynchronous processing, such as scheduled refreshes or background processing.
- Define event schemas, retries, dead-letter handling, idempotency, and event ownership before introducing Kafka.
- Avoid distributed transactions where possible.

## 9. Reliability and security

- Timeouts for GitHub and Groq calls.
- Bounded retries with backoff for transient failures.
- Circuit breakers where appropriate.
- Idempotency for retryable write operations.
- Rate limiting at gateway and/or service level.
- Input validation and output encoding.
- Secure cookies/session handling and CSRF protection where applicable.
- Secrets in environment/secret management, never committed.
- Structured logs, correlation IDs, metrics, and error monitoring.
- Database backups and recovery procedures.

## 10. Architecture decision still to confirm

PostgreSQL is recommended for relational storage and advanced querying, but the user has asked to learn SQL and is not yet familiar with PostgreSQL. Teach SQL concepts first and explain PostgreSQL-specific features as they are introduced. Confirm the database choice before committing to implementation.
