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
          <h2 className="font-heading text-xl text-[#F1ECE2]">1. CORE PRINCIPLE: MINIMAL DATA COLLECTION</h2>
          <p>
            OpenSourceContributor is an open-source issue discovery platform for developers. We collect only the data strictly required to provide verified issue discovery, saved issue bookmarks, contribution tracking, and account personalization. We do not sell, rent, or monetize your data.
          </p>
        </div>

        {/* Section 2 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">2. GITHUB OAUTH &amp; CREDENTIAL HANDLING</h2>
          <p>
            When authenticating via GitHub OAuth 2.0, we request read-only access to your public profile strictly using the minimal <code className="text-[#F4C13D]">read:user</code> scope to establish your contributor identity. We receive your unique GitHub numeric ID, username (<code className="text-[#F4C13D]">login</code>), public display name (<code className="text-[#F4C13D]">name</code>), public avatar URL (<code className="text-[#F4C13D]">avatar_url</code>), and public bio. Contributor email addresses are not requested, fetched, or stored in our application database under this scope.
          </p>
          <p>
            We never request repository write permissions, we never access private repositories, and we never commit code or open pull requests on your behalf. OpenSourceContributor does not retain external GitHub OAuth access tokens after establishing your authenticated session; tokens are discarded immediately post-handshake.
          </p>
        </div>

        {/* Section 3 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">3. AI QUERY PROCESSING (GROQ INFERENCE)</h2>
          <p>
            When utilizing AI-assisted search, your natural-language search query is interpreted using an external language model hosted by Groq Inc. The application is configured to use the <code className="text-[#F4C13D]">openai/gpt-oss-20b</code> model (via Groq’s OpenAI-compatible API endpoint).
          </p>
          <p>
            <strong>Exact Data Sent to Groq:</strong> For each AI search request, the application makes exactly one API call to Groq containing only:
          </p>
          <ul className="list-disc pl-5 space-y-1">
            <li>A static system prompt providing instructions for JSON criteria extraction.</li>
            <li>The search prompt text you typed into the search bar.</li>
          </ul>
          <p>
            <strong>Absence of Identifying Information:</strong> The request transmitted to Groq does <em>not</em> include your user ID, GitHub username, display name, email address, session cookies (<code className="text-[#F4C13D]">JSESSIONID</code>, <code className="text-[#F4C13D]">XSRF-TOKEN</code>), OAuth tokens, client credentials, or database identifiers.
          </p>
          <p>
            <strong>Application-Side Storage:</strong> OpenSourceContributor does not store your search queries, prompts, or AI-generated responses in the PostgreSQL database. Only an aggregated numeric counter (<code className="text-[#F4C13D]">request_count</code>) per calendar day is stored in the database to enforce the daily quota of 20 searches. Match explanations for discovered issues are generated deterministically by backend heuristics, not by subsequent AI calls.
          </p>
          <p>
            <strong>Third-Party Processing by Groq:</strong> AI requests are processed on an isolated, per-query basis without passing conversational history or persistent user contexts. Processing, transient transmission, and any infrastructure telemetry at the inference provider level are subject to Groq Inc.’s independent privacy policy and API terms of service.
          </p>
        </div>

        {/* Section 4 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">4. UPSTREAM APIS &amp; INFRASTRUCTURE PROCESSORS</h2>
          <p>
            OpenSourceContributor integrates with the following specialized third-party services to operate the platform:
          </p>
          <ul className="list-disc pl-5 space-y-2">
            <li>
              <strong>GitHub Inc. (Identity &amp; Issue Provider):</strong> Provides OAuth 2.0 authentication and public repository/issue metadata via GitHub REST APIs. All indexed issue data originates from publicly accessible open-source projects.
            </li>
            <li>
              <strong>Groq Inc. (AI Inference Provider):</strong> Executes real-time language model inference using the configured model (default: <code className="text-[#F4C13D]">openai/gpt-oss-20b</code>) to extract structured search criteria (languages, labels, keywords, difficulty) from natural-language contributor prompts on an isolated, per-search basis.
            </li>
            <li>
              <strong>Vercel Inc. (Frontend Hosting &amp; Edge Routing):</strong> Hosts the static React single-page application and executes edge reverse-proxy rewrites for backend API and authentication traffic.
            </li>
            <li>
              <strong>Render Services Inc. (Backend Hosting &amp; Database):</strong> Hosts the Spring Boot application service and provides managed PostgreSQL database storage for contributor accounts and saved bookmarks.
            </li>
          </ul>
        </div>

        {/* Section 5 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">5. BROWSER COOKIES &amp; LOCAL STORAGE</h2>
          <p>
            OpenSourceContributor utilizes only first-party, essential session cookies and local storage:
          </p>
          <ul className="list-disc pl-5 space-y-1">
            <li><code className="text-[#F4C13D]">JSESSIONID</code>: Standard HTTP-only server session cookie to authenticate your logged-in state across requests.</li>
            <li><code className="text-[#F4C13D]">XSRF-TOKEN</code>: Cookie to protect against Cross-Site Request Forgery (CSRF) on state-changing API operations.</li>
            <li><code className="text-[#F4C13D]">osc_cookie_prefs</code> / <code className="text-[#F4C13D]">osc_cookie_consent</code>: Local storage entries recording your interface and storage preferences.</li>
          </ul>
          <p className="pt-1">
            We do not use third-party analytics cookies, advertising pixels, or cross-site behavioral trackers.
          </p>
        </div>

        {/* Section 6 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">6. ACCOUNT DELETION &amp; DATA RETENTION</h2>
          <p>
            You have the right to permanently delete your OpenSourceContributor account at any time via Account &amp; Stack Settings.
          </p>
          <p>
            <strong>Application Data (PostgreSQL):</strong> Executing account deletion permanently and irreversibly purges your contributor profile, personal preferences, bookmarked issues, contribution tracking ledger records, and AI usage counters from our database. Active browser sessions are immediately invalidated across all devices. This application-level deletion cannot be undone.
          </p>
          <p>
            <strong>Temporary In-Memory Rate-Limiting Data:</strong> To protect upstream GitHub REST APIs against abusive scraping, client IP request counts for public guest discovery are tracked strictly in temporary application memory within rolling one-hour sliding windows. This transient rate-limiting data is not stored in our PostgreSQL database and automatically expires from memory.
          </p>
          <p>
            <strong>Logs Controlled by the Hosting Provider:</strong> Low-level network routing, firewall, and web-server connection logs (such as request timestamps, IP addresses, HTTP methods, and response codes) are generated and maintained independently by external hosting and infrastructure providers. Account deletion purges your data from OpenSourceContributor’s application database, but does not remove or alter server logs controlled by the hosting provider under their own independent infrastructure policies.
          </p>
        </div>

        {/* Section 7 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">7. DATA PROTECTION &amp; GRIEVANCE CONTACT</h2>
          <p>
            OpenSourceContributor is committed to honoring global privacy standards and transparent data management. If you have questions regarding data processing, wish to request manual export or correction of your contributor records, or seek to submit a privacy grievance, please contact our designated contact channel:
          </p>
          <div className="p-4 border border-[#F4C13D]/40 bg-[#1A1A1A] space-y-2">
            <div className="text-[#F4C13D] font-bold text-xs uppercase tracking-wider flex items-center space-x-2">
              <Mail size={14} />
              <span>Grievance Officer / Privacy Contact</span>
            </div>
            <p className="text-[#A7A29A]">
              Designated Grievance &amp; Privacy Officer: <span className="text-[#F1ECE2] font-semibold">[AWAITING OWNER CONFIGURATION — CONTACT_EMAIL]</span>
            </p>
            <p className="text-[#A7A29A]">
              Response Window: Acknowledgement within 48 hours; resolution within 30 days of receipt.
            </p>
          </div>
          <div className="space-y-2 pt-2 text-[#A7A29A]">
            <p>
              <strong>Regulatory Framework &amp; DPDP Notice:</strong> For contributors accessing this service from India, personal data is processed in accordance with the Digital Personal Data Protection Act, 2023 (DPDP Act). Please note that while the primary enactment is passed, specific delegated rules, statutory notices, and compliance transition periods (anticipated 2026–2027) are currently being framed and notified by the Central Government.
            </p>
            <p>
              OpenSourceContributor is designed for software developers, students, and engineers participating in open-source collaboration. Applicability of verifiable parental consent mechanisms to purely technical developer utility platforms under upcoming DPDP delegated rules is subject to final guidance and determination by repository owner legal counsel.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
