# GitHub OAuth 2.0 Authentication & Session Management

## 1. Overview
The OpenSourceContributor backend uses Spring Security's OAuth 2.0 authorization code flow with GitHub to authenticate users. Sessions are managed on the server with secure, `HttpOnly` cookies (`JSESSIONID`) protected by synchronized double-submit cookie CSRF tokens (`XSRF-TOKEN`).

---

## 2. Environment Variables

| Variable | Required | Default (Dev) | Description |
|---|---|---|---|
| `GITHUB_CLIENT_ID` | Yes (for OAuth) | *None* | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | Yes (for OAuth) | *None* | GitHub OAuth App Client Secret |
| `FRONTEND_URL` | No | `http://localhost:5173` | React frontend URL for post-login redirects |
| `SESSION_COOKIE_SECURE`| No | `false` | Set to `true` in production (requires HTTPS) |
| `SESSION_COOKIE_SAME_SITE`| No | `lax` | Cookie SameSite policy (`lax`, `strict`, or `none` for cross-site) |

> **Graceful Fallback**: If `GITHUB_CLIENT_ID` or `GITHUB_CLIENT_SECRET` are not set, the application initializes an `EmptyClientRegistrationRepository`. The application boots cleanly for local development, API documentation inspection, and health monitoring without crashing.

---

## 3. GitHub OAuth App Configuration

To configure a GitHub OAuth Application on [GitHub Developer Settings](https://github.com/settings/developers):

1. **Application Name**: `OpenSourceContributor`
2. **Homepage URL**:
   - Development: `http://localhost:5173`
   - Production: `https://<your-frontend-domain>`
3. **Authorization Callback URL**:
   - Development: `http://localhost:8080/login/oauth2/code/github`
   - Production: `https://<your-backend-domain>/login/oauth2/code/github`
4. **Requested Scopes**:
   - `read:user` (Minimal scope: read-only access to user profile and identity)

---

## 4. Endpoints

| Endpoint | Method | Security | Description |
|---|---|---|---|
| `/oauth2/authorization/github` | `GET` | Public | Initiates GitHub OAuth authorization code redirect |
| `/login/oauth2/code/github` | `GET` | Public | GitHub callback handler: exchanges authorization code for token |
| `/api/v1/auth/csrf` | `GET` | Public | Initializes/reads CSRF cookie and metadata |
| `/api/v1/auth/status` | `GET` | Public | Returns `{ authenticated: boolean, userId: number, username: string }` |
| `/api/v1/auth/me` | `GET` | Authenticated | Returns current authenticated `UserDto` profile |
| `/api/v1/auth/logout` | `POST` | Authenticated (CSRF required) | Invalidates HTTP session, clears security context, deletes `JSESSIONID` |

---

## 5. Security & Session Design
- **No Token Storage in LocalStorage**: Access tokens remain confidential to the backend server and are never exposed to browser storage.
- **Session Cookie Protection**:
  - `HttpOnly`: Prevents client-side script access (mitigating XSS theft).
  - `SameSite=Lax`: Mitigates Cross-Site Request Forgery on top-level navigations.
  - `Secure`: Configurable for HTTPS environments.
- **CSRF Defense**: Double-submit CSRF cookie pattern via `CookieCsrfTokenRepository` (`XSRF-TOKEN` cookie read by client and sent in `X-XSRF-TOKEN` header on non-GET requests).
- **Session Fixation Defense**: Session ID is rotated upon authentication via `.sessionFixation().changeSessionId()`.
