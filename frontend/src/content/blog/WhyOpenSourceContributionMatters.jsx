import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../../constants/routes';
import {
  ExternalLink,
  ShieldAlert,
  HelpCircle,
  Sparkles,
  ArrowRight,
} from 'lucide-react';

export function WhyOpenSourceContributionMattersContent() {
  return (
    <article className="space-y-12 text-[#D0CBC1] font-sans text-base leading-relaxed">
      {/* 1. Introduction: Why Open Source Matters Today */}
      <section id="introduction" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          1. Introduction: Why Open Source Matters Today
        </h2>
        <p>
          Open-source software forms an essential foundation of contemporary technology. From operating system kernels and web browsers to database engines, programming runtimes, and developer tooling, modern digital systems rely heavily on shared, open-source codebases.
        </p>
        <p>
          Making an <strong className="text-[#F1ECE2]">open source contribution</strong> allows developers to participate directly in this collaborative ecosystem. Rather than writing software exclusively in private sandboxes, contributing connects you to active software communities where real users, complex requirements, and diverse developer perspectives converge.
        </p>
        <p>
          Whether you are exploring an <strong className="text-[#F1ECE2]">open source contribution for beginners</strong> or looking to deepen your existing software engineering skills, understanding how open source works—and why it matters—is a valuable step in your development journey.
        </p>
      </section>

      {/* 2. What Is Open Source? */}
      <section id="what-is-open-source" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          2. What Is Open Source?
        </h2>
        <p>
          Open-source software is software distributed under a license that grants users specific rights to use, study, modify, and redistribute the software. These principles are formally codified in the{' '}
          <a
            href="https://opensource.org/osd"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
          >
            Open Source Definition <ExternalLink size={13} />
          </a>{' '}
          maintained by the Open Source Initiative (OSI).
        </p>
        <p>
          Under compliant open-source licenses, software provides core liberties:
        </p>
        <ul className="list-disc pl-6 space-y-1.5 text-sm md:text-base">
          <li>
            <strong>Source Code Access:</strong> Users have the right to inspect the actual source code rather than relying solely on opaque, compiled binaries.
          </li>
          <li>
            <strong>Right to Modify:</strong> Developers are legally permitted to tailor the software, fix bugs, optimize algorithms, or experiment with enhancements.
          </li>
          <li>
            <strong>Right to Redistribute:</strong> Copies of the original or modified code can be shared with others under the terms of the applicable license.
          </li>
          <li>
            <strong>No Discrimination:</strong> The license must not restrict who can use the software or what fields of work (commercial, academic, or personal) it can be applied to.
          </li>
        </ul>

        {/* Callout: Public Repo != Open Source */}
        <div className="border border-[#F06522]/40 bg-[#161616] p-5 space-y-2 border-l-4 border-l-[#F06522]">
          <div className="flex items-center gap-2 text-xs font-mono text-[#F06522] uppercase tracking-wider font-bold">
            <ShieldAlert size={15} />
            <span>Essential Distinction: Public Visibility vs. Open-Source Licensing</span>
          </div>
          <p className="text-xs md:text-sm text-[#D0CBC1] leading-relaxed">
            A common misconception among early-career developers is assuming that any repository publicly visible on GitHub is automatically open source. Under copyright law, if a repository does not have an explicit open-source license, default copyright applies: the author reserves all rights, and other developers have no legal permission to copy, adapt, modify, or distribute the code. For guidance on establishing clear terms, review the{' '}
            <a
              href="https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              GitHub guide to licensing a repository <ExternalLink size={12} />
            </a>
            .
          </p>
        </div>

        <p className="pt-2">
          Open-source licenses differ in their specific conditions and legal obligations:
        </p>
        <ul className="list-disc pl-6 space-y-2 text-xs md:text-sm">
          <li>
            <strong>Permissive Licenses (MIT, Apache 2.0, BSD):</strong> Grant broad freedom to use, modify, and distribute code with few restrictions. While all require preserving copyright notices, Apache 2.0 additionally provides explicit patent rights grants and trademark restrictions.
          </li>
          <li>
            <strong>Copyleft Licenses (GPL):</strong> The GNU General Public License (GPL) imposes reciprocal conditions: when modified versions of the software are distributed, they must be made available under the same license terms.
          </li>
          <li>
            <strong>Network Copyleft Licenses (AGPL):</strong> The GNU Affero General Public License (AGPLv3) addresses network-hosted software by requiring that if you modify a covered program and run it on a server, you must offer users interacting with it remotely through a computer network an opportunity to receive the Corresponding Source of your version, subject to the conditions of the license.
          </li>
        </ul>
        <p className="text-xs text-[#A7A29A] font-mono">
          Note: Because license requirements are not identical, contributors and organizations should always inspect each project's declared license.
        </p>
      </section>

      {/* 3. Why Open Source Is Important Today */}
      <section id="why-open-source-is-important" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          3. Why Open Source Is Important Today
        </h2>
        <p>
          Open source has transformed software engineering by encouraging shared problem-solving and collective stewardship. Key aspects of its importance include:
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">Global Collaboration</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Engineers across countries and time zones collaborate asynchronously on common software infrastructure, bringing diverse expertise and review to shared codebases.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">Accelerated Innovation</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Shared foundational components—such as HTTP parsers, data serializers, and cryptography libraries—allow engineering teams to build higher-level solutions without continually reinventing basic building blocks.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">Transparency &amp; Code Inspection</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Open source allows engineers and researchers to inspect source code directly, verify behavior, audit implementations, and diagnose root causes rather than treating third-party code as a black box.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">Accessible Educational Resources</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Production repositories serve as an invaluable public learning resource. Developers worldwide can study how experienced teams structure projects, write tests, and manage releases.
            </p>
          </div>
        </div>

        <p className="pt-2">
          Key open-source projects illustrate this collaborative model:
        </p>
        <ul className="list-disc pl-6 space-y-2 text-xs md:text-sm">
          <li>
            <strong>Linux:</strong> An open-source operating system kernel created by Linus Torvalds in 1991, widely utilized across cloud server environments, supercomputers, and mobile devices (via the Android platform).
          </li>
          <li>
            <strong>Git:</strong> The distributed version control system developed by Linus Torvalds in 2005 to manage the Linux kernel, now standard across modern software development workflows.
          </li>
          <li>
            <strong>React:</strong> Created by Jordan Walke at Facebook (Meta) and open-sourced in 2013, React is maintained by a dedicated team at Meta alongside an active international community of contributors, as highlighted in the{' '}
            <a
              href="https://react.dev/community/acknowledgements"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              React community acknowledgements <ExternalLink size={12} />
            </a>
            .
          </li>
          <li>
            <strong>PostgreSQL:</strong> Originated from the POSTGRES project at the University of California, Berkeley in 1986 under the leadership of Professor Michael Stonebraker. The project adopted the name PostgreSQL in 1996 when SQL support was added, as documented in the official{' '}
            <a
              href="https://www.postgresql.org/docs/current/history.html"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              PostgreSQL history documentation <ExternalLink size={12} />
            </a>
            .
          </li>
          <li>
            <strong>Kubernetes:</strong> An open-source container orchestration system that originated at Google based on internal experience with its Borg cluster manager. It was open-sourced in 2014 and donated in 2016 to form the founding project of the{' '}
            <a
              href="https://www.cncf.io/projects/kubernetes/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              Cloud Native Computing Foundation (CNCF) <ExternalLink size={12} />
            </a>
            .
          </li>
        </ul>

        {/* Security disclaimer */}
        <div className="border border-[#2E2E2E] bg-[#141414] p-4 text-xs font-mono text-[#A7A29A] space-y-1">
          <p className="font-bold text-[#F1ECE2]">A Realistic Note on Security &amp; Maintenance:</p>
          <p>
            While having source code public enables peer review and independent audits, open-source projects are not automatically immune to security bugs or abandonment. Projects maintained by solo volunteers may experience delayed patches, and supply-chain vulnerabilities require active vigilance. Responsible open source depends on active maintainers, reproducible builds, automated testing, and community support.
          </p>
        </div>
      </section>

      {/* 4. Why Developers Should Contribute */}
      <section id="why-developers-should-contribute" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          4. Why Developers Should Contribute
        </h2>
        <p>
          Understanding the <strong className="text-[#F1ECE2]">benefits of open source contribution</strong> helps clarify why so many software engineers participate. Working on established repositories helps you develop skills that tutorial exercises rarely replicate:
        </p>

        <div className="space-y-3 pt-2">
          <div className="border border-[#2E2E2E] bg-[#161616] p-5 space-y-1.5 border-l-4 border-l-[#5B86F2]">
            <h3 className="font-heading text-lg text-[#F1ECE2]">1. Navigating Unfamiliar Codebases</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              In software development roles, engineers rarely write applications entirely from scratch. Most daily work involves reading existing code, tracing execution paths, and making targeted updates without regressing existing behavior. Open-source contribution builds proficiency in navigating large, unfamiliar codebases.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-5 space-y-1.5 border-l-4 border-l-[#F4C13D]">
            <h3 className="font-heading text-lg text-[#F1ECE2]">2. Practicing Git &amp; Collaborative Workflows</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Contributing requires using Git in collaborative settings: managing feature branches, writing clear commit messages, linking issues in pull request summaries, resolving merge conflicts, and responding to automated CI test suites.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-5 space-y-1.5 border-l-4 border-l-[#F06522]">
            <h3 className="font-heading text-lg text-[#F1ECE2]">3. Experiencing Constructive Code Reviews</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Code reviews from experienced maintainers provide actionable feedback on software design, edge cases, naming conventions, and performance considerations. Learning to discuss technical decisions constructively is a cornerstone of professional engineering.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#161616] p-5 space-y-1.5 border-l-4 border-l-[#F29BB8]">
            <h3 className="font-heading text-lg text-[#F1ECE2]">4. Building Publicly Visible Evidence of Your Work</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Merged pull requests, issue discussions, and code commits on public repositories offer publicly visible evidence of your work. They demonstrate not only your technical ability, but also your capacity to communicate clearly and collaborate with others.
            </p>
          </div>
        </div>
      </section>

      {/* 5. Importance for College Students and Freshers */}
      <section id="for-students-and-freshers" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          5. Importance for College Students &amp; Freshers
        </h2>
        <p>
          Finding relevant <strong className="text-[#F1ECE2]">open source projects for students</strong> and early-career developers provides practical experience that complements academic study:
        </p>
        <ul className="list-disc pl-6 space-y-2 text-xs md:text-sm">
          <li>
            <strong>Bridging Classroom Theory and Production Practice:</strong> Computer science curricula provide strong theoretical fundamentals in algorithms and systems. Open source allows you to apply those concepts to production codebases with active users and automated continuous integration pipelines.
          </li>
          <li>
            <strong>Concrete Talking Points in Technical Interviews:</strong> Discussing a specific bug you investigated, the test case you wrote, or how you incorporated maintainer feedback provides authentic, grounded examples during technical and behavioral interviews.
          </li>
          <li>
            <strong>Demonstrating Initiative:</strong> Contributing to open source shows an ability to read documentation independently, set up development environments, and engage constructively with external engineering teams.
          </li>
        </ul>

        <div className="border border-[#2E2E2E] bg-[#141414] p-4 text-xs font-mono text-[#A7A29A]">
          <p className="font-bold text-[#F4C13D]">Realistic Career Perspective:</p>
          <p>
            Contributing to open source builds genuine competence and provides tangible talking points, but it does not guarantee an internship, job offer, or automatic recognition. It is best approached as an effective avenue for practical skill development, collaborative learning, and professional growth.
          </p>
        </div>
      </section>

      {/* 6. Contributions Are Not Limited to Coding */}
      <section id="non-code-contributions" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          6. Contributions Are Not Limited to Coding
        </h2>
        <p>
          Open-source software projects require diverse forms of contribution beyond writing application code. For guidance on varied ways to participate, consult the{' '}
          <a
            href="https://docs.github.com/en/get-started/exploring-projects-on-github/finding-ways-to-contribute-to-open-source-on-github"
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
          >
            GitHub documentation on finding ways to contribute <ExternalLink size={12} />
          </a>
          .
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F06522] uppercase tracking-wider block">Documentation</span>
            <p className="text-xs text-[#A7A29A]">
              Clarifying confusing installation steps, correcting outdated code samples, fixing typos, and writing tutorials. High-quality documentation directly lowers onboarding hurdles for other developers.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F4C13D] uppercase tracking-wider block">Bug Reports &amp; Reproduction</span>
            <p className="text-xs text-[#A7A29A]">
              Submitting detailed bug reports with environment details, exact reproduction steps, and minimal reproducible code snippets helps maintainers diagnose problems faster.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#5B86F2] uppercase tracking-wider block">Testing &amp; Quality Assurance</span>
            <p className="text-xs text-[#A7A29A]">
              Writing automated unit tests, testing release candidates across different operating systems, and verifying that fixed issues do not suffer from regressions.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F29BB8] uppercase tracking-wider block">UI &amp; Visual Design</span>
            <p className="text-xs text-[#A7A29A]">
              Refining component layouts, improving responsive mobile behavior, enhancing iconography, and fixing visual inconsistencies in web or desktop interfaces.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F06522] uppercase tracking-wider block">Accessibility (a11y)</span>
            <p className="text-xs text-[#A7A29A]">
              Adding appropriate ARIA attributes, ensuring keyboard navigability, checking color contrast ratios, and verifying screen-reader compatibility.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F4C13D] uppercase tracking-wider block">Localization &amp; Translation</span>
            <p className="text-xs text-[#A7A29A]">
              Translating documentation, user manuals, and interface strings into additional languages to make projects accessible to non-English-speaking developer communities.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#5B86F2] uppercase tracking-wider block">Issue Triage</span>
            <p className="text-xs text-[#A7A29A]">
              Helping project maintainers categorize incoming issues, identify duplicate tickets, verify whether bug reports remain reproducible in the latest release, and label tasks.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2">
            <span className="text-xs font-mono font-bold text-[#F29BB8] uppercase tracking-wider block">Code Maintenance &amp; Bug Fixes</span>
            <p className="text-xs text-[#A7A29A]">
              Fixing verified bugs, removing deprecated API usages, updating build configurations, and implementing scoped features requested by the community.
            </p>
          </div>
        </div>
      </section>

      {/* 7. Challenges Beginners Face & Practical Solutions */}
      <section id="challenges-and-solutions" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          7. Challenges Beginners Face &amp; Practical Solutions
        </h2>
        <p>
          Preparing for your <strong className="text-[#F1ECE2]">first open source contribution</strong> can involve common friction points. Understanding how to handle them makes the process manageable:
        </p>

        <div className="space-y-4 pt-2">
          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F06522]">Challenge 1: Codebase Size and Complexity</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              <strong>Practical Solution:</strong> You do not need to understand an entire repository to make a valuable contribution. Focus specifically on the file, module, or test related to your chosen issue. Use codebase search to locate relevant functions and trace only the execution path involved in the task.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F4C13D]">Challenge 2: Setting Up the Local Development Environment</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              <strong>Practical Solution:</strong> Follow the project's <code className="text-[#F4C13D]">CONTRIBUTING.md</code> and <code className="text-[#F4C13D]">README.md</code> step by step. If a project offers containerized environments (such as Docker or VS Code devcontainers), use them to avoid local dependency mismatches. When a build error occurs, search closed issues in the repository—often another developer has documented the solution.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#5B86F2]">Challenge 3: Selecting an Appropriate Issue</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              <strong>Practical Solution:</strong> Start with issues specifically tagged with labels like <code className="text-[#F4C13D]">good first issue</code>, <code className="text-[#F4C13D]">documentation</code>, or <code className="text-[#F4C13D]">help wanted</code>. Avoid taking on large architectural refactorings or complex concurrency issues on your initial contribution.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F29BB8]">Challenge 4: Receiving Review Feedback</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              <strong>Practical Solution:</strong> Remember that code review is a collaborative discussion aimed at maintaining code quality, not a personal critique. Maintainers appreciate contributors who communicate courteously, ask clarifying questions when feedback is unclear, and push updates in response to suggestions.
            </p>
          </div>
        </div>
      </section>

      {/* 8. How to Start Contributing: Common Contribution Workflow */}
      <section id="how-to-start-contributing" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          8. How to Start Contributing: Common Contribution Workflow
        </h2>
        <p>
          If you are wondering <strong className="text-[#F1ECE2]">how to contribute to open source</strong>, following a structured workflow helps keep your work organized. Note that specific contribution instructions, branching standards, and review processes differ between projects, so always consult the repository's guidelines:
        </p>

        <ol className="space-y-3 pt-2 font-mono text-xs md:text-sm">
          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">01.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Understand the Project and Its Purpose</strong>
              <span className="text-[#A7A29A]">
                Familiarize yourself with what the software does, who uses it, and how it is structured. Choosing a technology or programming language you already use reduces onboarding friction.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">02.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Read README.md and CONTRIBUTING.md</strong>
              <span className="text-[#A7A29A]">
                Review the project's documentation to understand local environment setup commands, testing requirements, code formatting conventions, and commit style guidelines.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">03.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Check Existing Issues and Discussions</strong>
              <span className="text-[#A7A29A]">
                Search open and closed issues to ensure the topic you want to address has not already been solved, rejected by maintainers, or handled in an active pull request.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">04.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Select an Issue Appropriate to Your Experience</strong>
              <span className="text-[#A7A29A]">
                Begin with clearly defined tasks such as small bug fixes, documentation clarifications, or test coverage additions.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">05.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Understand Maintainer Expectations</strong>
              <span className="text-[#A7A29A]">
                Check whether the project requires issues to be assigned before opening a pull request. If appropriate, leave a polite comment expressing your interest and outlining your intended approach.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">06.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Fork or Clone the Repository as Appropriate</strong>
              <span className="text-[#A7A29A]">
                Most open-source projects on GitHub use a fork-and-pull-request model. Fork the repository to your account, clone your fork locally, and configure the upstream remote:
              </span>
              <div className="bg-[#0D0D0D] p-2 mt-2 border border-[#2E2E2E] text-[#F4C13D] font-mono text-xs">
                git clone https://github.com/your-username/repository-name.git
              </div>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">07.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Create a Focused Branch and Make a Small Change</strong>
              <span className="text-[#A7A29A]">
                Keep changes isolated on a descriptive branch. Keep your edits concise and focused strictly on the problem at hand, avoiding unrelated stylistic changes:
              </span>
              <div className="bg-[#0D0D0D] p-2 mt-2 border border-[#2E2E2E] text-[#F4C13D] font-mono text-xs">
                git checkout -b fix/issue-123-null-check
              </div>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">08.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Test Your Changes Locally</strong>
              <span className="text-[#A7A29A]">
                Run the project's test suite, linters, and type checkers. Verify that existing tests continue to pass and add a new test covering your fix or feature where applicable.
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">09.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Submit a Clear Pull Request</strong>
              <span className="text-[#A7A29A]">
                Push your branch and open a pull request against the upstream project. Fill out the project's PR template, describe what changes were made, explain how to test them, and reference the issue (e.g. <code className="text-[#F4C13D]">Closes #123</code>).
              </span>
            </div>
          </li>

          <li className="p-4 border border-[#2E2E2E] bg-[#141414] flex gap-3">
            <span className="text-[#F06522] font-bold text-base">10.</span>
            <div>
              <strong className="text-[#F1ECE2] block mb-1">Respond Constructively to Review Feedback</strong>
              <span className="text-[#A7A29A]">
                Reviewers may ask questions or suggest refinements. Push updates to your branch—the pull request updates automatically. Thank maintainers for their time once the review concludes.
              </span>
            </div>
          </li>
        </ol>
      </section>

      {/* 9. Where to Find Open-Source Opportunities */}
      <section id="where-to-find-opportunities" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          9. Where to Find Open-Source Opportunities
        </h2>
        <p>
          Finding relevant <strong className="text-[#F1ECE2]">GitHub issues for beginners</strong> can be done through several channels:
        </p>

        <ul className="list-disc pl-6 space-y-2 text-xs md:text-sm">
          <li>
            <strong>GitHub Issue Search:</strong> Search GitHub issues directly using specific label qualifiers (e.g. <code className="text-[#F4C13D]">is:open is:issue label:"good first issue" language:javascript</code>).
          </li>
          <li>
            <strong>GitHub Explore:</strong> Discover active repositories and curated topics through{' '}
            <a
              href="https://github.com/explore"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              GitHub Explore <ExternalLink size={13} />
            </a>
            .
          </li>
          <li>
            <strong>First Contributions:</strong> Practice the complete Git contribution process in a welcoming sandbox repository at{' '}
            <a
              href="https://github.com/firstcontributions/first-contributions"
              target="_blank"
              rel="noopener noreferrer"
              className="text-[#F4C13D] underline hover:text-[#ffd666] inline-flex items-center gap-1"
            >
              First Contributions <ExternalLink size={13} />
            </a>
            .
          </li>
          <li>
            <strong>Community Programs:</strong> Programs like Google Summer of Code (GSoC), Hacktoberfest, and the Linux Foundation Mentorship program offer structured environments for open-source learning.
          </li>
        </ul>

        {/* Introduction of OpenSourceContributor */}
        <div className="border border-[#2E2E2E] bg-[#161616] p-6 space-y-4 border-l-4 border-l-[#F06522] mt-4">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-[#F06522]" />
            <h3 className="font-heading text-xl text-[#F1ECE2]">
              Exploring Issues with OpenSourceContributor
            </h3>
          </div>
          <p className="text-xs md:text-sm text-[#A7A29A] leading-relaxed">
            To streamline issue discovery, we developed <strong className="text-[#F1ECE2]">OpenSourceContributor</strong>. The platform provides tools to search, organize, and manage your open-source journey:
          </p>
          <ul className="list-disc pl-5 space-y-1.5 text-xs md:text-sm text-[#D0CBC1]">
            <li>
              <strong>GitHub Issue Discovery:</strong> Helps users search and browse public open-source issues across various programming languages and topics retrieved via GitHub APIs.
            </li>
            <li>
              <strong>AI-Powered Search:</strong> Helps interpret natural-language issue search requests (e.g., describing your tech stack and interests) by translating them into structured GitHub search parameters.
            </li>
            <li>
              <strong>Difficulty Labels as Guidance:</strong> Categorizes issues into difficulty tiers (<code className="text-[#F4C13D]">Good First Issue</code>, <code className="text-[#5B86F2]">Intermediate</code>, and <code className="text-[#F06522]">Advanced</code>) as heuristic guidance to help filter opportunities, while noting they are navigational estimates rather than guarantees of task complexity.
            </li>
            <li>
              <strong>Crate (Saved Issues):</strong> Allows signed-in developers to save and organize interesting issues in a personal crate for later review.
            </li>
            <li>
              <strong>Contribution Tracking:</strong> Provides a personal workspace ledger where contributors can manually record and track their progress—logging status values like <em>Planning</em>, <em>In Progress</em>, <em>PR Opened</em>, or <em>Completed</em>, along with branch names, pull request links, and personal notes.
            </li>
          </ul>
          <div className="pt-2">
            <Link
              to={ROUTES.EXPLORE}
              className="inline-flex items-center gap-2 px-4 py-2 bg-[#F06522] text-[#0D0D0D] font-mono text-xs font-bold uppercase tracking-wider hover:bg-[#F4C13D] transition-colors"
            >
              BROWSE OPEN ISSUES ON EXPLORE <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </section>

      {/* 10. Conclusion */}
      <section id="conclusion" className="space-y-4">
        <h2 className="font-heading text-2xl md:text-3xl text-[#F1ECE2] uppercase tracking-tight pt-2 border-b border-[#2E2E2E] pb-3">
          10. Conclusion
        </h2>
        <p>
          Open-source software remains one of the most effective collaborative models in modern technology. It gives developers the opportunity to examine production code, learn from real-world software architecture, and contribute back to tools utilized by the broader engineering community.
        </p>
        <p>
          Getting started does not require solving an architectural overhaul on your first day. You can begin with a manageable step: clarifying a paragraph in documentation, submitting a reproducible bug report, or adding an automated test case. Small, consistent contributions help build practical experience, strengthen teamwork skills, and advance the shared software ecosystem.
        </p>
      </section>

      {/* 11. FAQ Section */}
      <section id="faq" className="space-y-6 pt-4">
        <div className="border-t-2 border-[#2E2E2E] pt-8">
          <div className="flex items-center gap-2 text-xs font-mono text-[#F4C13D] uppercase tracking-widest font-bold mb-2">
            <HelpCircle size={15} />
            <span>KNOWLEDGE ARCHIVE</span>
          </div>
          <h2 className="font-heading text-2xl md:text-4xl text-[#F1ECE2] uppercase tracking-tight">
            Frequently Asked Questions
          </h2>
          <p className="text-xs md:text-sm text-[#A7A29A] font-mono mt-1">
            Common questions regarding open-source contribution, licensing, workflows, and developer benefits.
          </p>
        </div>

        <div className="space-y-4">
          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">1. What is an open-source contribution?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              An open-source contribution is any work shared with an open-source project to maintain, improve, or support it. Contributions include writing or reviewing code, improving documentation, writing automated tests, submitting reproducible bug reports, providing design feedback, answering community questions, and triaging issues.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">2. Why is open source important in software development?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Open source enables global collaboration, reduces redundant engineering effort, facilitates code inspection and transparency, and allows developers everywhere to study and build upon battle-tested software foundations.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">3. Can beginners contribute to open-source projects?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Yes. Many repositories explicitly maintain issues labeled <code className="text-[#F4C13D]">good first issue</code>, where maintainers provide guidance to help new contributors understand the project setup and pull request review process.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">4. Is open-source contribution useful for college students?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Yes. Working on real projects helps students apply theoretical classroom concepts to production codebases, practice version control workflows, and gain concrete examples of problem-solving to discuss during technical interviews.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">5. Do I need advanced programming skills?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              No. Beginners can contribute through documentation corrections, test additions, bug reproduction, or small bug fixes that require only foundational programming and Git knowledge.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">6. Can I contribute without writing code?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Yes. Documentation editing, reproducible bug reports, design suggestions, accessibility reviews, translation, and issue triage are non-code contributions that project maintainers value.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">7. How do I find beginner-friendly GitHub issues?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              You can search GitHub using labels like <code className="text-[#F4C13D]">good first issue</code>, explore curated resources like First Contributions, or use discovery tools like OpenSourceContributor to filter issues by language, topic, and difficulty labels.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">8. Does open-source contribution help with placements?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Contributing helps demonstrate initiative, practical Git skills, and collaborative communication through verifiable code examples. While it does not guarantee a job or internship, it provides concrete talking points during technical interviews.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">9. Which programming languages can I use?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              You can contribute in any programming language used across open-source projects, including JavaScript, TypeScript, Python, Java, Go, Rust, C++, C#, Kotlin, and others. Choosing a language you already know or are learning helps you focus on the contribution process.
            </p>
          </div>

          <div className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-2">
            <h3 className="font-heading text-lg text-[#F1ECE2]">10. How do I make my first pull request?</h3>
            <p className="text-xs md:text-sm text-[#A7A29A]">
              Fork the repository on GitHub, clone your fork to your computer, create a new branch, make your change, test it locally, push the branch to your fork, and click <em>New Pull Request</em> on GitHub with a concise summary linking to the issue.
            </p>
          </div>
        </div>
      </section>
    </article>
  );
}
