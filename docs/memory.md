# OpenSourceContributor — Project Memory for Antigravity

This file records durable project decisions and context. Read it before making substantial changes. Update it when a decision is explicitly changed.

## 1. Product identity
- Name: OpenSourceContributor.
- Approved tagline: **FIND WHERE YOU FIT. CONTRIBUTE.**
- Product: AI-assisted discovery of real GitHub open-source issues.
- The product is for developers across experience levels and technical backgrounds.
- It is not Java-only and not beginner-only.

## 2. Authentication
- GitHub OAuth only.
- No Google login.
- No email/password login.
- Keep permissions minimal.
- Do not request repository write permissions for read-only issue discovery.
- Public browsing should not require sign-in.

## 3. Approved technology direction
- Frontend: React + Vite + JavaScript (not TypeScript).
- Styling: Tailwind CSS plus custom CSS where appropriate.
- Routing: React Router.
- Icons: Lucide React.
- Motion: Motion for React when useful.
- Backend: Java + Spring Boot.
- AI integration: Spring AI with Groq as the initial provider.
- GitHub API supplies actual issue/repository facts.
- SQL is the query language. PostgreSQL is recommended as the relational DBMS, but confirm the final database choice before committing implementation.
- Redis and Kafka are planned options, not mandatory from day one.

## 4. Architecture direction
- Target architecture: microservices with clear business boundaries.
- Develop incrementally; do not create many empty services just to claim microservices.
- Keep frontend and backend in separate root directories.
- Proposed service capabilities: API Gateway, Auth, User, AI Search, Issue, Discovery, Contribution.
- HLD, LLD, database design, and API contracts should precede substantial backend implementation.
- Use SOLID and design patterns where they solve real problems.

## 5. Search integrity
- AI interprets user prompts and creates structured filters.
- GitHub is the source of truth for issues and repository data.
- Validate LLM output.
- Never invent issues or metadata.
- Distinguish verified facts from AI estimates.
- Do not promise maintainer responsiveness or PR acceptance.
- Ranking weights must be tested, not presented as established truth.

## 6. Design direction
- The final approved style is inspired by the Run Out Records website prompt.
- This supersedes the earlier Kestrel and Vanguard design explorations.
- Dark ground `#0D0D0D`, off-white `#F1ECE2`, surface `#1A1A1A`, muted `#A7A29A`.
- Accent colours: orange `#F06522`, mustard `#F4C13D`, cobalt `#5B86F2`, pink `#F29BB8`.
- Fonts: Anton, Archivo, JetBrains Mono.
- Sharp corners, dense editorial layout, oversized typography, flat colour blocks.
- No lime-green identity, glassmorphism, purple gradients, glowing UI, fake stats, or generic AI illustrations.
- Approved hero: **FIND WHERE YOU FIT. CONTRIBUTE.**
- Approved supporting copy: "Discover open-source projects and real GitHub issues that match your skills, interests and experience. Explore opportunities across technologies and contribute to projects that matter."

## 7. Pages
The intended website has 17 routes:
Home, Explore Issues, Issue Details, How It Works, About, Contact, GitHub Login, OAuth Callback, Dashboard, Saved Issues, My Contributions, Settings, Privacy Policy, Terms, Cookie Preferences, Thank You, and 404.

## 8. Tooling and workflow
- Google Stitch is used for UI design.
- Antigravity is used for implementation.
- Stitch MCP setup was reported by the user as successfully configured; verify tool availability before claiming it works.
- Use Stitch designs as the visual source of truth.
- Work in small phases and verify builds/tests.
- Never expose API keys in chat or commit them.
- The user prefers concise, direct explanations and step-by-step guidance.
- The user wants to understand LLD and system design decisions rather than blindly accepting generated code.

## 9. Known implementation caution
- Antigravity previously appeared to wait for dependency installation for an extended period. Diagnose terminal output before retrying installs.
- The project initially showed only a `package.json` in the root. Inspect current files before restructuring.
- Preserve existing work when creating `frontend/` and `backend/`.
- Do not assume the frontend setup has completed successfully until verified.

## 10. Current next action
Verify the existing repository and dependency installation. Then establish separate `frontend/` and `backend/` directories safely, finish the frontend foundation, and implement the Stitch UI. Do not start backend services until HLD/LLD and API contracts are reviewed.
