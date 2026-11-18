import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { browsePublicIssues } from '../services/discoveryService';
import { savedIssueService } from '../services/savedIssueService';
import { getAuthStatus } from '../services/authService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  Terminal,
  ArrowRight,
  ExternalLink,
  Bookmark,
  CheckCircle2,
  Cpu,
  FolderGit2,
  Clock,
  Eye,
  Loader2,
} from 'lucide-react';

const TECH_STACK_ROWS = [
  {
    name: 'JAVA',
    bg: 'bg-[#F06522] text-[#0D0D0D]',
    desc: 'Spring Boot, Quarkus, Kafka, Micronaut, Maven Ecosystems',
    query: 'Java',
  },
  {
    name: 'PYTHON',
    bg: 'bg-[#F4C13D] text-[#0D0D0D]',
    desc: 'FastAPI, Django, PyTorch, Scikit-Learn, Poetry CLI Tools',
    query: 'Python',
  },
  {
    name: 'JAVASCRIPT',
    bg: 'bg-[#5B86F2] text-[#0D0D0D]',
    desc: 'Node runtime, bundlers, TypeScript primitives, ECMAScript standards',
    query: 'TypeScript',
  },
  {
    name: 'REACT',
    bg: 'bg-[#F29BB8] text-[#0D0D0D]',
    desc: 'Hooks, RSC Server Components, Next.js, Component Libraries, UI state',
    query: 'React',
  },
  {
    name: 'SPRING BOOT',
    bg: 'bg-[#E5E2E1] text-[#313030]',
    desc: 'Microservices, Spring Security 6, Data JPA, Native GraalVM',
    query: 'Java',
  },
  {
    name: 'NODE.JS',
    bg: 'bg-[#F06522] text-[#0D0D0D]',
    desc: 'Event loop, stream pipes, Express, Fastify, asynchronous I/O tooling',
    query: 'TypeScript',
  },
];

const MATCHING_RULES = [
  {
    rule: 'RULE // 001',
    icon: Cpu,
    title: 'SKILL MATCHING',
    desc: 'We decompose issues by architectural layer, not just syntax labels. If you specify backend experience with database migrations, we surface issues requiring schema refactoring rather than CSS adjustments accidentally tagged as general bugs.',
    validation: 'VALIDATION: SYNTAX + RUNTIME SPEC',
  },
  {
    rule: 'RULE // 002',
    icon: FolderGit2,
    title: 'REPOSITORY CONTEXT',
    desc: 'A good issue in a dead repository is a waste of your engineering hours. We inspect commit cadence, median PR merge speed, and issue close velocity before any repository is admitted into our discovery pool.',
    validation: 'CRITERIA: ACTIVE CI + MERGE VELOCITY',
  },
  {
    rule: 'RULE // 003',
    icon: Clock,
    title: 'ISSUE FRESHNESS',
    desc: "Stale issues with unresolved merge conflicts don't get surfaced. Our search queries GitHub directly upon your execution to confirm that issues remain active and open.",
    validation: 'LIVE PING: TIMESTAMP VERIFIED',
  },
  {
    rule: 'RULE // 004',
    icon: Eye,
    title: 'TRANSPARENT REASONS',
    desc: 'Every card shows exactly why it landed in your crate. No black-box algorithms. We list the direct match points, prerequisite concepts, and what to expect from the maintainers during review.',
    validation: 'INSPECTION: OPEN ATTRIBUTION',
  },
];

const JOURNEY_STEPS = [
  {
    step: 'STEP 01',
    code: '[IN]',
    title: 'SEARCH',
    desc: 'Specify stack, difficulty preferences, and personal objectives.',
    barColor: 'bg-[#F4C13D]',
    stepColor: 'text-[#F4C13D]',
  },
  {
    step: 'STEP 02',
    code: '[EVAL]',
    title: 'REVIEW',
    desc: 'Evaluate codebase structure, maintainer notes, and reproduction files.',
    barColor: 'bg-[#F06522]',
    stepColor: 'text-[#F06522]',
  },
  {
    step: 'STEP 03',
    code: '[HOLD]',
    title: 'SAVE',
    desc: 'Save promising issues into your personal workspace crate.',
    barColor: 'bg-[#5B86F2]',
    stepColor: 'text-[#5B86F2]',
  },
  {
    step: 'STEP 04',
    code: '[DEV]',
    title: 'WORK',
    desc: 'Fork the repo, branch locally, and write code against test suites.',
    barColor: 'bg-[#E5E2E1]',
    stepColor: 'text-[#F1ECE2]',
  },
  {
    step: 'STEP 05',
    code: '[OUT]',
    title: 'PULL REQUEST',
    desc: 'Open PR referencing the issue with verified test passes.',
    barColor: 'bg-[#F06522]',
    stepColor: 'text-[#F06522]',
  },
];

export function HomePage() {
  useDocumentTitle();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [featuredMatches, setFeaturedMatches] = useState([]);
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [loadingIssues, setLoadingIssues] = useState(true);
  const [savedIds, setSavedIds] = useState(new Set());
  const [showStickyMobileCta, setShowStickyMobileCta] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 350) {
        setShowStickyMobileCta(true);
      } else {
        setShowStickyMobileCta(false);
      }
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    let isMounted = true;
    async function loadFeatured() {
      setLoadingIssues(true);
      try {
        const [browseRes, savedRes] = await Promise.all([
          browsePublicIssues({
            labels: 'good first issue',
            perPage: 4,
            page: 1,
          }).catch(() => null),
          savedIssueService.getSavedIssues().catch(() => []),
        ]);

        if (isMounted) {
          const items = browseRes?.results || [];
          setFeaturedMatches(items);
          if (items.length > 0) {
            setSelectedMatch(items[0]);
          }
          if (Array.isArray(savedRes)) {
            const ids = new Set(savedRes.map((s) => String(s.githubIssueId)));
            setSavedIds(ids);
          }
        }
      } catch (err) {
        console.warn('Failed to load featured issues:', err);
      } finally {
        if (isMounted) {
          setLoadingIssues(false);
        }
      }
    }
    loadFeatured();
    return () => {
      isMounted = false;
    };
  }, []);

  const isCurrentSaved = selectedMatch?.issue?.id
    ? savedIds.has(String(selectedMatch.issue.id))
    : false;

  const handleToggleSave = async (e, match) => {
    e.stopPropagation();
    const issue = match?.issue;
    if (!issue) return;

    const auth = await getAuthStatus();
    if (!auth.authenticated) {
      navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.HOME)}`);
      return;
    }

    const issueIdStr = String(issue.id);
    const alreadySaved = savedIds.has(issueIdStr);

    try {
      if (alreadySaved) {
        const savedList = await savedIssueService.getSavedIssues();
        const found = savedList.find((s) => String(s.githubIssueId) === issueIdStr);
        if (found) {
          await savedIssueService.deleteSavedIssue(found.id);
          setSavedIds((prev) => {
            const next = new Set(prev);
            next.delete(issueIdStr);
            return next;
          });
        }
      } else {
        await savedIssueService.saveIssue({
          githubIssueId: issue.id,
          repositoryFullName: issue.repositoryFullName,
          issueNumber: issue.number,
          title: issue.title,
          issueUrl: issue.htmlUrl,
        });
        setSavedIds((prev) => new Set([...prev, issueIdStr]));
      }
    } catch (err) {
      console.error('Failed to toggle save issue:', err);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`${ROUTES.EXPLORE}?q=${encodeURIComponent(query.trim())}`);
    } else {
      navigate(ROUTES.EXPLORE);
    }
  };

  const handleQuickPreset = (presetQuery) => {
    setQuery(presetQuery);
    navigate(`${ROUTES.EXPLORE}?q=${encodeURIComponent(presetQuery)}`);
  };

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* SECTION 2: HERO SECTION */}
      <section className="w-full bg-[#0D0D0D] border-b border-[#2E2E2E] px-4 md:px-8 py-12 md:py-20 max-w-7xl mx-auto">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center mb-16">
          {/* Left Column: Title */}
          <div className="lg:col-span-7 xl:col-span-8 space-y-4">
            <div className="inline-flex items-center space-x-2 text-xs font-mono text-[#F4C13D]">
              <span className="w-2.5 h-2.5 bg-[#F06522] inline-block" />
              <span className="tracking-widest uppercase font-bold">OPEN SOURCE DISCOVERY</span>
            </div>

            <h1 className="font-heading text-5xl sm:text-6xl md:text-7xl lg:text-[5.5rem] xl:text-[6.5rem] tracking-tight leading-[0.88] uppercase">
              FIND <br />
              WHERE YOU FIT. <br />
              <span className="bg-[#F06522] text-[#0D0D0D] px-3 inline-block mt-1">
                CONTRIBUTE.
              </span>
            </h1>
          </div>

          {/* Right Column: Information Panel */}
          <div className="lg:col-span-5 xl:col-span-4 space-y-4 lg:border-l lg:border-[#2E2E2E] lg:pl-8">
            <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed">
              Discover open-source projects and real GitHub issues that match your skills, interests and experience. Explore opportunities across technologies and contribute to projects that matter.
            </p>

            <div className="border-t border-[#2E2E2E] pt-4 flex flex-wrap items-center gap-3">
              <Link to={ROUTES.EXPLORE}>
                <Button variant="primary" size="md">
                  EXPLORE ISSUES
                </Button>
              </Link>
              <Link to={ROUTES.HOW_IT_WORKS}>
                <Button variant="secondary" size="md">
                  HOW IT WORKS
                </Button>
              </Link>
            </div>
          </div>
        </div>

        {/* SECTION 3: SEARCH CONSOLE */}
        <div className="space-y-4 pt-6 border-t border-[#1C1C1C]">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-2">
            <div className="space-y-1">
              <span className="font-mono text-xs uppercase tracking-widest text-[#A7A29A] font-bold block">
                NATURAL SEARCH
              </span>
              <h2 className="font-heading text-2xl sm:text-3xl text-[#F1ECE2] uppercase tracking-tight m-0">
                DESCRIBE WHAT YOU WANT TO BUILD.
              </h2>
            </div>
            <div className="font-mono text-xs text-[#A7A29A] bg-[#141414] px-3 py-1 border border-[#2E2E2E] self-start sm:self-auto">
              AI DISCOVERY
            </div>
          </div>

          <form onSubmit={handleSearchSubmit} className="space-y-3">
            <div className="bg-[#141414] border-2 border-[#2E2E2E] p-4 focus-within:border-[#F06522] transition-colors flex flex-col gap-3">
              <div className="flex items-start gap-2">
                <span className="font-mono text-[#F4C13D] font-bold text-lg select-none pt-0.5">&gt;</span>
                <textarea
                  rows={2}
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Describe your skills, interests or the kind of open-source project you want to contribute to..."
                  className="w-full bg-transparent text-[#F1ECE2] font-mono text-sm placeholder:text-[#666666] focus:outline-none resize-none leading-relaxed"
                />
              </div>

              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-3 border-t border-[#222222]">
                <div className="flex items-center gap-2 text-xs font-mono text-[#A7A29A]">
                  <span className="w-2 h-2 bg-[#F4C13D] inline-block" />
                  <span>SEARCH GITHUB ISSUES BY TECHNOLOGY, FRAMEWORK OR TOPIC</span>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-auto">
                  {query && (
                    <button
                      type="button"
                      onClick={() => setQuery('')}
                      className="px-3 py-2 font-mono text-xs uppercase text-[#A7A29A] hover:text-[#F1ECE2] border border-[#2E2E2E] hover:bg-[#222222] transition-colors rounded-none"
                    >
                      CLEAR
                    </button>
                  )}
                  <Button type="submit" variant="primary" size="md">
                    FIND MATCHES
                    <ArrowRight size={16} className="ml-1.5" />
                  </Button>
                </div>
              </div>
            </div>

            {/* Presets Strip */}
            <div className="flex flex-wrap items-center gap-2 text-xs font-mono text-[#A7A29A] pt-1">
              <span className="text-[11px] uppercase tracking-wider text-[#666666] mr-1">PRESETS:</span>
              {[
                'Find beginner-friendly Python issues in machine learning projects',
                'Show me React and TypeScript frontend issues',
                'Find Java Spring Boot backend opportunities',
                'Explore Go projects with API and database issues',
                'Find documentation and accessibility improvements',
                'Show me good first issues across different programming languages',
              ].map((preset) => (
                <button
                  key={preset}
                  type="button"
                  onClick={() => handleQuickPreset(preset)}
                  className="px-2.5 py-1 bg-[#1A1A1A] hover:bg-[#F4C13D] hover:text-[#0D0D0D] border border-[#2E2E2E] transition-colors text-[11px] text-left rounded-none truncate max-w-full"
                >
                  {preset}
                </button>
              ))}
            </div>
          </form>
        </div>
      </section>

      {/* SECTION 4: SEARCH METADATA STRIP */}
      <section className="w-full bg-[#141414] border-b border-[#2E2E2E] overflow-x-auto py-3 px-4 md:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-6 min-w-[640px] font-mono text-xs tracking-wider uppercase text-[#A7A29A]">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 bg-[#F4C13D] inline-block" />
            <span className="text-[#F1ECE2]">[SOURCE: GITHUB REST API]</span>
          </div>
          <div>·</div>
          <div className="text-[#F06522] font-bold">[MODE: AI-ASSISTED DISCOVERY]</div>
          <div>·</div>
          <div>[FILTERS: USER CONTROLLED]</div>
          <div>·</div>
          <div className="text-[#00D26A] font-bold">[DATA: LIVE REPOSITORY INDEX]</div>
        </div>
      </section>

      {/* SECTION 5: LIVE ISSUE WORKSPACE */}
      <section className="w-full bg-[#0D0D0D] px-4 md:px-8 py-16 border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-col gap-6">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between border-b border-[#2E2E2E] pb-3 gap-2">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-[#F06522] font-bold block mb-1">
                FEATURED OPEN SOURCE ISSUES
              </span>
              <h2 className="font-heading text-3xl md:text-4xl uppercase text-[#F1ECE2] tracking-tight m-0">
                THE DISCOVERY WORKSPACE
              </h2>
            </div>
            <Link
              to={ROUTES.EXPLORE}
              className="text-xs font-mono text-[#F4C13D] hover:underline uppercase font-bold"
            >
              EXPLORE ALL ISSUES →
            </Link>
          </div>

          {loadingIssues ? (
            <div className="border border-[#2E2E2E] bg-[#141414] p-12 text-center space-y-3">
              <Loader2 size={24} className="animate-spin text-[#F06522] mx-auto" />
              <div className="font-mono text-xs text-[#A7A29A] uppercase tracking-wider">
                LOADING FEATURED GITHUB ISSUES...
              </div>
            </div>
          ) : featuredMatches.length === 0 ? (
            <div className="border border-[#2E2E2E] bg-[#141414] p-12 text-center space-y-4">
              <p className="font-mono text-xs text-[#A7A29A] uppercase">
                NO FEATURED ISSUES CURRENTLY CACHED. BROWSE THE COMPLETE LIVE GITHUB CATALOG.
              </p>
              <Link to={ROUTES.EXPLORE}>
                <Button variant="primary" size="md">
                  OPEN EXPLORE CATALOG
                </Button>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-12 border border-[#2E2E2E] bg-[#0E0E0E]">
              {/* Left: Issue Table Stream (7 cols) */}
              <div className="lg:col-span-7 border-b lg:border-b-0 lg:border-r border-[#2E2E2E] flex flex-col">
                <div className="bg-[#1C1B1B] px-4 py-2 border-b border-[#2E2E2E] flex items-center justify-between text-xs font-mono">
                  <span className="font-bold text-[#F1ECE2] uppercase tracking-wider">
                    FEATURED CANDIDATE ISSUES
                  </span>
                  <span className="text-[#00D26A] uppercase font-bold">VERIFIED REPOSITORIES</span>
                </div>

                {/* Table Header */}
                <div className="hidden sm:grid grid-cols-12 bg-[#141414] px-4 py-2 border-b border-[#2E2E2E] font-mono text-[11px] text-[#A7A29A] uppercase font-bold">
                  <div className="col-span-6">REPOSITORY / ISSUE</div>
                  <div className="col-span-4">TAGS</div>
                  <div className="col-span-2 text-right">STATUS</div>
                </div>

                {/* Table Rows */}
                <div className="divide-y divide-[#222222] flex flex-col">
                  {featuredMatches.map((match) => {
                    const issue = match.issue;
                    if (!issue) return null;
                    const isCurrent = selectedMatch?.issue?.id === issue.id;
                    const labels = issue.labels || [];
                    return (
                      <div
                        key={issue.id}
                        onClick={() => setSelectedMatch(match)}
                        className={`cursor-pointer p-4 transition-colors group tracklist-row border-l-4 ${
                          isCurrent
                            ? 'border-l-[#F06522] bg-[#1A1A1A]'
                            : 'border-l-transparent hover:bg-[#F1ECE2] hover:text-[#0D0D0D]'
                        }`}
                      >
                        <div className="grid grid-cols-1 sm:grid-cols-12 gap-2 sm:gap-0 items-start sm:items-center">
                          <div className="sm:col-span-6 flex flex-col pr-2">
                            <span
                              className={`font-mono text-xs font-bold ${
                                isCurrent ? 'text-[#F06522]' : 'text-[#A7A29A] group-hover:text-[#0D0D0D]'
                              }`}
                            >
                              {issue.repositoryFullName}
                            </span>
                            <span className="font-sans text-sm font-semibold text-[#F1ECE2] group-hover:text-[#0D0D0D] line-clamp-1">
                              #{issue.number} {issue.title}
                            </span>
                          </div>

                          <div className="sm:col-span-4 flex flex-wrap gap-1">
                            {labels.slice(0, 3).map((lbl, idx) => (
                              <span
                                key={idx}
                                className="font-mono text-[10px] px-1.5 py-0.5 uppercase font-bold bg-[#2A2A2A] text-[#A7A29A] group-hover:bg-[#0D0D0D] group-hover:text-[#F1ECE2]"
                              >
                                {typeof lbl === 'string' ? lbl : lbl.name}
                              </span>
                            ))}
                          </div>

                          <div className="sm:col-span-2 sm:text-right flex sm:justify-end">
                            <span
                              className={`font-mono text-[10px] px-2 py-0.5 uppercase font-bold border ${
                                !issue.isAssigned
                                  ? 'bg-[#F06522]/20 text-[#F06522] border-[#F06522]/40 group-hover:border-[#0D0D0D] group-hover:bg-[#0D0D0D] group-hover:text-[#F06522]'
                                  : 'bg-[#1C1B1B] text-[#A7A29A] border-[#2E2E2E] group-hover:border-[#0D0D0D] group-hover:text-[#0D0D0D]'
                              }`}
                            >
                              {issue.isAssigned ? 'ASSIGNED' : 'UNASSIGNED'}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {/* Table Footer */}
                <div className="p-3 bg-[#141414] border-t border-[#2E2E2E] flex items-center justify-between font-mono text-xs">
                  <span className="text-[#A7A29A]">USE SEARCH TO EXPAND RESULTS</span>
                  <Link
                    to={ROUTES.EXPLORE}
                    className="text-[#F4C13D] hover:underline uppercase font-bold"
                  >
                    VIEW ALL IN CATALOG →
                  </Link>
                </div>
              </div>

              {/* Right: Sleeve Inspector (5 cols) */}
              {selectedMatch?.issue ? (
                <div className="lg:col-span-5 bg-[#141414] p-6 flex flex-col justify-between gap-6">
                  <div className="flex flex-col gap-4">
                    {/* Inspection Header */}
                    <div className="flex items-start justify-between border-b border-[#2E2E2E] pb-3">
                      <div className="flex flex-col">
                        <span className="font-mono text-xs text-[#A7A29A] uppercase tracking-wider">
                          ISSUE INSPECTOR // #{selectedMatch.issue.number}
                        </span>
                        <span className="font-heading text-lg text-[#F1ECE2] uppercase">
                          {selectedMatch.issue.repositoryFullName}
                        </span>
                      </div>
                    </div>

                    {/* Title & Metadata */}
                    <div>
                      <h3 className="font-heading text-2xl uppercase text-[#F1ECE2] leading-tight m-0">
                        {selectedMatch.issue.title}
                      </h3>
                      <div className="flex items-center gap-2 mt-2 text-[#A7A29A] font-mono text-xs">
                        <span>OPENED BY @{selectedMatch.issue.authorUsername || 'contributor'}</span>
                        <span>·</span>
                        <span>{selectedMatch.issue.commentsCount || 0} COMMENTS</span>
                      </div>
                    </div>

                    {/* Tags */}
                    {selectedMatch.issue.labels && selectedMatch.issue.labels.length > 0 && (
                      <div className="flex flex-col gap-1.5">
                        <span className="font-mono text-[11px] text-[#A7A29A] uppercase tracking-wider">
                          LABELS:
                        </span>
                        <div className="flex flex-wrap gap-1.5">
                          {selectedMatch.issue.labels.slice(0, 5).map((tag, idx) => (
                            <Badge key={idx} variant="surface" size="xs">
                              {typeof tag === 'string' ? tag : tag.name}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Why it matches */}
                    {selectedMatch.matchExplanation && (
                      <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-3 flex flex-col gap-1.5">
                        <div className="flex items-center gap-1.5 text-[#F4C13D] font-mono text-xs font-bold uppercase">
                          <CheckCircle2 size={15} />
                          <span>MATCH SUMMARY:</span>
                        </div>
                        <p className="font-mono text-xs text-[#A7A29A] leading-relaxed m-0">
                          {selectedMatch.matchExplanation}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* CTAs */}
                  <div className="flex flex-col sm:flex-row items-center gap-2 pt-3 border-t border-[#2E2E2E]">
                    <a
                      href={selectedMatch.issue.htmlUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full sm:flex-1"
                    >
                      <Button variant="primary" size="md" className="w-full flex items-center justify-center gap-1.5">
                        <span>VIEW GITHUB ISSUE</span>
                        <ExternalLink size={14} />
                      </Button>
                    </a>
                    <Button
                      variant="secondary"
                      size="md"
                      onClick={(e) => handleToggleSave(e, selectedMatch)}
                      className="w-full sm:w-auto flex items-center justify-center gap-1.5"
                    >
                      <Bookmark size={14} fill={isCurrentSaved ? '#F06522' : 'none'} className={isCurrentSaved ? 'text-[#F06522]' : ''} />
                      <span>{isCurrentSaved ? 'IN CRATE' : 'SAVE TO CRATE'}</span>
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="lg:col-span-5 bg-[#141414] p-6 flex items-center justify-center text-xs font-mono text-[#A7A29A]">
                  SELECT AN ISSUE TO VIEW DETAILS
                </div>
              )}
            </div>
          )}
        </div>
      </section>

      {/* SECTION 6: HOW DISCOVERY WORKS */}
      <section className="w-full bg-[#0D0D0D] px-4 md:px-8 py-16 border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-col gap-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between border-b border-[#2E2E2E] pb-3 gap-2">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-[#F4C13D] font-bold block mb-1">
                DISCOVERY WORKFLOW
              </span>
              <h2 className="font-heading text-3xl md:text-4xl uppercase text-[#F1ECE2] tracking-tight m-0">
                FROM SKILLS TO CONTRIBUTIONS.
              </h2>
            </div>
            <div className="font-mono text-xs text-[#A7A29A] uppercase">
              SEMANTIC SEARCH + GITHUB REST API
            </div>
          </div>

          <div className="flex flex-col border border-[#2E2E2E] divide-y divide-[#2E2E2E] bg-[#141414]">
            {/* Step 01 */}
            <div className="p-6 md:p-8 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start hover:bg-[#181818] transition-colors">
              <div className="lg:col-span-4 flex flex-col">
                <span className="font-mono text-xs text-[#F06522] font-bold">01 // SPECIFY</span>
                <h3 className="font-heading text-2xl md:text-3xl uppercase text-[#F1ECE2] m-0 tracking-tight mt-1">
                  01 — DESCRIBE
                </h3>
              </div>
              <div className="lg:col-span-8 flex flex-col justify-center">
                <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed m-0">
                  Tell us your languages, frameworks, experience level, and preferred domains. Whether you want to master virtual threads in Java 21, write Postgres migration scripts, or fix rendering bottlenecks in React, state your scope plainly.
                </p>
                <div className="flex flex-wrap items-center gap-2 mt-4 font-mono text-xs text-[#A7A29A]">
                  <span className="text-[#F4C13D] font-bold">&gt; INPUT:</span>
                  <span className="bg-[#1C1B1B] border border-[#2E2E2E] px-2 py-0.5 text-[#F1ECE2]">
                    NATURAL LANGUAGE PROMPT
                  </span>
                </div>
              </div>
            </div>

            {/* Step 02 */}
            <div className="p-6 md:p-8 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start hover:bg-[#181818] transition-colors">
              <div className="lg:col-span-4 flex flex-col">
                <span className="font-mono text-xs text-[#F4C13D] font-bold">02 // SEARCH</span>
                <h3 className="font-heading text-2xl md:text-3xl uppercase text-[#F1ECE2] m-0 tracking-tight mt-1">
                  02 — DISCOVER
                </h3>
              </div>
              <div className="lg:col-span-8 flex flex-col justify-center">
                <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed m-0">
                  AI interprets your request and queries actual, live GitHub issues. It screens out abandoned repositories, verifies issue freshness, evaluates maintainer responsiveness, and grades contributor onboarding complexity.
                </p>
                <div className="flex flex-wrap items-center gap-2 mt-4 font-mono text-xs text-[#A7A29A]">
                  <span className="text-[#F4C13D] font-bold">&gt; FILTER CRITERIA:</span>
                  <span className="bg-[#1C1B1B] border border-[#2E2E2E] px-2 py-0.5 text-[#F1ECE2]">
                    ACTIVE COMMITS
                  </span>
                  <span className="bg-[#1C1B1B] border border-[#2E2E2E] px-2 py-0.5 text-[#F1ECE2]">
                    VERIFIED OPEN ISSUES
                  </span>
                </div>
              </div>
            </div>

            {/* Step 03 */}
            <div className="p-6 md:p-8 grid grid-cols-1 lg:grid-cols-12 gap-6 items-start hover:bg-[#181818] transition-colors">
              <div className="lg:col-span-4 flex flex-col">
                <span className="font-mono text-xs text-[#5B86F2] font-bold">03 // CONTRIBUTE</span>
                <h3 className="font-heading text-2xl md:text-3xl uppercase text-[#F1ECE2] m-0 tracking-tight mt-1">
                  03 — CONTRIBUTE
                </h3>
              </div>
              <div className="lg:col-span-8 flex flex-col justify-center">
                <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed m-0">
                  Review curated issue briefs with code snippets, maintainer suggestions, and step-by-step contribution prerequisites. Save candidate issues directly to your crate and transition straight to your pull request.
                </p>
                <div className="flex flex-wrap items-center gap-2 mt-4 font-mono text-xs text-[#A7A29A]">
                  <span className="text-[#F4C13D] font-bold">&gt; ACTION:</span>
                  <span className="bg-[#1C1B1B] border border-[#2E2E2E] px-2 py-0.5 text-[#F1ECE2]">
                    DIRECT FORK &amp; CLONE
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* SECTION 7: TECHNOLOGY CRATES */}
      <section className="w-full bg-[#0D0D0D] border-b border-[#2E2E2E]">
        <div className="px-4 md:px-8 py-10 border-b border-[#2E2E2E]">
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-end justify-between gap-2">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-[#F06522] font-bold block mb-1">
                TECHNOLOGY CATALOG
              </span>
              <h2 className="font-heading text-3xl md:text-4xl uppercase text-[#F1ECE2] tracking-tight m-0">
                PICK YOUR STACK
              </h2>
            </div>
            <p className="font-mono text-xs text-[#A7A29A] uppercase">
              POPULAR RUNTIMES &amp; ECOSYSTEMS
            </p>
          </div>
        </div>

        {/* Full-Width Colored Tech Rows */}
        <div className="w-full flex flex-col divide-y divide-[#2E2E2E]">
          {TECH_STACK_ROWS.map((stack, idx) => (
            <div
              key={idx}
              onClick={() => navigate(`${ROUTES.EXPLORE}?stack=${encodeURIComponent(stack.query)}`)}
              className={`group block w-full ${stack.bg} px-4 md:px-8 lg:px-12 py-6 transition-all duration-75 hover:pl-8 lg:hover:pl-16 cursor-pointer select-none`}
            >
              <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex flex-col sm:flex-row sm:items-baseline gap-2 sm:gap-6">
                  <span className="font-heading text-3xl sm:text-4xl md:text-5xl uppercase tracking-tighter leading-none">
                    {stack.name}
                  </span>
                  <span className="font-mono text-xs md:text-sm font-semibold opacity-90">
                    {stack.desc}
                  </span>
                </div>
                <div className="flex items-center gap-2 font-mono text-xs font-bold tracking-widest uppercase shrink-0">
                  <span>BROWSE ISSUES</span>
                  <ArrowRight size={16} className="transition-transform group-hover:translate-x-1" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* SECTION 8: MATCHING PRINCIPLES */}
      <section className="w-full bg-[#0D0D0D] px-4 md:px-8 py-16 border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-col gap-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between border-b border-[#2E2E2E] pb-3 gap-2">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-[#F4C13D] font-bold block mb-1">
                SYSTEM RIGOR
              </span>
              <h2 className="font-heading text-3xl md:text-4xl uppercase text-[#F1ECE2] tracking-tight m-0">
                NO RANDOM ISSUES. BETTER MATCHES.
              </h2>
            </div>
            <div className="font-mono text-xs text-[#A7A29A] uppercase">
              REAL GITHUB METADATA
            </div>
          </div>

          {/* 2x2 Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 border-t border-l border-[#2E2E2E]">
            {MATCHING_RULES.map((rule, idx) => {
              const Icon = rule.icon;
              return (
                <div
                  key={idx}
                  className="border-b border-r border-[#2E2E2E] p-6 md:p-8 bg-[#141414] flex flex-col justify-between gap-6 hover:bg-[#181818] transition-colors"
                >
                  <div className="flex items-start justify-between">
                    <span className="font-mono text-xs font-bold text-[#F06522]">{rule.rule}</span>
                    <Icon size={20} className="text-[#F4C13D]" />
                  </div>
                  <div>
                    <h3 className="font-heading text-2xl uppercase text-[#F1ECE2] m-0 tracking-tight">
                      {rule.title}
                    </h3>
                    <p className="font-mono text-xs text-[#A7A29A] mt-2 leading-relaxed">
                      {rule.desc}
                    </p>
                  </div>
                  <div className="font-mono text-[11px] text-[#A7A29A] border-t border-[#222222] pt-3 uppercase">
                    {rule.validation}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* SECTION 9: CONTRIBUTION JOURNEY */}
      <section className="w-full bg-[#0D0D0D] px-4 md:px-8 py-16 border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-col gap-8">
          <div className="flex flex-col sm:flex-row sm:items-end justify-between border-b border-[#2E2E2E] pb-3 gap-2">
            <div>
              <span className="font-mono text-xs uppercase tracking-widest text-[#F06522] font-bold block mb-1">
                WORKFLOW TIMELINE
              </span>
              <h2 className="font-heading text-3xl md:text-4xl uppercase text-[#F1ECE2] tracking-tight m-0">
                FROM DISCOVERY TO PULL REQUEST.
              </h2>
            </div>
            <div className="font-mono text-xs text-[#A7A29A] uppercase">
              5 STEPS TO CONTRIBUTE
            </div>
          </div>

          {/* 5-Column Linear Stepper Bar */}
          <div className="grid grid-cols-1 md:grid-cols-5 border-t border-l border-[#2E2E2E]">
            {JOURNEY_STEPS.map((s, idx) => (
              <div
                key={idx}
                className="p-6 bg-[#141414] border-b border-r border-[#2E2E2E] flex flex-col justify-between gap-4 hover:bg-[#181818] transition-colors"
              >
                <div className="flex items-center justify-between font-mono text-xs">
                  <span className={`font-bold ${s.stepColor}`}>{s.step}</span>
                  <span className="text-[#A7A29A]">{s.code}</span>
                </div>
                <div>
                  <h4 className="font-heading text-xl uppercase text-[#F1ECE2] m-0 tracking-tight">
                    {s.title}
                  </h4>
                  <p className="font-mono text-xs text-[#A7A29A] mt-1.5 leading-relaxed">
                    {s.desc}
                  </p>
                </div>
                <div className={`w-full h-1 ${s.barColor}`} />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* SECTION 10: FINAL CTA */}
      <section className="w-full bg-[#F06522] text-[#0D0D0D] px-4 md:px-8 lg:px-12 py-16 md:py-24">
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-center justify-between gap-8">
          <div className="flex flex-col gap-2 max-w-3xl">
            <span className="font-mono text-xs uppercase font-bold tracking-widest text-[#0D0D0D]/80">
              GET STARTED
            </span>
            <h2 className="font-heading text-4xl sm:text-5xl md:text-6xl lg:text-7xl uppercase tracking-tighter leading-[0.9] m-0">
              STOP SEARCHING.<br />START CONTRIBUTING.
            </h2>
            <p className="font-mono text-sm md:text-base text-[#0D0D0D] font-medium mt-2 leading-relaxed">
              Find an issue that fits your skills and take your next step into open source today. Real repositories. Active teams. Genuine contributions.
            </p>
          </div>
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4">
            <Link to={ROUTES.EXPLORE}>
              <button
                type="button"
                className="inline-flex items-center justify-center px-8 py-4 bg-[#0D0D0D] text-[#F1ECE2] font-mono text-sm font-bold uppercase tracking-wider hover:bg-[#1A1A1A] hover:scale-[1.02] active:scale-[0.99] transition-all rounded-none select-none cursor-pointer"
              >
                FIND YOUR FIRST ISSUE
              </button>
            </Link>
          </div>
        </div>
      </section>

      {/* Sticky Mobile CTA: Appears when scrolling past the hero */}
      {showStickyMobileCta && (
        <div className="sm:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#141414]/95 backdrop-blur-md border-t-2 border-[#F06522] px-4 py-3 flex items-center justify-between shadow-2xl">
          <div className="space-y-0.5">
            <span className="block font-mono text-[10px] text-[#F4C13D] uppercase font-bold tracking-wider">
              OPEN SOURCE DISCOVERY
            </span>
            <span className="block font-heading text-sm text-[#F1ECE2] uppercase tracking-tight">
              REAL GITHUB ISSUES
            </span>
          </div>
          <Link to={ROUTES.EXPLORE}>
            <Button variant="primary" size="sm" className="flex items-center gap-1 font-bold">
              <span>EXPLORE</span>
              <ArrowRight size={13} />
            </Button>
          </Link>
        </div>
      )}
    </div>
  );
}

export default HomePage;
