# OpenSourceContributor — Product Requirements Document

**Status:** Working product specification
**Version:** 0.1
**Product name:** OpenSourceContributor

## 1. Product vision

OpenSourceContributor helps developers discover real open-source contribution opportunities on GitHub that fit their technical skills, interests, and experience.

**Approved tagline:**
**FIND WHERE YOU FIT. CONTRIBUTE.**

The product is technology-agnostic. It must not be presented as a Java-only platform or as a platform exclusively for beginners.

## 2. Problem

Developers can struggle to:
- Find relevant issues across many repositories.
- Identify projects that use technologies they know or want to learn.
- Filter opportunities by experience, issue type, repository activity, and other criteria.
- Understand why a particular issue is a good match.
- Keep track of issues they intend to work on.

## 3. Target users

- Beginners looking for approachable first contributions.
- Intermediate and experienced developers looking for relevant issues.
- Developers exploring a new language, framework, domain, or project type.
- Contributors who want to save and organise potential contributions.

## 4. Core user journey

1. A visitor opens the public website.
2. The visitor enters a natural-language search prompt or uses structured filters.
3. The AI layer interprets the prompt and returns structured search criteria.
4. The application searches GitHub using those criteria.
5. The application retrieves and verifies issue and repository information from GitHub.
6. Results are ranked and accompanied by an explanation of relevance.
7. The visitor opens the original GitHub issue.
8. A signed-in user can save issues and manage their contribution tracking.

Public issue discovery should be available without requiring sign-in. GitHub sign-in unlocks user-specific features.

## 5. Functional requirements

### 5.1 Natural-language discovery
- Accept free-form prompts describing skills, technologies, project domains, experience, and desired contribution types.
- Extract structured filters such as languages, frameworks, keywords, difficulty preference, star range, issue state, and exclusions.
- Do not invent requirements that the user did not state.
- Represent unspecified filters as absent or null, not guessed values.
- Allow users to inspect and edit extracted filters before or after searching.

### 5.2 GitHub issue discovery
- Search real GitHub issues through the GitHub API.
- Retrieve repository metadata where required.
- Support filters such as language, framework/keywords, labels, issue state, repository stars, and update recency where supported by available data.
- Handle pagination, API errors, rate limits, and partial results.
- Link every result to its original GitHub issue.
- Display when data was last fetched or verified.

### 5.3 Ranking and explanations
- Rank results using transparent, configurable criteria.
- Possible criteria include technology relevance, user preference match, issue-description relevance, repository activity, and freshness.
- Initial weights are experimental and must be evaluated before being treated as reliable.
- Explain why an issue matched.
- Clearly distinguish verified GitHub metadata from AI-generated relevance or difficulty estimates.

### 5.4 Issue details
Display available verified information:
- Issue title and number.
- Repository name and link.
- Issue state and labels.
- Issue body or a safe summary.
- Created and updated timestamps.
- Repository description and available metadata.
- Original GitHub URL.
- Match explanation, clearly marked as an estimate where appropriate.

### 5.5 Authentication
- GitHub OAuth is the only sign-in method.
- Do not add Google, email/password, or other social login.
- Use minimal OAuth permissions.
- Do not request repository write permissions unless a future, explicitly approved feature requires them.
- Validate OAuth state and protect the callback flow.
- Keep OAuth secrets on the server.
- Public discovery must not require authentication.

### 5.6 User features
- View GitHub-connected profile information.
- Save and remove issues.
- Set technical preferences.
- Track contributions with user-managed statuses:
  - Planning
  - In Progress
  - Pull Request Opened
  - Completed
- Store a PR URL when supplied or supported by a future integration.
- Do not claim automatic contribution detection unless implemented.

### 5.7 Contact and legal pages
- Contact form with validation, loading, success, and error states.
- Privacy Policy and Terms pages with legal copy reviewed before launch.
- Cookie preferences only for cookies or analytics that are actually implemented.
- Thank-you page shown only after successful backend confirmation.

## 6. Non-functional requirements

- Responsive desktop, tablet, and mobile UI.
- Accessible semantic controls, keyboard navigation, visible focus, readable contrast, and reduced-motion support.
- Server-side validation for all backend inputs.
- Secure session/token handling.
- Rate limiting and protection against abuse.
- Observability through structured logs and error monitoring.
- Caching for repeated GitHub lookups where appropriate.
- Database indexes based on actual query patterns.
- Automated tests for critical flows.
- No fabricated metrics, issue counts, testimonials, or maintainer responsiveness claims.

## 7. Initial page inventory

1. Home `/`
2. Explore Issues `/explore`
3. Issue Details `/issues/:id`
4. How It Works `/how-it-works`
5. About `/about`
6. Contact `/contact`
7. GitHub Login `/login`
8. OAuth Callback `/auth/callback`
9. Dashboard `/dashboard`
10. Saved Issues `/saved-issues`
11. My Contributions `/my-contributions`
12. Profile and Settings `/settings`
13. Privacy Policy `/privacy`
14. Terms and Conditions `/terms`
15. Cookie Preferences `/cookie-preferences`
16. Thank You `/thank-you`
17. Custom 404

## 8. MVP boundaries

Prioritise:
- Public issue search.
- Natural-language filter extraction.
- GitHub issue retrieval and verification.
- Useful ranking and match explanations.
- GitHub OAuth.
- Saving issues.
- Basic user-managed contribution tracking.

Do not initially build:
- Automatic pull-request creation.
- Repository write operations.
- A code-hosting platform.
- A social network or discussion forum.
- Claims of guaranteed beginner suitability or maintainer acceptance.
- Python ML infrastructure without a demonstrated need.

## 9. Success criteria

Evaluate the search system with a representative set of real prompts and manually labelled results. Track:
- Filter extraction accuracy.
- Precision@10 and other ranking metrics.
- Freshness/verification correctness.
- Explanation accuracy.
- Search latency and GitHub API failure rate.
- Successful save and contribution-tracking flows.

Do not publish performance claims until measured on a documented evaluation set.
