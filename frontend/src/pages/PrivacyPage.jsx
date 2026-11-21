import React from 'react';
import { ROUTES } from '../constants/routes';
import { usePageSEO } from '../hooks/usePageSEO';
import { Shield, Lock, Cpu, Globe, Cookie, Trash2, Mail } from 'lucide-react';

export function PrivacyPage() {
  usePageSEO({
    title: 'Privacy Policy',
    description:
      'Read the OpenSourceContributor data governance policy. Learn how we handle GitHub OAuth scopes, minimal telemetry, and contributor data protection.',
    canonicalPath: ROUTES.PRIVACY,
  });

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-12">
        <div className="max-w-4xl mx-auto space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-[#F4C13D] uppercase tracking-widest">
            <Shield size={14} />
            <span>LEGAL ARCHIVE // DATA GOVERNANCE</span>
          </div>
          <h1 className="font-heading text-4xl uppercase tracking-tight text-[#F1ECE2]">
            PRIVACY POLICY
          </h1>
          <p className="font-mono text-xs text-[#A7A29A]">
            Effective: January 2026 · Technical Data Governance Specification
          </p>
        </div>
      </section>

      <section className="px-4 md:px-8 py-12 max-w-4xl mx-auto w-full space-y-8 font-mono text-xs text-[#D0CBC1] leading-relaxed">
        {/* Section 1 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">1. CORE PRINCIPLE &amp; DATA CATEGORIES</h2>
          <p>
            OpenSourceContributor is an open-source issue discovery platform designed to help software developers discover relevant GitHub issues and organize their open-source contribution workflows. We do not sell, rent, trade, or monetize your personal information.
          </p>
          <p>
            To deliver open-source issue discovery, authenticated workspace tooling, platform security, and operational reliability, the application processes the following categories of data:
          </p>
          <ul className="list-disc pl-5 space-y-1.5">
            <li>
              <strong>GitHub Public Profile Data:</strong> When you authenticate via GitHub OAuth, we collect your stable numeric GitHub user ID, username (<code className="text-[#F4C13D]">login</code>), public display name (<code className="text-[#F4C13D]">name</code>), public avatar URL (<code className="text-[#F4C13D]">avatar_url</code>), and public bio, where provided on your profile.
            </li>
            <li>
              <strong>Contributor Workspace Records:</strong> Contributor-saved issues (bookmarked issues, repository full names, issue numbers, titles, and issue URLs) and contribution tracking ledger items (work status, branch names, pull request URLs, and optional notes).
            </li>
            <li>
              <strong>Personalization &amp; Platform Quotas:</strong> Discovery preferences (preferred programming languages, domain topics, and target difficulty levels) and daily AI search usage counters (<code className="text-[#F4C13D]">request_count</code>) to enforce per-user rate limits.
            </li>
            <li>
              <strong>Voluntary Communications:</strong> Name, email address, topic, subject, and message content voluntarily submitted through the platform's Contact desk or issue reporting forms.
            </li>
            <li>
              <strong>Security &amp; Rate-Limiting Telemetry:</strong> Client IP addresses temporarily tracked in server memory to enforce rolling rate limits on guest searches, OAuth handshakes, and public form submissions, preventing automated scraping and service abuse.
            </li>
            <li>
              <strong>Technical &amp; Analytics Data:</strong> Aggregated, privacy-preserving technical diagnostics, page paths, referrer URLs, browser types, operating systems, and approximate regional locations collected via infrastructure hosting and web analytics providers.
            </li>
          </ul>
        </div>

        {/* Section 2 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">2. GITHUB OAUTH &amp; CREDENTIAL HANDLING</h2>
          <p>
            OpenSourceContributor utilizes GitHub OAuth 2.0 strictly for user authentication, contributor identity verification, and browser session establishment.
          </p>
          <p>
            <strong>Minimal Read-Only Scope:</strong> The application requests access strictly using the minimal <code className="text-[#F4C13D]">read:user</code> scope. We receive your unique GitHub numeric ID, username (<code className="text-[#F4C13D]">login</code>), public display name, avatar URL, and bio. Contributor email addresses are neither requested, accessed, nor stored as part of the GitHub OAuth authentication flow.
          </p>
          <p>
            <strong>Zero Token Retention Policy:</strong> OpenSourceContributor implements a strict zero-token retention architecture via a custom <code className="text-[#F4C13D]">DiscardingOAuth2AuthorizedClientRepository</code>. Once your identity is authenticated and a secure backend session is created, the raw GitHub OAuth access token is immediately discarded. OAuth access tokens are never persisted in our database, never stored in session storage, and never cached in server memory.
          </p>
          <p>
            <strong>No Repository Write Access:</strong> We never request repository write permissions (<code className="text-[#F4C13D]">repo</code>), we never access private repositories, and we never commit code, create branches, or open pull requests on your behalf. All repository and issue discovery data is retrieved from public GitHub REST APIs using unauthenticated or application-level requests.
          </p>
        </div>

        {/* Section 3 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">3. AI QUERY PROCESSING (GROQ INFERENCE)</h2>
          <p>
            When utilizing AI-assisted search, your natural-language search prompt is interpreted by an external large language model hosted by Groq Inc. The application is configured to use the <code className="text-[#F4C13D]">openai/gpt-oss-20b</code> model (or an operator-configured model) via Groq's OpenAI-compatible API endpoint.
          </p>
          <p>
            <strong>Exact Data Transmitted to Groq:</strong> For each AI search execution, the backend sends a single API request containing only:
          </p>
          <ul className="list-disc pl-5 space-y-1">
            <li>A static system prompt detailing JSON schema rules for extracting programming languages, issue labels, keywords, and difficulty filters.</li>
            <li>The raw natural-language search query entered into the search input.</li>
          </ul>
          <p>
            <strong>Absence of Identifying Context:</strong> Requests dispatched to Groq do not contain user database IDs, GitHub numeric IDs, usernames, display names, email addresses, session cookies (<code className="text-[#F4C13D]">JSESSIONID</code>, <code className="text-[#F4C13D]">XSRF-TOKEN</code>), OAuth credentials, or client IP addresses. Each query is processed independently without conversational history or cross-query user context.
          </p>
          <p>
            <strong>Application Database Storage:</strong> OpenSourceContributor does not store your search queries, parsed JSON filter parameters, or raw AI model responses in the PostgreSQL database. Only an aggregated numeric counter (<code className="text-[#F4C13D]">request_count</code>) tied to your user ID is recorded in the <code className="text-[#F4C13D]">ai_usage</code> table per calendar day to enforce the daily quota of 20 searches.
          </p>
          <p>
            <strong>Heuristic Explanations &amp; Application Logs:</strong> Match explanations and relevance scores displayed alongside discovered issues are generated deterministically by backend heuristics (<code className="text-[#F4C13D]">MatchExplanationGenerator</code>), not through additional AI inference calls. Application loggers may record search prompt text in temporary application server logs for operational debugging and error diagnostics.
          </p>
          <p>
            <strong>Provider Processing:</strong> Data transmission, inference execution, and infrastructure-level telemetry at the inference layer are governed independently by <a href="https://groq.com/privacy-policy/" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Groq Inc.'s Privacy Policy</a>.
          </p>
        </div>

        {/* Section 4 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">4. THIRD-PARTY INFRASTRUCTURE PROCESSORS</h2>
          <p>
            OpenSourceContributor integrates with trusted third-party infrastructure and service providers to operate the application:
          </p>
          <ul className="list-disc pl-5 space-y-2">
            <li>
              <strong>GitHub Inc. (Identity &amp; Public Issue Provider):</strong> Provides OAuth 2.0 authentication services and public repository/issue metadata via GitHub REST APIs. Indexed issue metadata is derived entirely from publicly accessible open-source repositories. Privacy Policy: <a href="https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">GitHub Privacy Statement</a>.
            </li>
            <li>
              <strong>Groq Inc. (AI Inference Provider):</strong> Executes real-time language model inference to parse natural-language search criteria into structured filter attributes on an isolated, per-query basis. Privacy Policy: <a href="https://groq.com/privacy-policy/" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Groq Privacy Policy</a>.
            </li>
            <li>
              <strong>Vercel Inc. (Frontend Hosting &amp; Web Analytics):</strong> Hosts the React single-page frontend application, provides edge reverse-proxy routing, and collects privacy-preserving, cookie-free web analytics. Privacy Policy: <a href="https://vercel.com/legal/privacy-policy" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Vercel Privacy Policy</a>.
            </li>
            <li>
              <strong>Render Services Inc. (Backend Application Hosting):</strong> Hosts the Spring Boot backend service, manages server execution environments, and processes incoming API traffic and rate limiting. Privacy Policy: <a href="https://render.com/privacy" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Render Privacy Policy</a>.
            </li>
            <li>
              <strong>Neon Inc. (Managed PostgreSQL Database):</strong> Provides managed, serverless PostgreSQL cloud database storage for user accounts, workspace bookmarks, contribution tracking records, and AI usage counters. Privacy Policy: <a href="https://neon.tech/privacy-policy" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Neon Privacy Policy</a>.
            </li>
          </ul>
        </div>

        {/* Section 5 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">5. BROWSER COOKIES, LOCAL STORAGE &amp; ANALYTICS</h2>
          <p>
            OpenSourceContributor utilizes essential first-party cookies, browser local storage, and privacy-focused web analytics:
          </p>
          <div className="space-y-2 pt-1">
            <h3 className="text-sm font-bold text-[#F1ECE2]">A. Essential First-Party Cookies</h3>
            <ul className="list-disc pl-5 space-y-1">
              <li><code className="text-[#F4C13D]">JSESSIONID</code>: An HTTP-only, secure session cookie generated by Spring Security to maintain your authenticated contributor state across requests. It expires upon browser session termination or 30 minutes of inactivity.</li>
              <li><code className="text-[#F4C13D]">XSRF-TOKEN</code>: A secure cookie providing Cross-Site Request Forgery (CSRF) protection on mutating API calls (<code className="text-[#F4C13D]">POST</code>, <code className="text-[#F4C13D]">PUT</code>, <code className="text-[#F4C13D]">DELETE</code>, <code className="text-[#F4C13D]">PATCH</code>).</li>
            </ul>
          </div>
          <div className="space-y-2 pt-1">
            <h3 className="text-sm font-bold text-[#F1ECE2]">B. Browser Local Storage (Not Cookies)</h3>
            <ul className="list-disc pl-5 space-y-1">
              <li><code className="text-[#F4C13D]">osc_cookie_consent</code>: Stores your cookie banner acknowledgment status and timestamp within your browser.</li>
              <li><code className="text-[#F4C13D]">osc_cookie_prefs</code>: Records your granular storage preferences (essential, offline cache, analytics preferences).</li>
            </ul>
            <p className="text-[11px] text-[#A7A29A]">
              Note: Local storage data remains stored exclusively on your device and is not automatically transmitted in HTTP request headers.
            </p>
          </div>
          <div className="space-y-2 pt-1">
            <h3 className="text-sm font-bold text-[#F1ECE2]">C. Vercel Web Analytics</h3>
            <p>
              The frontend includes Vercel Web Analytics to measure site traffic and performance trends. In accordance with <a href="https://vercel.com/docs/analytics" target="_blank" rel="noopener noreferrer" className="text-[#F4C13D] underline hover:text-[#ffd666]">Vercel Analytics Documentation</a>:
            </p>
            <ul className="list-disc pl-5 space-y-1">
              <li>Vercel Web Analytics does not use third-party tracking cookies, advertising IDs, or cross-site behavioral tracking scripts.</li>
              <li>Unique visitors are calculated using an anonymized, daily-rotated hash of the visitor's IP address, User-Agent, and domain name, preventing permanent individual tracking.</li>
              <li>Processed metrics include page URLs, referrer sources, device categories, browser engines, operating systems, and approximate geographic location (country/region).</li>
            </ul>
          </div>
        </div>

        {/* Section 6 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">6. ACCOUNT DELETION &amp; DATA RETENTION</h2>
          <p>
            You have the right to permanently delete your OpenSourceContributor account at any time through Account Settings.
          </p>
          <p>
            <strong>Application Database Purge:</strong> When you initiate account deletion, the system enforces a recent re-authentication check (within 15 minutes) and atomically deletes your records from the live PostgreSQL database. This includes your user profile (<code className="text-[#F4C13D]">users</code>), saved issue bookmarks (<code className="text-[#F4C13D]">saved_issues</code>), contribution tracking entries (<code className="text-[#F4C13D]">contributions</code>), discovery preferences (<code className="text-[#F4C13D]">user_preferences</code>), and AI usage quota counters (<code className="text-[#F4C13D]">ai_usage</code>). This deletion is irreversible.
          </p>
          <p>
            <strong>Session Invalidation:</strong> Upon deletion, your active HTTP session is invalidated, the <code className="text-[#F4C13D]">JSESSIONID</code> cookie is expired, and active sessions registered in the server's in-memory session registry are terminated. Subsequent requests from any device using the deleted credentials are automatically rejected with HTTP 401 Unauthorized via our account validation filter.
          </p>
          <p>
            <strong>Administrative &amp; Separate Records:</strong> Inquiries submitted through the Contact desk (<code className="text-[#F4C13D]">contact_messages</code>), issue reports, and repository requests are stored in separate administrative tables for support and platform moderation; these are not automatically linked to or deleted by contributor account deletion. Administrative action audit logs (<code className="text-[#F4C13D]">admin_audit_logs</code>) are maintained for system integrity and accountability.
          </p>
          <p>
            <strong>Temporary In-Memory Telemetry:</strong> Client IP addresses tracked for rolling rate limiting on guest searches and public form submissions reside strictly in transient application memory (rolling one-hour or 10-minute sliding windows) and expire automatically.
          </p>
          <p>
            <strong>Backups &amp; Infrastructure Provider Logs:</strong> Account deletion immediately purges records from the live database. Encrypted database disaster-recovery backups managed by Neon and web/server access logs maintained by hosting providers (Render, Vercel) follow independent retention schedules governed by provider operational policies and are overwritten or expired over time.
          </p>
        </div>

        {/* Section 7 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">7. DATA SUBJECT RIGHTS, DPDP FRAMEWORK &amp; GRIEVANCE CONTACT</h2>
          <p>
            OpenSourceContributor is committed to transparent data governance and respecting contributor privacy rights under applicable data protection laws.
          </p>
          <div className="space-y-2 pt-1">
            <h3 className="text-sm font-bold text-[#F1ECE2]">A. Contributor Rights &amp; Self-Service</h3>
            <p>
              Subject to applicable legal frameworks, contributors have the right to access personal data associated with their account, request correction of inaccurate records, and obtain erasure of their account and associated workspace data. You may self-manage discovery preferences, contribution records, and account deletion directly within your Account Settings.
            </p>
          </div>
          <div className="space-y-2 pt-1">
            <h3 className="text-sm font-bold text-[#F1ECE2]">B. Digital Personal Data Protection (DPDP) Notice (India)</h3>
            <p>
              For contributors accessing OpenSourceContributor from India, personal data is governed under the <strong>Digital Personal Data Protection Act, 2023</strong> (DPDP Act) and the <strong>Digital Personal Data Protection Rules, 2025</strong>.
            </p>
            <p>
              The statutory framework operates on a phased commencement schedule established by the Central Government. While foundational institutional and procedural provisions are being phased in, the principal operational data-processing obligations under the statutory framework are scheduled to commence in <strong>May 2027</strong>, subject to official Government of India notifications, transitional rules, and amendments.
            </p>
            <p>
              OpenSourceContributor is an open-source collaboration tool designed for software developers, engineers, and students. The platform does not claim blanket exemption from children's personal data obligations. As corresponding delegated rules, technical standards, and verifiable parental consent guidelines take legal effect and become applicable, the platform intends to comply with all applicable requirements governing personal data of individuals under eighteen years of age.
            </p>
          </div>
          <div className="p-4 border border-[#F4C13D]/40 bg-[#1A1A1A] space-y-2 mt-3">
            <div className="text-[#F4C13D] font-bold text-xs uppercase tracking-wider flex items-center space-x-2">
              <Mail size={14} />
              <span>Privacy Inquiries &amp; Grievance Communication</span>
            </div>
            <p className="text-[#A7A29A]">
              Contributors seeking to exercise data subject rights, submit privacy questions, or communicate grievances may do so through the platform's official <a href={ROUTES.CONTACT} className="text-[#F4C13D] underline hover:text-[#ffd666]">Contact Dispatch Desk</a> (selecting topic <em>General Inquiry</em> or <em>Platform Feedback</em>) or by opening an issue on the official OpenSourceContributor GitHub repository.
            </p>
            <p className="text-[#A7A29A] text-[11px]">
              Note: Formal designated Grievance Officer email configuration is pending administrative establishment by the platform operator. Inquiries submitted via the Contact desk are recorded for maintainer review, and the platform endeavors to investigate and address verified privacy communications in a timely and diligent manner.
            </p>
          </div>
          <div className="pt-2 text-[11px] text-[#A7A29A]">
            <p>
              <strong>Security Safeguards:</strong> We employ defense-in-depth safeguards including strict session cookies (<code className="text-[#F4C13D]">HttpOnly</code>, <code className="text-[#F4C13D]">SameSite</code>, <code className="text-[#F4C13D]">Secure</code>), Cross-Site Request Forgery (CSRF) protection, zero-token OAuth storage, rate limiting, and encrypted HTTPS transit.
            </p>
            <p className="pt-1">
              <strong>Policy Revisions:</strong> This policy may be updated periodically to reflect architectural changes or regulatory developments. Updated specifications will be published to this URL with an updated effective date.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
