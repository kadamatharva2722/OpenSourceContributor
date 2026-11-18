import React from 'react';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { FileText, ShieldAlert, Cpu, CheckCircle2, AlertTriangle, ExternalLink } from 'lucide-react';

export function TermsPage() {
  useDocumentTitle('Terms of Service');
  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-12">
        <div className="max-w-4xl mx-auto space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-[#F06522] uppercase tracking-widest">
            <FileText size={14} />
            <span>LEGAL ARCHIVE // TERMS OF SERVICE</span>
          </div>
          <h1 className="font-heading text-4xl uppercase tracking-tight text-[#F1ECE2]">
            TERMS &amp; CONDITIONS
          </h1>
          <p className="font-mono text-xs text-[#A7A29A]">
            Effective: January 2026 · Contributor Service Terms
          </p>
        </div>
      </section>

      <section className="px-4 md:px-8 py-12 max-w-4xl mx-auto w-full space-y-8 font-mono text-xs text-[#D0CBC1] leading-relaxed">
        {/* Section 1 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">1. ACCEPTABLE USAGE &amp; CONTRIBUTOR ETIQUETTE</h2>
          <p>
            OpenSourceContributor is designed to connect software developers with verified open-source issues and facilitate genuine open-source contributions.
          </p>
          <p>
            Users agree not to utilize this platform for automated bot spam, scraping abuse, unsolicited commercial solicitations in GitHub issue threads, or harassment of repository maintainers. Maintainer attention is valuable; contributions should follow repository guidelines and community codes of conduct.
          </p>
        </div>

        {/* Section 2 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">2. GITHUB &amp; THIRD-PARTY SERVICE DEPENDENCIES</h2>
          <p>
            All issue records, pull request data, and repository statistics are retrieved from the public GitHub REST API in accordance with GitHub's Terms of Service.
          </p>
          <p>
            OpenSourceContributor is an independent discovery platform and is not affiliated with, endorsed by, or sponsored by GitHub, Inc. Search availability and issue freshness are contingent upon upstream GitHub API operational status and rate limits.
          </p>
        </div>

        {/* Section 3 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">3. AI-GENERATED EXPLANATIONS &amp; LIMITATIONS</h2>
          <p>
            Match scores, skill inference, and issue explanations are generated using machine-learning inference (Groq/Llama).
          </p>
          <p>
            These explanations are provided for navigational guidance and discovery assistance only. AI-generated match summaries do not constitute an official endorsement by repository maintainers and may occasionally misinterpret project architecture or complexity.
          </p>
        </div>

        {/* Section 4 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">4. USER RESPONSIBILITY FOR UPSTREAM VERIFICATION</h2>
          <p>
            Before cloning repositories, claiming tasks, or writing code, contributors are solely responsible for:
          </p>
          <ul className="list-disc pl-5 space-y-1">
            <li>Reviewing the live issue thread directly on GitHub to confirm the issue is unassigned and actively wanted.</li>
            <li>Inspecting the destination repository's <code className="text-[#F4C13D]">CONTRIBUTING.md</code> and Code of Conduct.</li>
            <li>Confirming the project's development workflow (e.g. branch naming, linting rules, and pull request conventions).</li>
          </ul>
        </div>

        {/* Section 5 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">5. SERVICE AVAILABILITY &amp; SEARCH WINDOW LIMITS</h2>
          <p>
            OpenSourceContributor is provided on an "as is" and "as available" basis without warranties of uninterrupted service.
          </p>
          <p>
            In accordance with GitHub Search API specifications, the maximum accessible search window is limited to the first 1,000 matching results per search query. We encourage using specific language, topic, and difficulty filters to narrow discovery scope.
          </p>
        </div>

        {/* Section 6 */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3">
          <h2 className="font-heading text-xl text-[#F1ECE2]">6. INTELLECTUAL PROPERTY &amp; CODEBASE LICENSING</h2>
          <p>
            Indexed repositories, issue titles, issue descriptions, and code snippets remain the intellectual property of their respective creators and contributors under their declared open-source licenses (such as MIT, Apache-2.0, BSD, or GPL).
          </p>
          <p>
            Contributors are responsible for complying with the specific license requirements and Contributor License Agreements (CLAs) of each project they contribute to.
          </p>
        </div>
      </section>
    </div>
  );
}
