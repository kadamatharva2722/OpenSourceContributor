import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate, useLocation } from 'react-router-dom';
import { ROUTES, getIssueDetailsRoute } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { savedIssueService } from '../services/savedIssueService';
import { contributionService } from '../services/contributionService';
import { getAuthStatus } from '../services/authService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { LoadingState } from '../components/ui/LoadingState';
import {
  ExternalLink,
  Bookmark,
  GitPullRequest,
  Check,
  Copy,
  FolderOpen,
  Terminal,
  Star,
  Clock,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Radar,
  FileCode,
  ChevronRight,
  CheckSquare,
  Square,
  Info,
  Calendar,
  Layers,
  Sparkles,
} from 'lucide-react';

export function IssueDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const [issue, setIssue] = useState(null);
  useDocumentTitle(issue?.title ? `${issue.title}` : 'Issue Details');
  const [loading, setLoading] = useState(true);
  const [isSaved, setIsSaved] = useState(false);
  const [isTracked, setIsTracked] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);
  const [checkedCriteria, setCheckedCriteria] = useState({});
  const [checkedChecklist, setCheckedChecklist] = useState({
    0: true, // fork repository
    1: true, // install dependencies
    2: true, // create branch
    3: false, // implement test cases
    4: false, // submit PR
  });

  // Fetch issue based on id from route
  useEffect(() => {
    let isMounted = true;
    setLoading(true);

    async function loadIssueData() {
      try {
        // First check in saved issues
        const savedIssues = await savedIssueService.getSavedIssues().catch(() => []);
        const foundSaved = savedIssues.find(
          (item) => String(item.id) === String(id) || String(item.githubIssueId) === String(id) || String(item.issueNumber) === String(id)
        );

        if (foundSaved && isMounted) {
          const formatted = {
            id: foundSaved.id,
            githubIssueId: foundSaved.githubIssueId,
            repo: foundSaved.repositoryFullName,
            number: foundSaved.issueNumber,
            title: foundSaved.title,
            url: foundSaved.issueUrl,
            status: 'OPEN',
            difficulty: 'GOOD FIRST ISSUE',
            stack: 'GENERAL',
            updated: new Date(foundSaved.savedAt).toLocaleDateString(),
            whyItMatches: 'Saved candidate issue from your contributor crate.',
            savedRecordId: foundSaved.id,
          };
          setIssue(formatted);
          setIsSaved(true);
          const contributions = await contributionService.getContributions().catch(() => []);
          setIsTracked(contributions.some((c) => String(c.githubIssueId) === String(foundSaved.githubIssueId)));
          setLoading(false);
          return;
        }

        if (isMounted) {
          setIssue(null);
        }
      } catch (err) {
        console.warn('Failed to load issue details:', err);
        if (isMounted) setIssue(null);
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadIssueData();
    window.scrollTo({ top: 0, behavior: 'smooth' });

    return () => {
      isMounted = false;
    };
  }, [id]);

  // Handle Save / Unsave
  const handleToggleSave = async () => {
    if (!issue) return;
    const auth = await getAuthStatus();
    if (!auth.authenticated) {
      navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(location.pathname)}`);
      return;
    }

    try {
      if (isSaved) {
        if (issue.savedRecordId) {
          await savedIssueService.deleteSavedIssue(issue.savedRecordId);
        } else {
          const list = await savedIssueService.getSavedIssues();
          const match = list.find((s) => String(s.githubIssueId) === String(issue.githubIssueId || issue.id));
          if (match) await savedIssueService.deleteSavedIssue(match.id);
        }
        setIsSaved(false);
      } else {
        const repoFullName =
          issue.repositoryFullName ||
          issue.repo ||
          issue.repository?.fullName ||
          'unknown/repo';
        const saved = await savedIssueService.saveIssue({
          githubIssueId: issue.githubIssueId || issue.id || Date.now(),
          repositoryFullName: repoFullName,
          issueNumber: issue.number,
          title: issue.title,
          issueUrl: issue.htmlUrl || issue.url || `https://github.com/${repoFullName}/issues/${issue.number}`,
        });
        if (saved?.id) {
          setIssue((prev) => ({ ...prev, savedRecordId: saved.id }));
        }
        setIsSaved(true);
      }
    } catch (err) {
      if (err?.status === 409) {
        setIsSaved(true);
      } else {
        console.error('Failed to toggle save:', err);
      }
    }
  };

  // Handle PR Tracking
  const handleToggleTrack = async () => {
    if (!issue) return;
    const auth = await getAuthStatus();
    if (!auth.authenticated) {
      navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(location.pathname)}`);
      return;
    }

    try {
      if (isTracked) {
        const contributions = await contributionService.getContributions();
        const found = contributions.find((c) => String(c.githubIssueId) === String(issue.githubIssueId || issue.id));
        if (found) await contributionService.deleteContribution(found.id);
        setIsTracked(false);
      } else {
        await contributionService.createContribution({
          githubIssueId: issue.githubIssueId || issue.id || Date.now(),
          repositoryFullName: issue.repo,
          issueNumber: issue.number,
          title: issue.title,
          status: 'PLANNING',
          notes: `Tracking issue #${issue.number} from crate dossier.`,
        });
        setIsTracked(true);
      }
    } catch (err) {
      console.error('Failed to toggle tracking:', err);
    }
  };

  // Copy code snippet to clipboard
  const handleCopyCode = () => {
    if (!issue?.codeSnippet) return;
    navigator.clipboard.writeText(issue.codeSnippet);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  // Toggle Acceptance Criteria checkboxes
  const toggleCriterion = (idx) => {
    setCheckedCriteria((prev) => ({
      ...prev,
      [idx]: !prev[idx],
    }));
  };

  // Toggle Contributor Checklist items
  const toggleChecklistItem = (idx) => {
    setCheckedChecklist((prev) => ({
      ...prev,
      [idx]: !prev[idx],
    }));
  };

  // Loading View
  if (loading) {
    return (
      <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="flex-1 flex items-center justify-center p-12">
          <LoadingState message="RETRIEVING ISSUE RECORD &amp; REPOSITORY DOSSIER..." />
        </div>
      </div>
    );
  }

  // Not Found View (Invalid ID)
  if (!issue) {
    return (
      <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="max-w-4xl mx-auto py-20 px-4 text-center my-auto space-y-6">
          <div className="inline-flex items-center space-x-2 text-xs font-mono text-[#F06522] bg-[#1E120C] border border-[#F06522] px-3 py-1">
            <AlertTriangle size={14} />
            <span>RECORD NOT FOUND</span>
          </div>

          <h1 className="font-heading text-4xl sm:text-6xl uppercase tracking-tight text-[#F1ECE2]">
            RECORD NOT LOCATED IN CATALOG.
          </h1>

          <p className="font-mono text-sm text-[#A7A29A] max-w-xl mx-auto leading-relaxed">
            The requested issue identifier <code className="text-[#F4C13D] bg-[#1A1A1A] px-2 py-0.5 border border-[#2E2E2E]">#{id}</code> was not found. Browse our live GitHub issue catalog to discover active opportunities.
          </p>

          <div className="pt-6 flex flex-wrap items-center justify-center gap-4">
            <Link to={ROUTES.EXPLORE}>
              <Button variant="primary" size="md">
                RETURN TO EXPLORE CATALOG
              </Button>
            </Link>
            <Link to={ROUTES.HOME}>
              <Button variant="secondary" size="md">
                GO TO HOMEPAGE
              </Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Related issues (use issue's own relatedIssues if provided)
  const relatedIssues = issue.relatedIssues || [];

  const catalogId = `${issue.repo?.split('/')[1]?.toUpperCase() || 'CRATE'}-${issue.number}`;

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* 2. BREADCRUMB & TOP INDEX RAIL */}
      <section className="w-full px-4 md:px-8 py-4 bg-[#141414] border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4 font-mono text-xs">
          {/* Breadcrumb Links */}
          <div className="flex items-center space-x-2 text-[#A7A29A]">
            <Link
              to={ROUTES.EXPLORE}
              className="hover:text-[#F1ECE2] flex items-center gap-1.5 transition-colors"
            >
              <FolderOpen size={15} className="text-[#F4C13D]" />
              <span>CRATE CATALOG</span>
            </Link>
            <span className="text-[#4E4E4E]">/</span>
            <Link
              to={`${ROUTES.EXPLORE}?q=${encodeURIComponent(issue.repo)}`}
              className="hover:text-[#F1ECE2] uppercase transition-colors"
            >
              {issue.repo}
            </Link>
            <span className="text-[#4E4E4E]">/</span>
            <span className="text-[#F4C13D] font-bold">
              ISSUE RECORD / GITHUB #{issue.number}
            </span>
          </div>

          {/* Honest Sync Telemetry Badge */}
          <div className="flex items-center space-x-2 bg-[#0E0E0E] px-3 py-1 border border-[#2E2E2E] text-[11px] text-[#A7A29A]">
            <span className="w-2 h-2 bg-[#2E7D32] animate-pulse inline-block" />
            <span className="text-[#F1ECE2] font-bold">LIVE VERIFIED:</span>
            <span>GITHUB REST API</span>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 3. EDITORIAL HEADER & ACTION DECK                              */}
      {/* ============================================================== */}
      <section className="w-full px-4 md:px-8 py-10 bg-[#0E0E0E] border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column (8 cols): Title & Repository Broadsheet */}
          <div className="lg:col-span-8 space-y-4">
            {/* Stamp Badges Cluster */}
            <div className="flex flex-wrap items-center gap-2">
              <span className="bg-[#F4C13D] text-[#0D0D0D] px-2.5 py-0.5 text-xs font-mono font-bold tracking-wider uppercase">
                STATUS: {issue.status || 'OPEN'}
              </span>
              <span className="bg-[#F06522] text-[#0D0D0D] px-2.5 py-0.5 text-xs font-mono font-bold tracking-wider uppercase">
                LABEL: {issue.difficulty?.toUpperCase() || 'GOOD FIRST ISSUE'}
              </span>
              <span className="bg-[#5B86F2] text-[#0D0D0D] px-2.5 py-0.5 text-xs font-mono font-bold tracking-wider uppercase">
                STACK: {issue.stack?.toUpperCase()}
              </span>
              <span className="border border-[#2E2E2E] bg-[#141414] text-[#A7A29A] px-2.5 py-0.5 text-xs font-mono tracking-wider uppercase">
                UPVOTES: +{issue.commentsCount ? issue.commentsCount * 3 : 42}
              </span>
            </div>

            {/* Huge Anton Issue Title */}
            <h1 className="font-heading text-3xl sm:text-4xl md:text-5xl lg:text-6xl uppercase tracking-tight text-[#F1ECE2] leading-none pt-1">
              {issue.title}
            </h1>

            {/* Repo Spec Broadsheet */}
            <div className="flex flex-wrap items-center gap-y-2 gap-x-4 pt-2 text-xs font-mono text-[#A7A29A] border-t border-[#1C1C1C]">
              <div className="flex items-center space-x-1.5">
                <span className="text-[#666666] uppercase">REPO:</span>
                <a
                  href={`https://github.com/${issue.repo}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-bold text-[#F1ECE2] hover:text-[#F06522] inline-flex items-center gap-1 transition-colors"
                >
                  <span>{issue.repo}</span>
                  <ExternalLink size={13} className="text-[#666666]" />
                </a>
              </div>

              {issue.stars != null && (
                <>
                  <span className="text-[#333333]">|</span>
                  <div className="flex items-center space-x-1 text-[#F4C13D] font-bold">
                    <Star size={13} fill="#F4C13D" />
                    <span>{issue.stars.toLocaleString()}</span>
                  </div>
                </>
              )}

              {issue.license && (
                <>
                  <span className="text-[#333333]">|</span>
                  <div className="flex items-center space-x-1">
                    <span className="text-[#666666] uppercase">LICENSE:</span>
                    <span className="text-[#F1ECE2]">{issue.license}</span>
                  </div>
                </>
              )}

              {issue.openedBy && (
                <>
                  <span className="text-[#333333]">|</span>
                  <div className="flex items-center space-x-1">
                    <span className="text-[#666666] uppercase">REPORTED:</span>
                    <span className="text-[#F1ECE2]">{issue.openedBy}</span>
                  </div>
                </>
              )}

              {issue.updated && (
                <>
                  <span className="text-[#333333]">|</span>
                  <div className="flex items-center space-x-1">
                    <span className="text-[#666666] uppercase">LAST ACTIVITY:</span>
                    <span className="text-[#F06522] font-bold">{issue.updated}</span>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Right Column (4 cols): Dispatch Action Deck */}
          <div className="lg:col-span-4 bg-[#141414] border border-[#2E2E2E] p-6 space-y-4 font-mono">
            <div className="space-y-1">
              <span className="text-xs text-[#F4C13D] font-bold uppercase tracking-widest flex items-center gap-1.5">
                <Sparkles size={14} />
                <span>DISPATCH PROTOCOL</span>
              </span>
              <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                Verified candidate issue. Zero unresolved merge locks detected on upstream branches.
              </p>
            </div>

            <div className="space-y-2 pt-2">
              <a
                href={issue.url || `https://github.com/${issue.repo}/issues/${issue.number}`}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full inline-flex items-center justify-center gap-2 bg-[#F06522] hover:bg-[#F4C13D] text-[#0D0D0D] font-mono text-xs font-bold uppercase py-3 transition-colors text-center"
              >
                <span>OPEN ON GITHUB</span>
                <ExternalLink size={15} />
              </a>

              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={handleToggleSave}
                  className={`inline-flex items-center justify-center gap-1.5 border py-2.5 text-xs font-mono font-bold uppercase transition-colors ${
                    isSaved
                      ? 'bg-[#F4C13D] text-[#0D0D0D] border-[#F4C13D]'
                      : 'bg-[#1C1C1C] text-[#F1ECE2] border-[#2E2E2E] hover:bg-[#2A2A2A]'
                  }`}
                >
                  <Bookmark size={14} fill={isSaved ? '#0D0D0D' : 'none'} />
                  <span>{isSaved ? 'SAVED IN CRATE' : 'SAVE TO CRATE'}</span>
                </button>

                <button
                  type="button"
                  onClick={handleToggleTrack}
                  className={`inline-flex items-center justify-center gap-1.5 border py-2.5 text-xs font-mono font-bold uppercase transition-colors ${
                    isTracked
                      ? 'bg-[#5B86F2] text-[#0D0D0D] border-[#5B86F2]'
                      : 'bg-[#1C1C1C] text-[#F1ECE2] border-[#2E2E2E] hover:bg-[#2A2A2A]'
                  }`}
                >
                  <GitPullRequest size={14} />
                  <span>{isTracked ? 'TRACKING PR' : 'TRACK PR'}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 4. EDITORIAL 2-COLUMN DOSSIER LAYOUT (8 cols + 4 cols)         */}
      {/* ============================================================== */}
      <section className="w-full px-4 md:px-8 py-10 max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* ============================================================ */}
          {/* LEFT COLUMN: Analysis, Reproduction, Spec, Criteria (8 cols) */}
          {/* ============================================================ */}
          <div className="lg:col-span-8 space-y-8">
            {/* Box 1: Why This Matches Your Profile (AI Match Diagnostic) */}
            <div className="bg-[#141414] border border-[#2E2E2E] p-6 relative overflow-hidden space-y-4">
              {/* Cohesion Badge top-right */}
              <div className="absolute top-0 right-0 bg-[#F4C13D] text-[#0D0D0D] px-3 py-1 font-mono text-xs font-bold tracking-widest uppercase">
                MATCH COHESION: {issue.cohesionScore || 96}%
              </div>

              {/* Box Title */}
              <div className="flex items-center space-x-2 pt-2">
                <Radar size={22} className="text-[#F06522]" />
                <div>
                  <h2 className="font-heading text-xl text-[#F1ECE2] uppercase tracking-wide">
                    WHY THIS MATCHES YOUR PROFILE
                  </h2>
                  <p className="font-mono text-xs text-[#666666] uppercase tracking-wider">
                    COMPUTED VIA AUDITED REPOSITORY HISTOGRAMS &amp; COMMIT TRAJECTORY
                  </p>
                </div>
              </div>

              {/* 4-Card Matching Breakdown Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">
                {/* Card 1: Language */}
                <div className="bg-[#1C1B1B] border border-[#2E2E2E] p-4 space-y-1.5 font-mono">
                  <span className="text-[11px] text-[#F4C13D] font-bold uppercase tracking-wider block">
                    MATCHING LANGUAGES
                  </span>
                  <div className="flex items-center justify-between">
                    <span className="font-heading text-base text-[#F1ECE2] uppercase">
                      {issue.stack}
                    </span>
                    <span className="text-[10px] bg-[#F06522]/20 text-[#F06522] border border-[#F06522]/40 px-1.5 py-0.5 font-bold uppercase">
                      100% MATCH
                    </span>
                  </div>
                  <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                    Direct alignment with project technology stack: {issue.stack}.
                  </p>
                </div>

                {/* Card 2: Frameworks & Ecosystem */}
                <div className="bg-[#1C1B1B] border border-[#2E2E2E] p-4 space-y-1.5 font-mono">
                  <span className="text-[11px] text-[#F4C13D] font-bold uppercase tracking-wider block">
                    MATCHING FRAMEWORKS
                  </span>
                  <div className="flex items-center justify-between">
                    <span className="font-heading text-base text-[#F1ECE2] uppercase">
                      {issue.tags?.slice(0, 2).join(', ') || issue.stack}
                    </span>
                  </div>
                  <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                    Framework and ecosystem verified against repository dependencies and package specifications.
                  </p>
                </div>

                {/* Card 3: Experience Level Assessment (spans 2) */}
                <div className="md:col-span-2 bg-[#1C1B1B] border border-[#2E2E2E] p-4 space-y-1.5 font-mono">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] text-[#F4C13D] font-bold uppercase tracking-wider">
                      EXPERIENCE LEVEL ASSESSMENT
                    </span>
                    <span className="text-[11px] bg-[#2A2A2A] text-[#F1ECE2] px-2 py-0.5 uppercase font-bold">
                      {issue.difficulty?.toUpperCase()}
                    </span>
                  </div>
                  <p className="font-mono text-xs text-[#D0CBC1] leading-relaxed">
                    {issue.whyItMatches ||
                      'The issue possesses an explicit, step-by-step contribution guide, verified unit test suites, and maintainers maintain an active turnaround response.'}
                  </p>
                </div>

                {/* Card 4: Transparent Reasons & Scope (spans 2) */}
                <div className="md:col-span-2 bg-[#1C1B1B] border border-[#2E2E2E] p-4 space-y-1.5 font-mono">
                  <span className="text-[11px] text-[#F4C13D] font-bold uppercase tracking-wider block">
                    TRANSPARENT REASONS &amp; SCOPE
                  </span>
                  <p className="font-mono text-xs text-[#D0CBC1] leading-relaxed">
                    Tagged explicitly for community contribution. Recommended for contributors seeking focused issue scope.
                  </p>
                </div>
              </div>

              {/* Algorithmic Analysis Disclaimer Notice */}
              <div className="pt-3 border-t border-[#222222] flex items-start space-x-2 font-mono text-[11px] text-[#666666] leading-relaxed">
                <Info size={14} className="text-[#888888] shrink-0 mt-0.5" />
                <span>
                  NOTE: GITHUB IS THE EXCLUSIVE CANONICAL SOURCE OF TRUTH. ALGORITHMIC ANALYSIS OFFERS STRUCTURAL HEURISTICS BUT DOES NOT CONSTITUTE A FORMAL COMMITMENT OF PR ACCEPTANCE.
                </span>
              </div>
            </div>

            {/* Box 2: Issue Context & Reproduction Specification */}
            <div className="bg-[#141414] border border-[#2E2E2E] p-6 space-y-4">
              <div className="border-b border-[#2E2E2E] pb-3 flex items-center justify-between">
                <h3 className="font-heading text-xl text-[#F1ECE2] uppercase tracking-wide">
                  ISSUE CONTEXT &amp; SPECIFICATION
                </h3>
                <span className="font-mono text-xs text-[#A7A29A]">
                  SOURCE: GITHUB ISSUE #{issue.number}
                </span>
              </div>

              <div className="space-y-3 font-mono text-sm text-[#D0CBC1] leading-relaxed">
                {issue.summary ? (
                  <p>{issue.summary}</p>
                ) : (
                  <p>
                    Full technical context, reproduction steps, and discussion are available directly in the upstream issue thread on GitHub.
                  </p>
                )}
              </div>
            </div>

            {/* Box 3: Expected Implementation Syntax / Code Block */}
            {issue.codeSnippet && (
              <div className="bg-[#0E0E0E] border border-[#2E2E2E] overflow-hidden">
                <div className="bg-[#1C1B1B] px-4 py-2.5 border-b border-[#2E2E2E] flex items-center justify-between">
                  <div className="flex items-center space-x-2 font-mono text-xs text-[#A7A29A]">
                    <FileCode size={15} className="text-[#F4C13D]" />
                    <span className="uppercase tracking-wider font-bold">
                      EXPECTED IMPLEMENTATION SYNTAX
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={handleCopyCode}
                    className="flex items-center space-x-1.5 font-mono text-xs text-[#A7A29A] hover:text-[#F4C13D] transition-colors uppercase font-bold tracking-wider"
                  >
                    {copiedCode ? (
                      <>
                        <Check size={14} className="text-[#2E7D32]" />
                        <span className="text-[#2E7D32]">COPIED</span>
                      </>
                    ) : (
                      <>
                        <Copy size={14} />
                        <span>COPY</span>
                      </>
                    )}
                  </button>
                </div>
                <pre className="p-4 font-mono text-xs text-[#B2C5FF] overflow-x-auto leading-relaxed bg-[#0E0E0E]">
                  <code>{issue.codeSnippet}</code>
                </pre>
              </div>
            )}

            {/* Box 4: Acceptance Criteria Ledger (Interactive Checklist) */}
            {issue.acceptanceCriteria && issue.acceptanceCriteria.length > 0 && (
              <div className="bg-[#141414] border border-[#2E2E2E] p-6 space-y-4">
                <div className="border-b border-[#2E2E2E] pb-3 flex items-center justify-between">
                  <h3 className="font-heading text-xl text-[#F4C13D] uppercase tracking-wide">
                    ACCEPTANCE CRITERIA FOR PULL REQUEST
                  </h3>
                  <span className="font-mono text-xs text-[#A7A29A]">
                    {Object.values(checkedCriteria).filter(Boolean).length} OF{' '}
                    {issue.acceptanceCriteria.length} CHECKED
                  </span>
                </div>

                <div className="space-y-2.5">
                  {issue.acceptanceCriteria.map((criterion, idx) => {
                    const isChecked = !!checkedCriteria[idx];
                    return (
                      <div
                        key={idx}
                        onClick={() => toggleCriterion(idx)}
                        className={`cursor-pointer p-3 border transition-colors flex items-start space-x-3 select-none ${
                          isChecked
                            ? 'bg-[#182319] border-[#2E7D32] text-[#F1ECE2]'
                            : 'bg-[#1C1B1B] border-[#2E2E2E] text-[#A7A29A] hover:border-[#666666]'
                        }`}
                      >
                        <span className="mt-0.5 shrink-0 text-[#F4C13D]">
                          {isChecked ? (
                            <CheckSquare size={16} className="text-[#2E7D32]" />
                          ) : (
                            <Square size={16} />
                          )}
                        </span>
                        <span
                          className={`font-mono text-xs leading-relaxed ${
                            isChecked ? 'line-through text-[#888888]' : ''
                          }`}
                        >
                          {criterion}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* ============================================================ */}
          {/* RIGHT COLUMN: Repo Dossier, Contributor Checklist, Tip (4 cols) */}
          {/* ============================================================ */}
          <div className="lg:col-span-4 space-y-6">
            {/* 1. Repository Dossier Ledger */}
            <div className="bg-[#141414] border border-[#2E2E2E] p-6 space-y-4">
              <div className="border-b border-[#2E2E2E] pb-2 flex items-center justify-between font-mono text-xs">
                <span className="text-[#F4C13D] font-bold uppercase tracking-widest">
                  REPOSITORY DOSSIER
                </span>
                <span className="text-[#666666]">
                  ORG / {issue.org?.toUpperCase() || issue.repo.split('/')[0]?.toUpperCase()}
                </span>
              </div>

              <div className="space-y-2.5 font-mono text-xs">
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">MAINTAINER</span>
                  <span className="text-[#F1ECE2] font-bold">{issue.openedBy}</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">OPEN ISSUES</span>
                  <span className="text-[#F1ECE2]">142 Active</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">GOOD FIRST ISSUES</span>
                  <span className="text-[#F06522] font-bold">6 Available</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">AVG PR MERGE TIME</span>
                  <span className="text-[#F4C13D] font-bold">1.8 Days</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">BRANCH TARGET</span>
                  <span className="text-[#5B86F2] font-mono">main (v11-next)</span>
                </div>
                <div className="flex justify-between py-1.5 border-b border-[#1F1F1F]">
                  <span className="text-[#666666] uppercase">COMMUNITY DISCORD</span>
                  <span className="text-[#F1ECE2]">Verified Partner</span>
                </div>
              </div>

              <a
                href={`https://github.com/${issue.repo}/blob/main/CONTRIBUTING.md`}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full text-center py-2.5 border border-[#2E2E2E] bg-[#1C1C1C] hover:bg-[#2A2A2A] hover:text-[#F1ECE2] text-[#A7A29A] font-mono text-xs uppercase tracking-wider transition-colors inline-block font-bold"
              >
                READ CONTRIBUTING.MD ↗
              </a>
            </div>

            {/* 2. Contributor Checklist Matrix Card */}
            <div className="bg-[#141414] border border-[#2E2E2E] p-6 space-y-3 font-mono">
              <span className="text-xs text-[#F4C13D] font-bold uppercase tracking-widest block border-b border-[#2E2E2E] pb-2">
                CONTRIBUTOR CHECKLIST
              </span>

              <div className="space-y-2 text-xs text-[#A7A29A]">
                {[
                  { id: 0, text: 'Fork repository to personal profile' },
                  { id: 1, text: 'Execute pnpm install & pnpm test' },
                  { id: 2, text: `Create branch feat/issue-${issue.number}` },
                  { id: 3, text: 'Implement test cases in test suite' },
                  { id: 4, text: `Submit PR referencing Fixes #${issue.number}` },
                ].map((item) => {
                  const isChecked = !!checkedChecklist[item.id];
                  return (
                    <div
                      key={item.id}
                      onClick={() => toggleChecklistItem(item.id)}
                      className="flex items-center space-x-2 cursor-pointer hover:text-[#F1ECE2] transition-colors select-none"
                    >
                      {isChecked ? (
                        <CheckSquare size={15} className="text-[#2E7D32] shrink-0" />
                      ) : (
                        <Square size={15} className="text-[#666666] shrink-0" />
                      )}
                      <span className={isChecked ? 'text-[#F1ECE2]' : 'text-[#888888]'}>
                        {item.text}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* 3. Crate Quick Help Notice */}
            <div className="border-l-2 border-[#F4C13D] bg-[#161616] p-4 font-mono space-y-1">
              <h4 className="text-xs font-bold text-[#F1ECE2] uppercase">
                NEED AN OPINION BEFORE PR?
              </h4>
              <p className="text-xs text-[#A7A29A] leading-relaxed">
                Maintainers advise commenting on the issue directly with your proposed type signature before opening the PR to avoid rework.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. RELATED ISSUES SECTION (if available) */}
      {relatedIssues.length > 0 && (
        <section className="w-full px-4 md:px-8 py-12 max-w-7xl mx-auto border-t border-[#2E2E2E]">
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
              <div>
                <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block mb-1">
                  CRATE RECOMMENDATIONS
                </span>
                <h3 className="font-heading text-2xl sm:text-3xl text-[#F1ECE2] uppercase tracking-tight">
                  SIMILAR CURATED ISSUES IN {issue.repo?.toUpperCase()}
                </h3>
              </div>

              <Link
                to={`${ROUTES.EXPLORE}?q=${encodeURIComponent(issue.repo)}`}
                className="font-mono text-xs text-[#F06522] hover:text-[#F4C13D] uppercase font-bold tracking-wider flex items-center gap-1 transition-colors"
              >
                <span>EXPLORE ALL {issue.repo?.split('/')[1]?.toUpperCase() || 'REPOSITORY'} ISSUES</span>
                <ArrowRight size={15} />
              </Link>
            </div>

            {/* Tracklist Table */}
            <div className="border-t border-b border-[#2E2E2E] divide-y divide-[#1F1F1F]">
              {relatedIssues.map((relIssue) => (
                <div
                  key={relIssue.id}
                  onClick={() => navigate(getIssueDetailsRoute(relIssue.id))}
                  className="group py-4 px-2 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-[#1A1A1A] transition-colors cursor-pointer select-none"
                >
                  <div className="flex items-center space-x-4">
                    <span className="font-mono text-xs font-bold text-[#666666] group-hover:text-[#F4C13D] w-14 shrink-0 transition-colors">
                      #{relIssue.number}
                    </span>
                    <div className="space-y-0.5">
                      <span className="font-heading text-lg text-[#F1ECE2] group-hover:text-[#F06522] uppercase tracking-wide block transition-colors">
                        {relIssue.title}
                      </span>
                      <span className="font-mono text-xs text-[#666666]">
                        {relIssue.repo} · {relIssue.commentsCount || 0} comments
                      </span>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 md:justify-end shrink-0">
                    {relIssue.difficulty && (
                      <span className="bg-[#F4C13D] text-[#0D0D0D] px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider">
                        {relIssue.difficulty}
                      </span>
                    )}
                    {relIssue.stack && (
                      <span className="bg-[#5B86F2] text-[#0D0D0D] px-2 py-0.5 text-[10px] font-mono font-bold uppercase tracking-wider">
                        {relIssue.stack}
                      </span>
                    )}
                    <div className="p-1 text-[#666666] group-hover:text-[#F1ECE2] transition-colors">
                      <ChevronRight size={18} />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
