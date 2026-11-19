import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { usePageSEO } from '../hooks/usePageSEO';
import { Button } from '../components/ui/Button';
import {
  Terminal,
  Shield,
  Zap,
  Check,
  ArrowRight,
  Layers,
  Cpu,
  Search,
  Sparkles,
  ExternalLink,
  Code2,
  CheckCircle2,
  AlertTriangle,
  FolderGit2,
  Sliders,
  Filter,
  CheckSquare,
  FileCode,
  Radar,
  Lock,
  Compass,
} from 'lucide-react';

export function HowItWorksPage() {
  usePageSEO({
    title: 'How to Contribute to Open Source for Beginners',
    description:
      'Learn how to contribute to open source and find your first GitHub issues. A complete beginner guide to discovering good first issues and opening pull requests.',
    canonicalPath: ROUTES.HOW_IT_WORKS,
  });
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState('01');
  const [customPrompt, setCustomPrompt] = useState(
    'I know TypeScript, React, and Tailwind. Find good-first-issues in web dev tools with > 500 stars.'
  );

  // Example presets across various technologies
  const EXAMPLE_PRESETS = [
    {
      title: 'TypeScript & Web Tools',
      prompt: 'I know TypeScript, React, and Tailwind. Find beginner issues in developer tooling with 500+ stars.',
      parsed: {
        languages: ['TypeScript', 'JavaScript'],
        frameworks: ['React', 'Tailwind'],
        difficulty: 'Good First Issue',
        minStars: 500,
        expectedDiff: '< 40 lines (estimate)',
      },
    },
    {
      title: 'Rust & Systems CLI',
      prompt: 'Beginner-friendly Rust issues in CLI tools or build caching engines with isolated repro steps.',
      parsed: {
        languages: ['Rust'],
        frameworks: ['Cargo', 'CLI'],
        difficulty: 'Good First Issue',
        minStars: 1000,
        expectedDiff: '< 50 lines (estimate)',
      },
    },
    {
      title: 'Java & Spring Boot',
      prompt: 'Java 21 Spring Boot and Kafka backend opportunities with active maintainers and reproducible tests.',
      parsed: {
        languages: ['Java'],
        frameworks: ['Spring Boot', 'Kafka'],
        difficulty: 'Intermediate',
        minStars: 2000,
        expectedDiff: '~40 lines (estimate)',
      },
    },
    {
      title: 'Python & Data Validation',
      prompt: 'Python validation or documentation issues in modern web or data libraries with pytest suites.',
      parsed: {
        languages: ['Python'],
        frameworks: ['Pydantic', 'FastAPI'],
        difficulty: 'Good First Issue',
        minStars: 1500,
        expectedDiff: '< 30 lines (estimate)',
      },
    },
    {
      title: 'Go & Cloud Native',
      prompt: 'Go concurrency or API server bugs with single call sites and clean regression tests.',
      parsed: {
        languages: ['Go'],
        frameworks: ['Standard Library', 'HTTP'],
        difficulty: 'Intermediate',
        minStars: 5000,
        expectedDiff: '~60 lines (estimate)',
      },
    },
  ];

  const [activePresetIndex, setActivePresetIndex] = useState(0);
  const currentPreset = EXAMPLE_PRESETS[activePresetIndex];

  const handleLaunchSearch = (promptText) => {
    navigate(`${ROUTES.EXPLORE}?q=${encodeURIComponent(promptText)}`);
  };

  // Factual, defensible capability comparison data
  const COMPARISON_ROWS = [
    {
      feature: 'Query Input Style',
      osc: 'Natural language prompt parsing into structured search qualifiers',
      traditional: 'Static keyword or label browsing (e.g. #good-first-issue)',
      github: 'Free-text search supporting manual qualifier syntax',
    },
    {
      feature: 'Issue Context & Detail',
      osc: 'Curated dossier view with estimated diff, extracted criteria, and affected file references',
      traditional: 'Basic summary cards linking directly to GitHub issue thread',
      github: 'Raw repository thread with mixed comments, PR links, and discussion',
    },
    {
      feature: 'Workflow & Organization',
      osc: 'Personal crate bookmarking and user-managed contribution ledger (Planning to Completed)',
      traditional: 'Typically read-only directory listings without personal workflow tracking',
      github: 'Repository starring, notification subscriptions, and organization projects',
    },
    {
      feature: 'Technology Breadth',
      osc: 'Agnostic indexing across runtimes (TypeScript, Rust, Java, Python, Go, React)',
      traditional: 'Often segmented by community or language-specific portals',
      github: 'Universal search across all public GitHub repositories',
    },
    {
      feature: 'Current Data Source',
      osc: 'Live GitHub REST API with verified repository languages and Spring AI discovery',
      traditional: 'Live external crawls or scheduled batch database updates',
      github: 'Live canonical GitHub database',
    },
  ];

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 1. TOP TICKER STRIP */}
      <section className="w-full bg-[#F4C13D] text-[#0D0D0D] px-4 md:px-8 py-1.5 flex items-center justify-between overflow-hidden text-xs font-mono font-bold uppercase tracking-wider select-none border-b border-[#2E2E2E]">
        <div className="flex items-center space-x-3 whitespace-nowrap truncate">
          <span className="inline-flex items-center space-x-1.5 bg-[#0D0D0D] text-[#F4C13D] px-2 py-0.5 text-[10px]">
            <span className="w-1.5 h-1.5 bg-[#F4C13D] inline-block" />
            <span>DISCOVERY ARCHITECTURE // GUIDE</span>
          </span>
          <span className="hidden md:inline text-[11px] opacity-90">·</span>
          <span className="hidden md:inline text-[11px] opacity-90">
            NATURAL LANGUAGE SEMANTIC SEARCH
          </span>
          <span className="hidden md:inline text-[11px] opacity-90">·</span>
          <span className="hidden md:inline text-[11px] opacity-90">
            GITHUB REST API INTEGRATION
          </span>
        </div>
        <div className="font-mono text-xs font-bold uppercase tracking-wider hidden sm:block shrink-0 ml-4">
          LIVE GITHUB REST API ENGINE
        </div>
      </section>

      {/* ============================================================== */}
      {/* 2. PAGE HERO SECTION                                           */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] bg-[#0E0E0E] px-4 md:px-8 py-12 md:py-20">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">
          {/* Left Column (8 cols): Anton Headline & Subhead */}
          <div className="lg:col-span-8 space-y-4">
            <div className="inline-flex items-center space-x-2 font-mono text-xs text-[#F4C13D]">
              <span className="w-2.5 h-2.5 bg-[#F06522] inline-block" />
              <span className="uppercase tracking-widest font-bold">
                BEGINNER OPEN SOURCE CONTRIBUTION GUIDE
              </span>
            </div>

            <h1 className="font-heading text-4xl sm:text-5xl md:text-6xl lg:text-7xl uppercase tracking-tight text-[#F1ECE2] leading-[0.9]">
              HOW THE DISCOVERY<br />
              <span className="text-[#F06522]">ENGINE</span> WORKS.
            </h1>

            <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed max-w-2xl">
              Learn how to contribute to open source software and make your first open source contribution with confidence. Discover how our engine helps beginners find active GitHub issues, evaluate codebases, and open successful pull requests.
            </p>
          </div>

          {/* Right Column (4 cols): Quick Protocol Card */}
          <div className="lg:col-span-4 bg-[#141414] border border-[#2E2E2E] p-6 space-y-4 font-mono text-xs">
            <div className="border-b border-[#2E2E2E] pb-2 flex items-center justify-between">
              <span className="text-[#F4C13D] font-bold uppercase tracking-widest">
                CORE WORKFLOW
              </span>
              <span className="text-[#666666]">03 STEPS</span>
            </div>
            <p className="text-[#A7A29A] leading-relaxed">
              From natural-language skill description to verified GitHub Pull Request submission. Transparent separation of verified repository data, AI heuristics, and user workspaces.
            </p>
            <div className="pt-2">
              <Button
                variant="primary"
                size="md"
                onClick={() => handleLaunchSearch(customPrompt)}
                className="w-full flex items-center justify-center gap-2"
              >
                <span>TRY LIVE IN EXPLORE</span>
                <ArrowRight size={14} />
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 3. THREE-STEP WORKFLOW (01 DESCRIBE / 02 DISCOVER / 03 CONTRIBUTE) */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-16 max-w-7xl mx-auto w-full space-y-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b border-[#2E2E2E] pb-4">
          <div>
            <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block mb-1">
              WORKFLOW BLUEPRINT
            </span>
            <h2 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase tracking-tight">
              THREE-STEP WORKFLOW
            </h2>
          </div>

          {/* Step Selector Tabs */}
          <div className="flex flex-wrap items-center gap-1.5 font-mono text-xs">
            <button
              type="button"
              onClick={() => setActiveStep('01')}
              className={`px-3 py-1 font-bold uppercase border transition-colors ${
                activeStep === '01'
                  ? 'bg-[#F06522] text-[#0D0D0D] border-[#F06522]'
                  : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
              }`}
            >
              01 — DESCRIBE
            </button>
            <button
              type="button"
              onClick={() => setActiveStep('02')}
              className={`px-3 py-1 font-bold uppercase border transition-colors ${
                activeStep === '02'
                  ? 'bg-[#F4C13D] text-[#0D0D0D] border-[#F4C13D]'
                  : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
              }`}
            >
              02 — DISCOVER
            </button>
            <button
              type="button"
              onClick={() => setActiveStep('03')}
              className={`px-3 py-1 font-bold uppercase border transition-colors ${
                activeStep === '03'
                  ? 'bg-[#5B86F2] text-[#0D0D0D] border-[#5B86F2]'
                  : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
              }`}
            >
              03 — CONTRIBUTE
            </button>
            <button
              type="button"
              onClick={() => setActiveStep('all')}
              className={`px-3 py-1 font-bold uppercase border transition-colors ${
                activeStep === 'all'
                  ? 'bg-[#F1ECE2] text-[#0D0D0D] border-[#F1ECE2]'
                  : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
              }`}
            >
              VIEW ALL
            </button>
          </div>
        </div>

        {/* Workflow Cards Container */}
        <div className="space-y-8">
          {/* ------------------------------------------------------------ */}
          {/* STEP 01 — DESCRIBE                                           */}
          {/* ------------------------------------------------------------ */}
          {(activeStep === '01' || activeStep === 'all') && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start border-2 border-[#2E2E2E] bg-[#141414] p-6 md:p-8">
              <div className="lg:col-span-5 space-y-4 font-mono text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-5 bg-[#F06522] shrink-0 inline-block" />
                  <span className="text-xs font-bold text-[#F06522] uppercase tracking-widest">
                    STEP 01 // INPUT LAYER
                  </span>
                </div>

                <h3 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase leading-none">
                  DESCRIBE YOUR GOAL IN NATURAL LANGUAGE
                </h3>

                <p className="text-[#A7A29A] leading-relaxed">
                  Instead of forcing developers to hunt through fragmented GitHub issue labels (e.g. <code className="text-[#F4C13D]">help wanted</code>, <code className="text-[#F06522]">good-first-issue</code>, <code className="text-[#5B86F2]">documentation</code>), our interface accepts free-form intent.
                </p>

                <p className="text-[#A7A29A] leading-relaxed">
                  State what you know (e.g. <em>"TypeScript, Node.js"</em>), what you want to learn (e.g. <em>"Rust systems tooling"</em>), your difficulty preference, and desired task scope.
                </p>

                <div className="p-3 bg-[#0E0E0E] border border-[#2E2E2E] space-y-2 text-[#F06522]">
                  <span className="text-[#666666] block text-[11px]">&gt; EXAMPLE QUERY INPUT:</span>
                  <p className="text-xs leading-relaxed text-[#F1ECE2]">
                    "{currentPreset.prompt}"
                  </p>
                  <button
                    type="button"
                    onClick={() => handleLaunchSearch(currentPreset.prompt)}
                    className="inline-flex items-center gap-1.5 text-[11px] text-[#F4C13D] hover:underline font-bold uppercase pt-1"
                  >
                    <span>SEARCH THIS IN EXPLORE</span>
                    <ArrowRight size={12} />
                  </button>
                </div>
              </div>

              {/* Right Side: Visual Schema Breakdown */}
              <div className="lg:col-span-7 bg-[#0E0E0E] border border-[#2E2E2E] p-6 font-mono text-xs space-y-3">
                <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-2 text-[11px] text-[#A7A29A]">
                  <span className="flex items-center gap-1.5 text-[#F4C13D] font-bold">
                    <Sparkles size={14} />
                    <span>SEMANTIC INTAKE PARSER</span>
                  </span>
                  <span className="text-[#666666]">MODEL: SPRING AI / GROQ</span>
                </div>

                <div className="space-y-1.5 text-[11px]">
                  <div className="text-[#A7A29A]">&gt; INTAKE STRING: <span className="text-[#F1ECE2]">"{currentPreset.prompt}"</span></div>
                  <div className="text-[#2E7D32]">&gt; EXTRACTED PRIMARY LANGUAGES: <span className="text-[#F1ECE2]">{currentPreset.parsed.languages.join(', ')}</span></div>
                  <div className="text-[#2E7D32]">&gt; FRAMEWORKS &amp; KEYWORDS: <span className="text-[#F1ECE2]">{currentPreset.parsed.frameworks.join(', ')}</span></div>
                  <div className="text-[#5B86F2]">&gt; DIFFICULTY CEILING: <span className="text-[#F1ECE2]">{currentPreset.parsed.difficulty}</span></div>
                  <div className="text-[#F4C13D]">&gt; MINIMUM REPOSITORY STARS: <span className="text-[#F1ECE2]">&gt; {currentPreset.parsed.minStars}</span></div>
                  <div className="text-[#A7A29A]">&gt; PARSED TARGET DIFF LIMIT: <span className="text-[#F1ECE2]">{currentPreset.parsed.expectedDiff}</span></div>
                </div>

                <div className="pt-2 border-t border-[#1F1F1F] flex items-center justify-between text-[11px]">
                  <span className="text-[#666666]">VALIDATION: STRICT JSON SCHEMA</span>
                  <span className="text-[#F4C13D] font-bold">PARSED SCHEMA</span>
                </div>
              </div>
            </div>
          )}

          {/* ------------------------------------------------------------ */}
          {/* STEP 02 — DISCOVER                                           */}
          {/* ------------------------------------------------------------ */}
          {(activeStep === '02' || activeStep === 'all') && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start border-2 border-[#2E2E2E] bg-[#141414] p-6 md:p-8">
              <div className="lg:col-span-5 space-y-4 font-mono text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-5 bg-[#F4C13D] shrink-0 inline-block" />
                  <span className="text-xs font-bold text-[#F4C13D] uppercase tracking-widest">
                    STEP 02 // DISCOVERY &amp; SANITIZATION
                  </span>
                </div>

                <h3 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase leading-none">
                  INTELLIGENT SANITIZATION HEURISTICS
                </h3>

                <p className="text-[#A7A29A] leading-relaxed">
                  Searching raw GitHub returns significant noise: inactive tickets from years past, issues stalled on unresolved architectural debates, or issues with competing unmerged pull requests.
                </p>

                <p className="text-[#A7A29A] leading-relaxed">
                  OpenSourceContributor applies objective heuristic filters to candidate issues returned by the GitHub API:
                </p>

                <ul className="space-y-1.5 text-[#A7A29A] list-disc list-inside">
                  <li><strong>Activity Threshold:</strong> Filter repositories without commit activity in the last 90 days.</li>
                  <li><strong>Pull Request Status:</strong> Filter issues that already have open, active pull requests.</li>
                  <li><strong>Maintainer SLA:</strong> Evaluate recent review turnaround time on merged PRs.</li>
                  <li><strong>Documentation Context:</strong> Prioritize issues with clear reproduction steps or target file references.</li>
                </ul>
              </div>

              {/* Right Side: Sanitization Matrix */}
              <div className="lg:col-span-7 bg-[#0E0E0E] border border-[#2E2E2E] p-6 font-mono text-xs space-y-3">
                <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-2 text-[11px] text-[#A7A29A]">
                  <span className="flex items-center gap-1.5 text-[#F06522] font-bold">
                    <Shield size={14} />
                    <span>HEURISTIC FILTERING ENGINE</span>
                  </span>
                  <span className="text-[#666666]">AUTOMATED SCREENING</span>
                </div>

                <div className="space-y-2">
                  <div className="p-2.5 bg-[#1F1414] border border-[#7F1D1D] flex items-center justify-between text-[11px]">
                    <div className="space-y-0.5">
                      <span className="text-[#FF8A80] font-bold block">FILTER: INACTIVE REPOSITORY</span>
                      <span className="text-[#A7A29A]">No repository commit activity in 180+ days.</span>
                    </div>
                    <span className="text-[#FF5252] font-bold px-2 py-0.5 bg-[#3B1212]">REJECT</span>
                  </div>

                  <div className="p-2.5 bg-[#1F1414] border border-[#7F1D1D] flex items-center justify-between text-[11px]">
                    <div className="space-y-0.5">
                      <span className="text-[#FF8A80] font-bold block">FILTER: COMPETING PULL REQUEST</span>
                      <span className="text-[#A7A29A]">Existing open PR already addresses this issue.</span>
                    </div>
                    <span className="text-[#FF5252] font-bold px-2 py-0.5 bg-[#3B1212]">REJECT</span>
                  </div>

                  <div className="p-2.5 bg-[#122415] border border-[#2E7D32] flex items-center justify-between text-[11px]">
                    <div className="space-y-0.5">
                      <span className="text-[#A5D6A7] font-bold block">PASS: ACTIVE MAINTAINER ACTIVITY</span>
                      <span className="text-[#A7A29A]">Active test suite, recent commits, no unmerged competing PR.</span>
                    </div>
                    <span className="text-[#2E7D32] font-bold px-2 py-0.5 bg-[#1C3320]">PASS</span>
                  </div>
                </div>

                <div className="pt-2 border-t border-[#1F1F1F] flex items-center justify-between text-[11px] text-[#666666]">
                  <span>NOTE: HEURISTICS RUN ON BACKEND DISCOVERY SERVICE</span>
                  <span>CURRENT: PRODUCTION ENGINE</span>
                </div>
              </div>
            </div>
          )}

          {/* ------------------------------------------------------------ */}
          {/* STEP 03 — CONTRIBUTE                                         */}
          {/* ------------------------------------------------------------ */}
          {(activeStep === '03' || activeStep === 'all') && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start border-2 border-[#2E2E2E] bg-[#141414] p-6 md:p-8">
              <div className="lg:col-span-5 space-y-4 font-mono text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-5 bg-[#5B86F2] shrink-0 inline-block" />
                  <span className="text-xs font-bold text-[#5B86F2] uppercase tracking-widest">
                    STEP 03 // ACTION LAYER
                  </span>
                </div>

                <h3 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase leading-none">
                  INSPECT, CRATE &amp; TRACK YOUR CONTRIBUTION
                </h3>

                <p className="text-[#A7A29A] leading-relaxed">
                  When you select a candidate opportunity, you receive structured engineering specifications to help evaluate scope before cloning the repository:
                </p>

                <ul className="space-y-1.5 text-[#A7A29A] list-disc list-inside">
                  <li><strong>Target File Path:</strong> Identified source file reference.</li>
                  <li><strong>Expected Diff:</strong> Estimated line changes (e.g. &lt; 40 lines).</li>
                  <li><strong>Acceptance Criteria:</strong> Extracted checklist of requirements from the issue description.</li>
                  <li><strong>Crate Persistence:</strong> Save to your personal crate or track pull request status.</li>
                </ul>

                <div className="pt-2">
                  <Link to={ROUTES.EXPLORE}>
                    <Button variant="secondary" size="md" className="w-full">
                      BROWSE CURRENT CRATE CATALOG
                    </Button>
                  </Link>
                </div>
              </div>

              {/* Right Side: Illustrative Dossier */}
              <div className="lg:col-span-7 bg-[#0E0E0E] border border-[#2E2E2E] p-6 font-mono text-xs space-y-3">
                <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-2 text-[11px] text-[#A7A29A]">
                  <span className="flex items-center gap-1.5 text-[#5B86F2] font-bold">
                    <FileCode size={14} />
                    <span>ILLUSTRATIVE EXAMPLE DOSSIER</span>
                  </span>
                  <span className="text-[#A7A29A] font-bold">[FORMAT SPEC]</span>
                </div>

                <div className="bg-[#141414] border border-[#2E2E2E] p-4 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-[#F06522] font-bold">trpc/trpc #5412</span>
                    <span className="bg-[#F4C13D] text-[#0D0D0D] px-2 py-0.5 text-[10px] font-bold uppercase">
                      GOOD FIRST ISSUE
                    </span>
                  </div>
                  <h4 className="font-heading text-lg text-[#F1ECE2] uppercase leading-snug">
                    Support custom serializer options in httpBatchLink
                  </h4>
                  <p className="text-[#A7A29A] text-xs">
                    Target: <code className="text-[#5B86F2]">packages/client/src/links/httpBatchLink.ts</code>
                  </p>
                  <div className="flex flex-wrap items-center gap-3 pt-2 text-[11px] text-[#A7A29A]">
                    <span className="text-[#F4C13D]">DIFF: &lt; 40 LINES (ESTIMATE)</span>
                    <span>·</span>
                    <span className="text-[#2E7D32]">MAINTAINER SLA: &lt; 24H (ESTIMATE)</span>
                    <span>·</span>
                    <span className="text-[#5B86F2]">MATCH SCORE: 96%</span>
                  </div>
                </div>

                <div className="pt-2 flex items-center justify-between text-[11px] text-[#666666]">
                  <span>ILLUSTRATIVE FORMAT ONLY</span>
                  <span>DISCOVER REAL OPPORTUNITIES VIA EXPLORE</span>
                </div>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* ============================================================== */}
      {/* 4. INTERACTIVE TECHNOLOGY PRESETS                              */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-16 max-w-7xl mx-auto w-full space-y-8">
        <div className="space-y-2 max-w-2xl">
          <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block">
            TECHNOLOGY AGNOSTIC SPECIFICATIONS
          </span>
          <h2 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase tracking-tight">
            EXAMPLE QUERIES ACROSS RUNTIMES
          </h2>
          <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed">
            Our pipeline is designed for all modern open-source stacks. Select a runtime below to see how natural queries map into structured parameters:
          </p>
        </div>

        {/* Runtime Preset Buttons */}
        <div className="flex flex-wrap items-center gap-2 font-mono text-xs">
          {EXAMPLE_PRESETS.map((preset, idx) => (
            <button
              key={preset.title}
              type="button"
              onClick={() => {
                setActivePresetIndex(idx);
                setCustomPrompt(preset.prompt);
              }}
              className={`px-3 py-2 font-bold uppercase border transition-colors ${
                activePresetIndex === idx
                  ? 'bg-[#F06522] text-[#0D0D0D] border-[#F06522]'
                  : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:border-[#F4C13D] hover:text-[#F1ECE2]'
              }`}
            >
              {preset.title}
            </button>
          ))}
        </div>

        {/* Interactive Query Box & Action */}
        <div className="bg-[#141414] border-2 border-[#2E2E2E] p-6 space-y-6 font-mono text-xs">
          <div className="space-y-2">
            <span className="text-[#666666] uppercase text-[11px]">ACTIVE NATURAL LANGUAGE PROMPT:</span>
            <div className="p-3 bg-[#0E0E0E] border border-[#2E2E2E] text-[#F1ECE2] text-sm leading-relaxed">
              &gt; {currentPreset.prompt}
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="p-3 bg-[#1A1A1A] border border-[#2E2E2E] space-y-1">
              <span className="text-[10px] text-[#666666] uppercase">PRIMARY RUNTIME</span>
              <span className="text-sm font-bold text-[#F4C13D] block truncate">
                {currentPreset.parsed.languages[0]}
              </span>
            </div>
            <div className="p-3 bg-[#1A1A1A] border border-[#2E2E2E] space-y-1">
              <span className="text-[10px] text-[#666666] uppercase">TARGET DIFFICULTY</span>
              <span className="text-sm font-bold text-[#F06522] block truncate">
                {currentPreset.parsed.difficulty}
              </span>
            </div>
            <div className="p-3 bg-[#1A1A1A] border border-[#2E2E2E] space-y-1">
              <span className="text-[10px] text-[#666666] uppercase">MINIMUM STARS</span>
              <span className="text-sm font-bold text-[#5B86F2] block truncate">
                &gt; {currentPreset.parsed.minStars} ★
              </span>
            </div>
            <div className="p-3 bg-[#1A1A1A] border border-[#2E2E2E] space-y-1">
              <span className="text-[10px] text-[#666666] uppercase">DIFF ESTIMATE</span>
              <span className="text-sm font-bold text-[#2E7D32] block truncate">
                {currentPreset.parsed.expectedDiff}
              </span>
            </div>
          </div>

          <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-4 border-t border-[#222222]">
            <span className="text-[11px] text-[#666666]">
              EXECUTE THIS QUERY DIRECTLY IN THE CRATE CATALOG VIA /EXPLORE?Q=...
            </span>
            <Button
              variant="primary"
              size="md"
              onClick={() => handleLaunchSearch(currentPreset.prompt)}
              className="w-full sm:w-auto flex items-center justify-center gap-2"
            >
              <span>RUN QUERY IN EXPLORE</span>
              <ArrowRight size={14} />
            </Button>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 5. TRANSPARENT DATA DISTINCTION                                */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-16 max-w-7xl mx-auto w-full space-y-8">
        <div className="max-w-2xl space-y-2">
          <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block">
            DATA INTEGRITY &amp; TRANSPARENCY
          </span>
          <h2 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase tracking-tight">
            DATA CLASSIFICATION &amp; VERIFICATION
          </h2>
          <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed">
            OpenSourceContributor strictly distinguishes three categories of data across the platform:
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 font-mono text-xs">
          {/* Category 1: Canonical Live GitHub API Data */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3 border-l-4 border-l-[#2E7D32]">
            <div className="flex items-center gap-2 text-[#2E7D32] font-bold uppercase">
              <CheckCircle2 size={16} />
              <span>1. CANONICAL GITHUB API DATA</span>
            </div>
            <p className="text-[#A7A29A] leading-relaxed">
              Verified facts directly from the GitHub REST API: real issue numbers, repository URLs, open/closed states, maintainer labels, and star counts. GitHub remains the exclusive canonical source of truth.
            </p>
          </div>

          {/* Category 2: AI Structural Heuristics */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3 border-l-4 border-l-[#F4C13D]">
            <div className="flex items-center gap-2 text-[#F4C13D] font-bold uppercase">
              <Radar size={16} />
              <span>2. AI INTERPRETATIONS &amp; ESTIMATES</span>
            </div>
            <p className="text-[#A7A29A] leading-relaxed">
              Algorithmic guidance generated by AI models: natural language search decomposition, estimated diff boundaries, cohesion match percentages, and parsed acceptance criteria checklists.
            </p>
          </div>

          {/* Category 3: Workspace Ledger */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-3 border-l-4 border-l-[#5B86F2]">
            <div className="flex items-center gap-2 text-[#5B86F2] font-bold uppercase">
              <Compass size={16} />
              <span>3. WORKSPACE LEDGER</span>
            </div>
            <p className="text-[#A7A29A] leading-relaxed">
              Secure persistence for saved crate issues and contribution lifecycle records.
            </p>
          </div>
        </div>

        {/* Legal Disclaimer Box */}
        <div className="p-4 bg-[#0E0E0E] border border-[#2E2E2E] font-mono text-xs text-[#666666] leading-relaxed flex items-start gap-2.5">
          <AlertTriangle size={15} className="text-[#F4C13D] shrink-0 mt-0.5" />
          <span>
            DISCLAIMER: Algorithmic analysis provides structural heuristics to help you discover and evaluate candidate issues. A high match score does not guarantee maintainer acceptance of a pull request. Always consult the destination repository's CONTRIBUTING.md before writing code.
          </span>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 6. HOW WE COMPARE MATRIX TABLE                                  */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-16 max-w-7xl mx-auto w-full space-y-8">
        <div className="max-w-2xl space-y-2">
          <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block">
            FEATURE COMPARISON
          </span>
          <h2 className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] uppercase tracking-tight">
            DISCOVERY APPROACH COMPARISON
          </h2>
          <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed">
            Factual comparison between OpenSourceContributor, traditional issue portals, and native GitHub search:
          </p>
        </div>

        <div className="border border-[#2E2E2E] bg-[#141414] overflow-x-auto">
          <table className="w-full text-left font-mono text-xs">
            <thead className="bg-[#1C1B1B] border-b border-[#2E2E2E] text-[11px] text-[#A7A29A] uppercase tracking-wider">
              <tr>
                <th className="p-4">CAPABILITY</th>
                <th className="p-4 text-[#F06522] bg-[#221713] font-bold">OPENSOURCECONTRIBUTOR</th>
                <th className="p-4">TRADITIONAL DIRECTORIES</th>
                <th className="p-4">GITHUB NATIVE SEARCH</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#2E2E2E]">
              {COMPARISON_ROWS.map((row, idx) => (
                <tr key={idx} className="hover:bg-[#1A1A1A] transition-colors">
                  <td className="p-4 font-bold text-[#F1ECE2]">{row.feature}</td>
                  <td className="p-4 font-bold text-[#F4C13D] bg-[#1E1612] leading-relaxed">
                    {row.osc}
                  </td>
                  <td className="p-4 text-[#A7A29A] leading-relaxed">{row.traditional}</td>
                  <td className="p-4 text-[#A7A29A] leading-relaxed">{row.github}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 7. FINAL CALL TO ACTION BROADSHEET                             */}
      {/* ============================================================== */}
      <section className="px-4 md:px-8 py-16 max-w-7xl mx-auto w-full">
        <div className="border-2 border-[#2E2E2E] bg-[#141414] p-8 md:p-12 flex flex-col md:flex-row items-start md:items-center justify-between gap-8">
          <div className="space-y-3 max-w-xl">
            <div className="flex items-center gap-2 font-mono text-xs text-[#F4C13D] uppercase tracking-widest font-bold">
              <span className="w-2 h-2 bg-[#F06522] inline-block" />
              <span>READY TO BEGIN?</span>
            </div>
            <h2 className="font-heading text-3xl sm:text-5xl text-[#F1ECE2] uppercase tracking-tight leading-none">
              FIND YOUR FIRST VERIFIED ISSUE.
            </h2>
            <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed">
              Explore candidate opportunities across TypeScript, Rust, Java, Python, and Go in our crate catalog.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto shrink-0 font-mono">
            <Link to={ROUTES.EXPLORE} className="w-full sm:w-auto">
              <Button variant="primary" size="lg" className="w-full flex items-center justify-center gap-2">
                <span>EXPLORE ISSUES</span>
                <ArrowRight size={16} />
              </Button>
            </Link>
            <Link to={ROUTES.LOGIN} className="w-full sm:w-auto">
              <Button variant="secondary" size="lg" className="w-full">
                SIGN IN WITH GITHUB
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
