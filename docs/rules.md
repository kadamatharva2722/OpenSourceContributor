# OpenSourceContributor — Project Rules

These rules apply to Antigravity and all contributors working in this repository.

## 1. Product rules

1. The product is technology-agnostic. Never make it Java-only.
2. Support discovery across programming languages, frameworks, domains, and contribution types.
3. GitHub is the source of truth for issue and repository facts.
4. Never fabricate issues, repository metadata, star counts, labels, timestamps, or user activity.
5. Clearly label AI-generated estimates and explanations.
6. Never promise issue acceptance or maintainer responsiveness without evidence.
7. Public issue browsing should not require sign-in.
8. GitHub OAuth is the only authentication method.

## 2. Approved design rules

1. Follow the approved Run Out Records-inspired design direction.
2. Main background: `#0D0D0D`.
3. Main text: `#F1ECE2`.
4. Surface: `#1A1A1A`.
5. Muted text: `#A7A29A`.
6. Accent colours:
   - Orange `#F06522`
   - Yellow `#F4C13D`
   - Blue `#5B86F2`
   - Pink `#F29BB8`
7. Fonts:
   - Anton for display headings.
   - Archivo for body and UI.
   - JetBrains Mono for technical metadata.
8. Sharp corners; do not introduce rounded SaaS cards or pill buttons.
9. Do not use lime as the primary accent.
10. Avoid generic AI illustrations, purple gradients, glow, glassmorphism, fake testimonials, and invented statistics.
11. The approved hero headline is: **FIND WHERE YOU FIT. CONTRIBUTE.**
12. Keep the search experience technology-agnostic and show diverse example prompts.

## 3. Frontend rules

1. Use React + Vite + JavaScript, not TypeScript.
2. Use React Router for page routing.
3. Tailwind CSS is allowed for layout, spacing, responsive utilities, and common styling.
4. Use custom CSS where needed for the distinctive visual system.
5. Keep pages, features, components, layouts, hooks, services, and utilities separated.
6. Keep API logic outside presentational components.
7. Do not hardcode secrets.
8. Do not make buttons appear functional if they have no defined behaviour.
9. Provide loading, empty, error, and success states where relevant.
10. Ensure responsive layouts and keyboard accessibility.
11. Do not overwrite existing work without inspecting it first.

## 4. Backend and architecture rules

1. Backend language: Java.
2. Backend framework: Spring Boot.
3. AI integration: Spring AI with Groq as the initial provider.
4. Use clear service boundaries and design for a microservice target architecture.
5. Do not create independently deployed services without a reason; develop and extract incrementally.
6. Keep frontend and backend in separate root directories.
7. Define HLD, LLD, database design, and API contracts before implementing substantial backend features.
8. Apply SOLID principles and design patterns where they solve real problems.
9. Use SQL for relational data access. PostgreSQL is recommended but must be confirmed before final adoption.
10. Use Redis and Kafka only when their requirements and operational costs are justified.
11. Do not put business logic in the API Gateway.

## 5. Security rules

1. GitHub OAuth only; no password or Google login.
2. Use minimal OAuth scopes.
3. Do not request GitHub repository write permissions for read-only discovery.
4. Validate OAuth state and protect callback handling.
5. Keep client secrets and provider API keys server-side.
6. Validate all inputs on the server.
7. Enforce authorization on user-owned records.
8. Protect against injection, XSS, CSRF, and abuse as applicable.
9. Use HTTPS in deployed environments.
10. Do not log secrets or sensitive tokens.

## 6. AI and data rules

1. Treat LLM output as untrusted input.
2. Validate structured AI output against a schema.
3. Use null/absent values for unspecified filters; do not guess.
4. AI interprets prompts; GitHub provides issue facts.
5. Keep ranking criteria transparent and measurable.
6. Handle provider timeouts, rate limits, and malformed responses.
7. Provide a manual/structured-filter fallback where practical.

## 7. Antigravity workflow rules

1. Inspect the repository before making changes.
2. Explain risky file moves or destructive changes before performing them.
3. Work in small, verifiable phases.
4. Run the relevant build, tests, and application after changes.
5. Report files changed, commands run, and remaining issues.
6. Do not repeatedly retry a failing install without diagnosing it.
7. Do not claim Stitch MCP is connected unless its tools are actually available and verified.
8. Use the approved Stitch designs as the UI source of truth.
9. Do not start backend implementation during a frontend-only phase.
10. Keep documentation updated when architecture or product decisions change.
