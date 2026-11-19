import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { usePageSEO } from '../hooks/usePageSEO';
import { savedIssueService } from '../services/savedIssueService';
import {
  browsePublicIssues,
  searchAiIssues,
  validateSearchPrompt,
  validatePublicFilter,
} from '../services/discoveryService';
import { getAuthStatus, getOAuthLoginUrl } from '../services/authService';
import { IssueCard } from '../components/explore/IssueCard';
import { IssueSkeleton } from '../components/explore/IssueSkeleton';
import { Button } from '../components/ui/Button';
import {
  Terminal,
  Search,
  Sparkles,
  ExternalLink,
  Bookmark,
  X,
  AlertTriangle,
  CheckCircle2,
  Copy,
  Check,
  RotateCcw,
  ShieldCheck,
  ShieldAlert,
  LogIn,
  Info,
  Compass,
  Filter,
  ToggleLeft,
  ToggleRight,
} from 'lucide-react';

const LANGUAGES = ['ALL', 'Java', 'TypeScript', 'JavaScript', 'Python', 'Go', 'Rust', 'C++'];
const LABELS = ['ALL', 'good first issue', 'help wanted', 'documentation', 'bug', 'enhancement'];
const SORTS = [
  { value: 'updated', label: 'NEWEST / RECENT ACTIVITY' },
  { value: 'comments', label: 'MOST COMMENTS' },
  { value: 'created', label: 'RECENTLY CREATED' },
];

export function ExplorePage() {
  usePageSEO({
    title: 'Find Beginner-Friendly Open Source Issues',
    description:
      'Find beginner-friendly open source issues and good first issues on GitHub. Search verified repositories, discover contribution opportunities, and start coding.',
    canonicalPath: ROUTES.EXPLORE,
  });
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Mode state: 'GUEST' or 'AUTHENTICATED_AI'
  const [mode, setMode] = useState('GUEST');

  // Search input state
  const initialQ = searchParams.get('q') || '';
  const initialLang = searchParams.get('lang') || 'ALL';
  const initialLabel = searchParams.get('label') || 'good first issue';
  const initialPage = parseInt(searchParams.get('page') || '1', 10);

  const [query, setQuery] = useState(initialQ);
  const [selectedLanguage, setSelectedLanguage] = useState(initialLang);
  const [selectedLabel, setSelectedLabel] = useState(initialLabel);
  const [selectedSort, setSelectedSort] = useState('updated');
  const [validationError, setValidationError] = useState(null);

  // Active execution parameters for pagination
  const [activeParams, setActiveParams] = useState(null);

  // Pagination & Results state
  const [page, setPage] = useState(Math.max(1, initialPage));
  const [perPage, setPerPage] = useState(30);
  const [results, setResults] = useState([]);
  const [parsedFilter, setParsedFilter] = useState(null);
  const [totalCount, setTotalCount] = useState(0);
  const [accessibleCount, setAccessibleCount] = useState(0);
  const [backendTotalPages, setBackendTotalPages] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [hasPrevious, setHasPrevious] = useState(false);
  const [remainingDailyQuota, setRemainingDailyQuota] = useState(null);
  const [rateLimit, setRateLimit] = useState(null);

  // Inspector & selection state
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [savedList, setSavedList] = useState([]);
  const [savedIds, setSavedIds] = useState([]);
  const [copiedLink, setCopiedLink] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);
  const detailsPanelRef = useRef(null);

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  // Scroll details panel back to top when selected issue changes
  useEffect(() => {
    if (detailsPanelRef.current) {
      detailsPanelRef.current.scrollTop = 0;
    }
  }, [selectedMatch?.issue?.id]);

  // Execution & UI state
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [errorState, setErrorState] = useState(null);

  // User auth state
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  // Check user authentication status on mount and configure mode
  useEffect(() => {
    let isMounted = true;
    getAuthStatus()
      .then(async (status) => {
        if (isMounted) {
          const isAuth = Boolean(status && status.authenticated);
          setCurrentUser(isAuth ? status : null);
          setMode(isAuth ? 'AUTHENTICATED_AI' : 'GUEST');
          setAuthLoading(false);
          if (isAuth) {
            try {
              const saved = await savedIssueService.getSavedIssues();
              if (isMounted) {
                setSavedList(saved);
                setSavedIds(saved.map((s) => String(s.githubIssueId)));
              }
            } catch {
              // ignore error loading saved issues
            }
          } else {
            setSavedList([]);
            setSavedIds([]);
          }
        }
      })
      .catch(() => {
        if (isMounted) {
          setCurrentUser(null);
          setMode('GUEST');
          setAuthLoading(false);
          setSavedList([]);
          setSavedIds([]);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  // Execute issue discovery search against the backend API
  const executeSearch = useCallback(
    async (execOptions, pageNumber = 1) => {
      const execMode = execOptions?.mode || mode;
      setErrorState(null);
      setValidationError(null);

      if (execMode === 'AUTHENTICATED_AI') {
        if (!currentUser) {
          setErrorState({
            status: 401,
            type: 'auth',
            title: 'AUTHENTICATION REQUIRED FOR AI DISCOVERY',
            message:
              'AI-powered semantic search requires an authenticated account. You can switch to Guest Mode below to browse issues without logging in, or sign in to receive 20 daily AI searches.',
          });
          return;
        }

        const promptText = (execOptions?.prompt || query || '').trim();
        const validation = validateSearchPrompt(promptText);
        if (!validation.valid) {
          setValidationError(validation.error);
          return;
        }

        setLoading(true);
        try {
          const response = await searchAiIssues({
            prompt: promptText,
            page: pageNumber,
            perPage: 30,
          });

          if (response) {
            const respTotal = response.totalCount || 0;
            const respAccessible = response.accessibleCount ?? Math.min(respTotal, 1000);
            const respPerPage = response.perPage || 30;
            const respTotalPages = response.totalPages ?? (respAccessible > 0 ? Math.ceil(respAccessible / respPerPage) : 0);

            setResults(response.results || []);
            setParsedFilter(response.parsedFilter || null);
            setTotalCount(respTotal);
            setAccessibleCount(respAccessible);
            setBackendTotalPages(respTotalPages);
            setPage(response.page || pageNumber);
            setPerPage(respPerPage);
            setHasNext(Boolean(response.hasNext));
            setHasPrevious(Boolean(response.hasPrevious));
            setRemainingDailyQuota(response.remainingDailyQuota);
            setRateLimit(response.rateLimit || null);
            setHasSearched(true);
            setActiveParams({ mode: 'AUTHENTICATED_AI', prompt: promptText });
            setSelectedMatch(response.results?.[0] || null);

            setSearchParams({ q: promptText, page: String(pageNumber) }, { replace: true });
          }
        } catch (err) {
          handleSearchError(err);
        } finally {
          setLoading(false);
        }
      } else {
        // GUEST MODE
        const queryText = (execOptions?.query !== undefined ? execOptions.query : query || '').trim();
        const lang = execOptions?.language !== undefined ? execOptions.language : selectedLanguage;
        const label = execOptions?.label !== undefined ? execOptions.label : selectedLabel;
        const sortVal = execOptions?.sort !== undefined ? execOptions.sort : selectedSort;

        const validation = validatePublicFilter({
          query: queryText,
          language: lang,
          labels: label !== 'ALL' ? [label] : [],
        });

        if (!validation.valid) {
          setValidationError(validation.error);
          return;
        }

        setLoading(true);
        try {
          const response = await browsePublicIssues({
            query: queryText,
            language: lang,
            labels: label !== 'ALL' ? [label] : [],
            sort: sortVal,
            page: pageNumber,
            perPage: 30,
          });

          if (response) {
            const respTotal = response.totalCount || 0;
            const respAccessible = response.accessibleCount ?? Math.min(respTotal, 1000);
            const respPerPage = response.perPage || 30;
            const respTotalPages = response.totalPages ?? (respAccessible > 0 ? Math.ceil(respAccessible / respPerPage) : 0);

            setResults(response.results || []);
            setParsedFilter(response.parsedFilter || null);
            setTotalCount(respTotal);
            setAccessibleCount(respAccessible);
            setBackendTotalPages(respTotalPages);
            setPage(response.page || pageNumber);
            setPerPage(respPerPage);
            setHasNext(Boolean(response.hasNext));
            setHasPrevious(Boolean(response.hasPrevious));
            setRemainingDailyQuota(null);
            setRateLimit(response.rateLimit || null);
            setHasSearched(true);
            setActiveParams({
              mode: 'GUEST',
              query: queryText,
              language: lang,
              label: label,
              sort: sortVal,
            });
            setSelectedMatch(response.results?.[0] || null);

            setSearchParams(
              {
                ...(queryText ? { q: queryText } : {}),
                ...(lang !== 'ALL' ? { lang } : {}),
                ...(label !== 'ALL' ? { label } : {}),
                page: String(pageNumber),
              },
              { replace: true }
            );
          }
        } catch (err) {
          handleSearchError(err);
        } finally {
          setLoading(false);
        }
      }
    },
    [mode, query, selectedLanguage, selectedLabel, selectedSort, setSearchParams, currentUser]
  );

  const handleSearchError = (err) => {
    console.error('[ExplorePage] Discovery search error:', err);
    const status = err.status || (err.data && err.data.status) || 500;
    const msg = (err.data && err.data.message) || err.message;

    if (status === 401) {
      setCurrentUser(null);
      setErrorState({
        status: 401,
        type: 'auth',
        title: 'AUTHENTICATION REQUIRED FOR AI DISCOVERY',
        message:
          'AI-powered semantic search requires an authenticated account. You can switch to Guest Mode below to browse issues without logging in, or sign in to receive 20 daily AI searches.',
      });
    } else if (status === 403) {
      setErrorState({
        status: 403,
        type: 'csrf',
        title: 'SECURITY TOKEN EXPIRED',
        message: 'Your session verification token (CSRF) was rejected or expired. Please retry your search.',
      });
    } else if (status === 429) {
      const isAiQuota = msg && msg.toLowerCase().includes('quota');
      setErrorState({
        status: 429,
        type: isAiQuota ? 'quota' : 'ratelimit',
        title: isAiQuota ? 'DAILY AI DISCOVERY QUOTA REACHED' : 'RATE LIMIT REACHED',
        message: msg || 'Too many requests. Please wait for your limit to reset or switch to Guest Mode.',
      });
    } else if (status === 503) {
      setErrorState({
        status: 503,
        type: 'ai-unavailable',
        title: 'AI DISCOVERY ENGINE UNAVAILABLE',
        message:
          msg ||
          'The AI interpretation service is currently unavailable. You can continue browsing issues using Guest Mode.',
      });
    } else if (status === 502) {
      setErrorState({
        status: 502,
        type: 'github-unavailable',
        title: 'GITHUB REST API COMMUNICATION ERROR',
        message: msg || 'GitHub API is temporarily unreachable or returned an invalid response. Please try again shortly.',
      });
    } else if (status === 400) {
      setErrorState({
        status: 400,
        type: 'validation',
        title: 'INVALID SEARCH REQUEST',
        message: msg || 'Please adjust your search filter criteria and try again.',
      });
    } else {
      setErrorState({
        status: 500,
        type: 'server',
        title: 'DISCOVERY INDEX FAULT',
        message: 'An unexpected error occurred while processing your discovery search. Please try again.',
      });
    }
  };

  // Auto-search if query parameters exist on initial load once auth status is resolved
  useEffect(() => {
    if (authLoading) return;
    if (initialQ || initialLang !== 'ALL' || initialLabel !== 'ALL') {
      executeSearch({
        mode: currentUser ? 'AUTHENTICATED_AI' : 'GUEST',
        prompt: initialQ,
        query: initialQ,
        language: initialLang,
        label: initialLabel,
      }, initialPage);
    }
  }, [authLoading]);

  // Form submit handler
  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (mode === 'AUTHENTICATED_AI' && !currentUser) {
      setErrorState({
        status: 401,
        type: 'auth',
        title: 'AUTHENTICATION REQUIRED FOR AI DISCOVERY',
        message:
          'AI-powered semantic search requires an authenticated account. You can switch to Guest Mode below to browse issues without logging in, or sign in to receive 20 daily AI searches.',
      });
      return;
    }
    executeSearch({ mode }, 1);
  };

  // Pagination navigation handler
  const handlePageChange = (newPage) => {
    if (loading || newPage === page) return;
    if (newPage < 1 || newPage > totalPages) return;
    executeSearch(activeParams || { mode }, newPage);
    window.scrollTo({ top: 380, behavior: 'smooth' });
  };

  // Save / Bookmark toggle
  const handleToggleSave = async (issue) => {
    if (!issue || !issue.id) return;
    if (!currentUser) {
      navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.EXPLORE)}`);
      return;
    }
    const issueIdStr = String(issue.id);
    const existingSaved = savedList.find((s) => String(s.githubIssueId) === issueIdStr);
    try {
      if (existingSaved) {
        await savedIssueService.deleteSavedIssue(existingSaved.id);
        setSavedList((prev) => prev.filter((s) => s.id !== existingSaved.id));
        setSavedIds((prev) => prev.filter((id) => id !== issueIdStr));
        showToast('REMOVED ISSUE FROM SAVED CRATE');
      } else {
        const repoFullName =
          issue.repositoryFullName ||
          issue.repository?.fullName ||
          (issue.repo ? issue.repo : 'unknown/repo');
        const newSaved = await savedIssueService.saveIssue({
          githubIssueId: issue.id,
          repositoryFullName: repoFullName,
          issueNumber: issue.number,
          title: issue.title,
          issueUrl: issue.htmlUrl || issue.url || `https://github.com/${repoFullName}/issues/${issue.number}`,
        });
        setSavedList((prev) => [...prev, newSaved]);
        setSavedIds((prev) => [...prev, issueIdStr]);
        showToast('SAVED ISSUE TO CRATE');
      }
    } catch (err) {
      if (err?.status === 409) {
        // Issue is already in crate on backend - reconcile local state
        setSavedIds((prev) => (prev.includes(issueIdStr) ? prev : [...prev, issueIdStr]));
        showToast('ISSUE IS ALREADY IN CRATE');
        try {
          const freshList = await savedIssueService.getSavedIssues();
          setSavedList(freshList);
        } catch {
          // ignore
        }
      } else {
        console.error('Failed to toggle save issue:', err);
        showToast('ERROR: FAILED TO UPDATE SAVED CRATE');
      }
    }
  };

  // Copy selected issue URL
  const handleCopyLink = () => {
    if (!selectedMatch?.issue?.htmlUrl) return;
    navigator.clipboard.writeText(selectedMatch.issue.htmlUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  // Reset to initial state
  const handleResetSearch = () => {
    setQuery('');
    setSelectedLanguage('ALL');
    setSelectedLabel('good first issue');
    setSelectedSort('updated');
    setResults([]);
    setParsedFilter(null);
    setTotalCount(0);
    setAccessibleCount(0);
    setBackendTotalPages(0);
    setHasSearched(false);
    setErrorState(null);
    setValidationError(null);
    setSelectedMatch(null);
    setActiveParams(null);
    setSearchParams({}, { replace: true });
  };

  const isSelectedSaved = selectedMatch?.issue?.id
    ? savedIds.includes(String(selectedMatch.issue.id))
    : false;

  const accessibleTotal = accessibleCount > 0 ? accessibleCount : Math.min(totalCount, 1000);
  const totalPages = backendTotalPages > 0
    ? backendTotalPages
    : (accessibleTotal > 0 ? Math.ceil(accessibleTotal / perPage) : 1);

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* ============================================================== */}
      {/* 1. TOP TICKER STRIP / MODE STATUS (Mustard #F4C13D)             */}
      {/* ============================================================== */}
      <section className="w-full bg-[#F4C13D] text-[#0D0D0D] px-4 md:px-8 py-1.5 flex items-center justify-between overflow-hidden text-xs font-mono font-bold uppercase tracking-wider select-none">
        <div className="flex items-center space-x-3 whitespace-nowrap">
          <span className="inline-flex items-center space-x-1.5 bg-[#0D0D0D] text-[#F4C13D] px-2 py-0.5 text-[10px]">
            <span className="w-1.5 h-1.5 bg-[#F4C13D] animate-pulse inline-block" />
            <span>DISCOVERY ENGINE</span>
          </span>
          <span className="opacity-90 hidden md:inline text-[11px]">
            {mode === 'AUTHENTICATED_AI'
              ? '· MODE: AI SEMANTIC SEARCH · 20 DAILY QUOTA · RELEVANCE RANKING'
              : '· MODE: PUBLIC GUEST BROWSE · UNLIMITED ACCESS · NO LOGIN REQUIRED'}
          </span>
        </div>
        <div className="flex items-center space-x-3 text-[10px]">
          {mode === 'AUTHENTICATED_AI' && remainingDailyQuota !== null && (
            <span className="bg-[#0D0D0D] text-[#F4C13D] px-2 py-0.5 font-bold">
              AI QUOTA: {remainingDailyQuota}/20 REMAINING
            </span>
          )}
          {mode === 'GUEST' && (
            <span className="bg-[#0D0D0D] text-[#5B86F2] px-2 py-0.5 font-bold">
              GUEST: PUBLIC BROWSE (0 QUOTA)
            </span>
          )}
        </div>
      </section>

      {/* ============================================================== */}
      {/* 2. WORKSPACE HEADER & TITLE BANNER                             */}
      {/* ============================================================== */}
      <section className="w-full px-4 md:px-8 py-5 md:py-6 bg-[#0E0E0E] border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-end justify-between gap-4">
          <div className="space-y-1.5 max-w-3xl">
            <div className="flex items-center space-x-2 font-mono text-xs">
              <span className="text-[#F4C13D] font-bold uppercase tracking-widest">
                DISCOVERY / {mode === 'AUTHENTICATED_AI' ? 'AI SEMANTIC' : 'PUBLIC GUEST'}
              </span>
              <span className="w-1.5 h-1.5 bg-[#2E2E2E] inline-block" />
              <span className="text-[#A7A29A] uppercase tracking-wider">
                GITHUB ISSUE FINDER
              </span>
            </div>

            <h1 className="font-heading text-3xl sm:text-4xl md:text-5xl uppercase text-[#F1ECE2] tracking-tight leading-none">
              FIND OPEN SOURCE ISSUES.
            </h1>

            <p className="font-mono text-xs md:text-sm text-[#A7A29A] leading-relaxed max-w-2xl">
              {mode === 'AUTHENTICATED_AI'
                ? 'Describe your tech stack, skills, or target tasks in natural language to discover beginner-friendly GitHub issues and contribution opportunities ranked by AI.'
                : 'Find beginner-friendly open source issues and good first issues across verified GitHub repositories by keyword, programming language, and maintainer label.'}
            </p>
          </div>

          {/* User Session & Quota Ledger Widget */}
          <div className="bg-[#141414] border border-[#2E2E2E] p-3 sm:p-3.5 flex items-center space-x-6 sm:space-x-8 self-start lg:self-auto font-mono">
            <div className="flex flex-col">
              <span className="text-[10px] text-[#A7A29A] uppercase tracking-wider">
                {mode === 'AUTHENTICATED_AI' ? 'DAILY QUOTA' : 'COST'}
              </span>
              <span className="font-heading text-xl text-[#F4C13D]">
                {mode === 'AUTHENTICATED_AI'
                  ? remainingDailyQuota !== null
                    ? `${remainingDailyQuota}/20`
                    : '20'
                  : 'FREE'}
              </span>
            </div>
            <div className="w-px h-8 bg-[#2E2E2E]" />
            <div className="flex flex-col">
              <span className="text-[10px] text-[#A7A29A] uppercase tracking-wider">MATCHED</span>
              <span className="font-heading text-xl text-[#5B86F2]">
                {hasSearched ? totalCount : '—'}
              </span>
            </div>
            <div className="w-px h-8 bg-[#2E2E2E]" />
            <div className="flex flex-col">
              <span className="text-[10px] text-[#A7A29A] uppercase tracking-wider">MODE</span>
              <span className="font-heading text-xs text-[#4ADE80] uppercase truncate">
                {mode === 'AUTHENTICATED_AI' ? 'AI SEMANTIC' : 'GUEST BROWSE'}
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 3. MODE SELECTOR & AUTHENTICATION BANNER                       */}
      {/* ============================================================== */}
      <section className="w-full bg-[#1A1A1A] border-b border-[#2E2E2E] px-4 md:px-8 py-2.5">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 font-mono text-xs">
          {/* Mode Indicator & Toggle */}
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-[#A7A29A] uppercase text-[11px] font-bold">DISCOVERY MODE:</span>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setMode('GUEST')}
                className={`px-3 py-1 uppercase text-xs font-bold border transition-colors ${
                  mode === 'GUEST'
                    ? 'bg-[#5B86F2] text-[#0D0D0D] border-[#5B86F2]'
                    : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
                }`}
              >
                GUEST BROWSE (PUBLIC)
              </button>

              <button
                type="button"
                onClick={() => {
                  if (currentUser) {
                    setMode('AUTHENTICATED_AI');
                  } else {
                    setErrorState({
                      status: 401,
                      type: 'auth',
                      title: 'AUTHENTICATION REQUIRED FOR AI DISCOVERY',
                      message:
                        'AI semantic discovery requires an authenticated account to reserve daily quota (20 searches/day). Please sign in with GitHub.',
                    });
                  }
                }}
                className={`px-3 py-1 uppercase text-xs font-bold border flex items-center gap-1.5 transition-colors ${
                  mode === 'AUTHENTICATED_AI'
                    ? 'bg-[#F4C13D] text-[#0D0D0D] border-[#F4C13D]'
                    : 'bg-[#141414] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2]'
                }`}
              >
                <Sparkles size={12} />
                <span>AI SEMANTIC SEARCH</span>
                {!currentUser && <span className="text-[10px] opacity-75">(LOGIN REQ)</span>}
              </button>
            </div>
          </div>

          {/* Right Action: Sign-in or Status */}
          {!currentUser && !authLoading ? (
            <a
              href={getOAuthLoginUrl()}
              className="inline-flex items-center gap-1.5 px-3 py-1 bg-[#F4C13D] text-[#0D0D0D] font-bold uppercase text-[11px] hover:bg-[#F06522] transition-colors shrink-0"
            >
              <LogIn size={13} />
              <span>SIGN IN TO UNLOCK 20 AI SEARCHES</span>
            </a>
          ) : (
            <span className="text-[#4ADE80] text-[11px] font-bold uppercase hidden md:inline">
              ✓ AUTHENTICATED CONTRIBUTOR ({currentUser?.username || 'user'})
            </span>
          )}
        </div>
      </section>

      {/* ============================================================== */}
      {/* 4. SEARCH COMMAND & FILTER FORM                                */}
      {/* ============================================================== */}
      <section className="w-full bg-[#141414] px-4 md:px-8 py-4 border-b border-[#2E2E2E]">
        <div className="max-w-7xl mx-auto space-y-3">
          <form onSubmit={handleFormSubmit} className="space-y-2.5">
            {/* Input Bar */}
            <div className="bg-[#0E0E0E] border-2 border-[#2E2E2E] p-1.5 flex flex-col md:flex-row items-stretch gap-2 focus-within:border-[#F06522] transition-colors">
              <div className="flex items-center gap-2 px-3 py-1 flex-1 bg-[#141414]">
                <Terminal size={18} className="text-[#F4C13D] shrink-0" />
                <span className="font-mono text-[#F4C13D] font-bold text-base select-none">&gt;</span>
                <input
                  id="issue-search-input"
                  name="q"
                  type="text"
                  aria-label="Find beginner-friendly open source issues by keyword or natural language"
                  value={query}
                  onChange={(e) => {
                    setQuery(e.target.value);
                    if (validationError) setValidationError(null);
                  }}
                  placeholder={
                    mode === 'AUTHENTICATED_AI'
                      ? currentUser
                        ? "Describe your skills & tasks... e.g. 'Find beginner-friendly Java issues with good first issue labels in active repos'"
                        : "Sign in with GitHub to use AI Semantic Search (or switch to Guest Browse)..."
                      : "Find open source issues by keyword (e.g. 'kafka', 'bug', 'docs') or select filters below..."
                  }
                  className="w-full bg-transparent font-mono text-sm text-[#F1ECE2] placeholder:text-[#666666] focus:outline-none"
                  disabled={loading}
                />
                {query && (
                  <button
                    type="button"
                    onClick={() => {
                      setQuery('');
                      setValidationError(null);
                    }}
                    className="text-[#A7A29A] hover:text-[#F1ECE2] p-1"
                    title="Clear query"
                  >
                    <X size={16} />
                  </button>
                )}
              </div>

              <div className="flex items-center shrink-0">
                <Button
                  type="submit"
                  variant={mode === 'AUTHENTICATED_AI' ? 'primary' : 'blue'}
                  size="md"
                  disabled={loading}
                  className="w-full md:w-auto flex items-center justify-center gap-2"
                >
                  {mode === 'AUTHENTICATED_AI' ? <Sparkles size={16} /> : <Search size={16} />}
                  <span>
                    {loading
                      ? 'SEARCHING...'
                      : mode === 'AUTHENTICATED_AI'
                      ? currentUser
                        ? 'DISCOVER ISSUES (AI)'
                        : 'SIGN IN FOR AI SEARCH'
                      : 'DISCOVER ISSUES'}
                  </span>
                </Button>
              </div>
            </div>

            {/* AI Authentication Prompt for Guests */}
            {mode === 'AUTHENTICATED_AI' && !currentUser && (
              <div className="bg-[#1A1414] border border-[#93000A] px-3.5 py-2 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono text-[#FF8585]">
                <div className="flex items-center gap-2">
                  <ShieldAlert size={15} className="shrink-0 text-[#FF8585]" />
                  <span>AI Semantic Search requires GitHub authentication (20 searches/day).</span>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <a
                    href={getOAuthLoginUrl()}
                    className="px-2.5 py-0.5 bg-[#F4C13D] text-[#0D0D0D] font-bold text-[11px] uppercase hover:bg-[#F06522] transition-colors"
                  >
                    Sign In With GitHub
                  </a>
                  <button
                    type="button"
                    onClick={() => {
                      setMode('GUEST');
                      setErrorState(null);
                    }}
                    className="text-[#A7A29A] hover:text-[#F1ECE2] text-[11px] underline uppercase"
                  >
                    Switch to Guest Mode
                  </button>
                </div>
              </div>
            )}

            {/* Guest Mode Filter Toolbar */}
            {mode === 'GUEST' && (
              <div className="flex flex-wrap items-center gap-3 pt-0.5 font-mono text-xs">
                {/* Language Filter */}
                <div className="flex items-center gap-1.5">
                  <label htmlFor="lang-filter" className="text-[11px] text-[#A7A29A] uppercase font-bold">LANG:</label>
                  <select
                    id="lang-filter"
                    value={selectedLanguage}
                    onChange={(e) => setSelectedLanguage(e.target.value)}
                    className="bg-[#1C1B1B] border border-[#2E2E2E] text-xs px-2.5 py-1 text-[#F1ECE2] focus:outline-none focus:border-[#5B86F2] uppercase font-bold"
                  >
                    {LANGUAGES.map((l) => (
                      <option key={l} value={l}>
                        {l}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Label Filter */}
                <div className="flex items-center gap-1.5">
                  <label htmlFor="label-filter" className="text-[11px] text-[#A7A29A] uppercase font-bold">LABEL:</label>
                  <select
                    id="label-filter"
                    value={selectedLabel}
                    onChange={(e) => setSelectedLabel(e.target.value)}
                    className="bg-[#1C1B1B] border border-[#2E2E2E] text-xs px-2.5 py-1 text-[#F4C13D] focus:outline-none focus:border-[#5B86F2] uppercase font-bold"
                  >
                    {LABELS.map((lbl) => (
                      <option key={lbl} value={lbl}>
                        {lbl}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Sort Order */}
                <div className="flex items-center gap-1.5">
                  <label htmlFor="sort-filter" className="text-[11px] text-[#A7A29A] uppercase font-bold">SORT:</label>
                  <select
                    id="sort-filter"
                    value={selectedSort}
                    onChange={(e) => setSelectedSort(e.target.value)}
                    className="bg-[#1C1B1B] border border-[#2E2E2E] text-xs px-2.5 py-1 text-[#5B86F2] focus:outline-none focus:border-[#5B86F2] uppercase font-bold"
                  >
                    {SORTS.map((s) => (
                      <option key={s.value} value={s.value}>
                        {s.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            )}

            {/* Validation Error Feedback */}
            {validationError && (
              <p className="font-mono text-xs text-[#FF8585] flex items-center gap-1.5 pl-1">
                <AlertTriangle size={13} />
                <span>{validationError}</span>
              </p>
            )}
          </form>

          {/* Active Criteria Ribbon (Compact & Visually Subtle) */}
          {parsedFilter && (
            <div className="bg-[#111111] border border-[#222222] px-3 py-1.5 flex flex-wrap items-center justify-between gap-2 text-xs font-mono text-[#A7A29A]">
              <div className="flex flex-wrap items-center gap-1.5 text-[11px]">
                <span className="text-[#888888] font-semibold uppercase flex items-center gap-1">
                  {mode === 'AUTHENTICATED_AI' ? (
                    <Sparkles size={12} className="text-[#F4C13D]" />
                  ) : (
                    <CheckCircle2 size={12} className="text-[#4ADE80]" />
                  )}
                  <span>{mode === 'AUTHENTICATED_AI' ? 'CRITERIA:' : 'ACTIVE CRITERIA:'}</span>
                </span>
                {parsedFilter.language && (
                  <span className="bg-[#181818] border border-[#2A2A2A] px-1.5 py-0.5 text-[#CCCCCC] uppercase">
                    <span className="text-[#777777]">lang:</span>{' '}
                    <strong className="text-[#F1ECE2] font-semibold">{parsedFilter.language}</strong>
                  </span>
                )}
                {parsedFilter.keywords && (
                  <span className="bg-[#181818] border border-[#2A2A2A] px-1.5 py-0.5 text-[#CCCCCC] uppercase">
                    <span className="text-[#777777]">q:</span>{' '}
                    <strong className="text-[#F1ECE2] font-semibold">{parsedFilter.keywords}</strong>
                  </span>
                )}
                {parsedFilter.repository && (
                  <span className="bg-[#181818] border border-[#2A2A2A] px-1.5 py-0.5 text-[#CCCCCC] uppercase">
                    <span className="text-[#777777]">repo:</span>{' '}
                    <strong className="text-[#F1ECE2] font-semibold">{parsedFilter.repository}</strong>
                  </span>
                )}
                {parsedFilter.labels && parsedFilter.labels.length > 0 && (
                  <span className="bg-[#181818] border border-[#2A2A2A] px-1.5 py-0.5 text-[#CCCCCC] uppercase">
                    <span className="text-[#777777]">label:</span>{' '}
                    <strong className="text-[#F1ECE2] font-semibold">{parsedFilter.labels.join(', ')}</strong>
                  </span>
                )}
              </div>

              <button
                type="button"
                onClick={handleResetSearch}
                className="text-[#888888] hover:text-[#F1ECE2] underline text-[10px] uppercase font-bold ml-auto transition-colors"
              >
                CLEAR
              </button>
            </div>
          )}
        </div>
      </section>

      {/* ============================================================== */}
      {/* 5. MAIN CONTENT DISPLAY (RESULTS / SKELETONS / ERRORS / EMPTY)  */}
      {/* ============================================================== */}
      <section className="w-full px-4 md:px-8 py-5 md:py-6 flex-1 max-w-7xl mx-auto">
        {/* State A: Loading Skeletons */}
        {loading && (
          <div className="space-y-6">
            <div className="bg-[#141414] border border-[#2E2E2E] px-4 py-3 flex items-center justify-between font-mono text-xs">
              <span className="text-[#F4C13D] font-bold uppercase flex items-center gap-2">
                <span className="w-2 h-2 bg-[#F06522] animate-ping inline-block" />
                <span>
                  {mode === 'AUTHENTICATED_AI'
                    ? 'DISCOVERING MATCHING ISSUES VIA AI SEMANTIC SEARCH...'
                    : 'SEARCHING OPEN SOURCE ISSUES & VERIFYING REPOSITORIES...'}
                </span>
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <IssueSkeleton />
              <IssueSkeleton />
              <IssueSkeleton />
              <IssueSkeleton />
            </div>
          </div>
        )}

        {/* State B: Error State Presentation */}
        {!loading && errorState && (
          <div className="max-w-2xl mx-auto my-8 border border-[#93000A] bg-[#1A1414] p-6 md:p-8 space-y-5 font-mono">
            <div className="flex items-start space-x-3">
              {errorState.type === 'auth' ? (
                <ShieldAlert className="w-8 h-8 text-[#FF8585] shrink-0 mt-0.5" />
              ) : (
                <AlertTriangle className="w-8 h-8 text-[#FF8585] shrink-0 mt-0.5" />
              )}
              <div className="space-y-1">
                <span className="text-[11px] text-[#FF8585] uppercase tracking-widest font-bold">
                  ERROR {errorState.status} // {errorState.type.toUpperCase()}
                </span>
                <h3 className="font-heading text-2xl uppercase text-[#FFDAD6]">
                  {errorState.title}
                </h3>
              </div>
            </div>

            <p className="text-xs md:text-sm text-[#E2D2D2] leading-relaxed">
              {errorState.message}
            </p>

            <div className="flex flex-wrap gap-3 pt-2">
              {errorState.type === 'auth' && (
                <>
                  <a href={getOAuthLoginUrl()} className="inline-block">
                    <Button variant="yellow" size="md" className="flex items-center gap-2">
                      <LogIn size={15} />
                      <span>SIGN IN WITH GITHUB</span>
                    </Button>
                  </a>
                  <Button
                    variant="blue"
                    size="md"
                    onClick={() => {
                      setMode('GUEST');
                      setErrorState(null);
                      executeSearch({ mode: 'GUEST' }, 1);
                    }}
                  >
                    CONTINUE IN GUEST MODE
                  </Button>
                </>
              )}

              {errorState.type !== 'auth' && (
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => executeSearch(activeParams || { mode }, page)}
                  className="flex items-center gap-2"
                >
                  <RotateCcw size={15} />
                  <span>RETRY QUERY</span>
                </Button>
              )}

              <Button variant="secondary" size="md" onClick={handleResetSearch}>
                RESET SEARCH
              </Button>
            </div>
          </div>
        )}

        {/* State C: Empty Search Results (Zero Issues Found) */}
        {!loading && !errorState && hasSearched && results.length === 0 && (
          <div className="max-w-2xl mx-auto my-12 border border-[#2E2E2E] bg-[#141414] p-8 text-center space-y-5 font-mono">
            <div className="w-14 h-14 bg-[#1F1F1F] text-[#F4C13D] flex items-center justify-center mx-auto border border-[#2E2E2E]">
              <Compass size={28} />
            </div>

            <div className="space-y-1">
              <span className="text-[11px] text-[#A7A29A] uppercase tracking-wider">
                QUERY COMPLETED WITH 0 RESULTS
              </span>
              <h2 className="font-heading text-3xl uppercase text-[#F1ECE2]">
                ZERO MATCHING ISSUES FOUND
              </h2>
            </div>

            <p className="text-xs text-[#A7A29A] max-w-lg mx-auto leading-relaxed">
              GitHub returned no open issues matching all criteria simultaneously.
            </p>

            <div className="bg-[#0E0E0E] border border-[#262626] p-4 text-left space-y-2 text-xs">
              <span className="text-[#F1ECE2] font-bold uppercase text-[11px]">
                RECOMMENDED SEARCH ADJUSTMENTS:
              </span>
              <ul className="list-disc list-inside space-y-1 text-[#A7A29A] text-[11px]">
                <li>Broaden or remove restrictive keywords.</li>
                <li>Try selecting standard entry labels like "good first issue" or "help wanted".</li>
                <li>Try selecting core programming languages (e.g. Java, TypeScript, Python, Go).</li>
              </ul>
            </div>

            <div className="flex justify-center gap-3 pt-2">
              <Button variant="yellow" size="md" onClick={handleResetSearch}>
                START NEW SEARCH
              </Button>
            </div>
          </div>
        )}

        {/* State D: Initial Unsearched State (Welcome Hero) */}
        {!loading && !errorState && !hasSearched && (
          <div className="border border-[#2E2E2E] bg-[#141414] p-8 md:p-12 space-y-8 font-mono">
            <div className="max-w-2xl space-y-3">
              <span className="text-xs uppercase text-[#F4C13D] font-bold tracking-widest flex items-center gap-1.5">
                <Sparkles size={14} />
                <span>OPEN SOURCE CONTRIBUTOR DISCOVERY ENGINE</span>
              </span>
              <h2 className="font-heading text-3xl md:text-5xl uppercase text-[#F1ECE2] leading-tight">
                FIND YOUR NEXT OPEN SOURCE CONTRIBUTION.
              </h2>
              <p className="text-xs md:text-sm text-[#A7A29A] leading-relaxed">
                Search verified open-source projects and beginner-friendly GitHub issues to contribute to. Filter by programming language, maintainer labels like "good first issue", or use AI semantic search to find contribution opportunities that fit your tech stack.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-4 space-y-2">
                <span className="text-[#5B86F2] font-bold uppercase text-[11px] block">
                  1. PUBLIC GUEST DISCOVERY
                </span>
                <p className="text-[#A7A29A] leading-relaxed text-[11px]">
                  Unauthenticated, fast GitHub REST browsing. Zero AI quota cost, factual metadata, and verified repository languages.
                </p>
              </div>

              <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-4 space-y-2">
                <span className="text-[#F4C13D] font-bold uppercase text-[11px] block">
                  2. AI SEMANTIC INTERPRETATION
                </span>
                <p className="text-[#A7A29A] leading-relaxed text-[11px]">
                  Translates conversational prompts into structured search criteria for authenticated contributors.
                </p>
              </div>

              <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-4 space-y-2">
                <span className="text-[#4ADE80] font-bold uppercase text-[11px] block">
                  3. FACTUAL REPOSITORY AUDITING
                </span>
                <p className="text-[#A7A29A] leading-relaxed text-[11px]">
                  Verifies repository primary languages directly from GitHub metadata and excludes spam, job ads, and study logs.
                </p>
              </div>
            </div>

            <div className="pt-2 border-t border-[#222222] text-[#A7A29A] text-xs">
              <span>Use the search bar and filters above to find beginner-friendly open source issues and GitHub contribution opportunities.</span>
            </div>
          </div>
        )}

        {/* State E: Active Results Display (Cards + Sticky Side Inspector) */}
        {!loading && !errorState && hasSearched && results.length > 0 && (
          <div className="grid grid-cols-1 xl:grid-cols-12 gap-8 items-start">
            {/* Left Column: Issue Results Rack (8 Cols) */}
            <div className="xl:col-span-8 space-y-4">
              {/* Header Summary Bar */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-[#141414] border border-[#2E2E2E] px-4 py-3 font-mono text-xs gap-3">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="font-heading text-xl uppercase text-[#F1ECE2]">
                    MATCHED: {totalCount.toLocaleString()} ISSUES
                  </span>
                  <span className="bg-[#201F1F] text-[#A7A29A] border border-[#2E2E2E] px-2 py-0.5 text-[11px] uppercase">
                    PAGE {page} OF {totalPages}
                  </span>
                  {totalCount > accessibleTotal && (
                    <span className="text-[#F4C13D] border border-[#F4C13D]/40 bg-[#1A1812] px-2 py-0.5 text-[10px] uppercase font-bold tracking-wider">
                      FIRST {accessibleTotal.toLocaleString()} ACCESSIBLE VIA GITHUB SEARCH
                    </span>
                  )}
                </div>
                <div className="hidden sm:flex items-center space-x-2 text-[#A7A29A]">
                  <span
                    className={`w-1.5 h-1.5 inline-block ${
                      mode === 'AUTHENTICATED_AI' ? 'bg-[#F4C13D]' : 'bg-[#5B86F2]'
                    }`}
                  />
                  <span>
                    {mode === 'AUTHENTICATED_AI'
                      ? 'AI RELEVANCE RANKING'
                      : 'CHRONOLOGICAL / RECENT ACTIVITY'}
                  </span>
                </div>
              </div>

              {/* Informative banner when search window exceeds GitHub's 1,000 accessible result cap */}
              {totalCount > accessibleTotal && (
                <div className="bg-[#141414] border border-[#2E2E2E] px-4 py-2.5 font-mono text-[11px] text-[#A7A29A] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <span>
                    GitHub Search API limits accessible results to the first 1,000 matches. Use specific language, label, or repository filters to narrow catalog scope.
                  </span>
                  <span className="text-[#F4C13D] uppercase font-bold text-[10px] whitespace-nowrap">
                    MAX {totalPages} ACCESSIBLE PAGES
                  </span>
                </div>
              )}

              {/* Cards Grid */}
              <div className="grid grid-cols-1 gap-4">
                {results.map((match) => (
                  <IssueCard
                    key={`${match.issue?.repositoryFullName || 'repo'}-${match.issue?.number || match.issue?.id}`}
                    issueMatch={match}
                    isSelected={selectedMatch?.issue?.id === match.issue?.id}
                    isSaved={savedIds.includes(String(match.issue?.id))}
                    onSelect={(m) => setSelectedMatch(m)}
                    onToggleSave={handleToggleSave}
                  />
                ))}
              </div>

              {/* Bottom Pagination Bar */}
              <div className="bg-[#141414] border border-[#2E2E2E] p-3 flex flex-col sm:flex-row items-center justify-between gap-3 font-mono text-xs">
                <span className="text-[#A7A29A] uppercase text-[11px]">
                  DISPLAYING {results.length} OF {accessibleTotal.toLocaleString()} ACCESSIBLE ISSUES
                  {totalCount > accessibleTotal && (
                    <span className="text-[#666666] ml-1">
                      ({totalCount.toLocaleString()} TOTAL ON GITHUB)
                    </span>
                  )}
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={!hasPrevious || page <= 1 || loading}
                    onClick={() => handlePageChange(Math.max(1, page - 1))}
                    className="disabled:opacity-40"
                  >
                    PREVIOUS
                  </Button>
                  <span className="px-3 py-1 bg-[#F4C13D] text-[#0D0D0D] font-bold text-xs">
                    {page} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={!hasNext || page >= totalPages || loading}
                    onClick={() => handlePageChange(page + 1)}
                    className="disabled:opacity-40"
                  >
                    NEXT
                  </Button>
                </div>
              </div>
            </div>

            {/* Right Column: Sticky Issue Inspector (4 Cols) */}
            <div className="xl:col-span-4 static xl:sticky xl:top-[96px] space-y-4">
              {selectedMatch && selectedMatch.issue ? (
                <div
                  ref={detailsPanelRef}
                  className="border border-[#2E2E2E] bg-[#141414] p-5 md:p-6 space-y-5 max-h-none xl:max-h-[calc(100vh-116px)] overflow-visible xl:overflow-y-auto xl:overflow-x-hidden overscroll-contain custom-scrollbar"
                >
                  {/* Inspector Header Ribbon */}
                  <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 font-mono text-xs">
                    <span className="bg-[#5B86F2] text-[#0D0D0D] px-2 py-0.5 font-bold uppercase text-[11px]">
                      GITHUB ISSUE #{selectedMatch.issue.number}
                    </span>
                    <span className="text-[#4ADE80] font-bold uppercase text-[11px]">
                      {selectedMatch.issue.state || 'OPEN'}
                    </span>
                  </div>

                  {/* Repo & Title */}
                  <div className="space-y-1">
                    <span className="font-mono text-xs text-[#F4C13D] uppercase font-bold tracking-wider break-words block">
                      {selectedMatch.issue.repositoryFullName}
                    </span>
                    <h2 className="font-heading text-2xl uppercase text-[#F1ECE2] leading-tight m-0 break-words">
                      {selectedMatch.issue.title}
                    </h2>
                    <div className="flex items-center space-x-2 text-[11px] font-mono text-[#A7A29A] pt-1">
                      <span>
                        Opened by <strong className="text-[#F1ECE2]">{selectedMatch.issue.authorUsername || 'contributor'}</strong>
                      </span>
                      <span>•</span>
                      <span>{selectedMatch.issue.commentsCount || 0} comments</span>
                    </div>
                  </div>

                  {/* AI Match Scoring Card or Factual Criteria Card */}
                  <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-4 space-y-2 font-mono text-xs">
                    <div className="flex items-center justify-between">
                      <span className="text-[#F4C13D] font-bold uppercase flex items-center gap-1.5">
                        <Sparkles size={14} />
                        <span>
                          {selectedMatch.matchScore !== null && selectedMatch.matchScore !== undefined
                            ? 'AI MATCH EXPLANATION'
                            : 'FACTUAL MATCH CRITERIA'}
                        </span>
                      </span>
                      {selectedMatch.matchScore !== null && selectedMatch.matchScore !== undefined ? (
                        <span className="font-heading text-xl text-[#F4C13D]">
                          {Math.round(selectedMatch.matchScore)}%
                        </span>
                      ) : (
                        <span className="text-[11px] text-[#5B86F2] font-bold uppercase">
                          VERIFIED
                        </span>
                      )}
                    </div>
                    <p className="text-[#A7A29A] text-xs leading-relaxed break-words">
                      {selectedMatch.matchExplanation}
                    </p>

                    {/* Matched Criteria Chips */}
                    {selectedMatch.matchedCriteria && selectedMatch.matchedCriteria.length > 0 && (
                      <div className="flex flex-wrap gap-1 pt-1">
                        {selectedMatch.matchedCriteria.map((criterion, idx) => (
                          <span
                            key={idx}
                            className="bg-[#0E0E0E] text-[#5B86F2] border border-[#2E2E2E] px-1.5 py-0.5 text-[10px] uppercase font-bold break-words"
                          >
                            ✓ {criterion}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Inspection Metadata Grid */}
                  <div className="grid grid-cols-2 gap-2 font-mono text-xs">
                    <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 flex flex-col">
                      <span className="text-[#666666] text-[10px] uppercase">COMMENTS</span>
                      <span className="font-bold text-[#F1ECE2] uppercase truncate">
                        {selectedMatch.issue.commentsCount || 0} RESPONSES
                      </span>
                    </div>
                    <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-2.5 flex flex-col">
                      <span className="text-[#666666] text-[10px] uppercase">STATUS</span>
                      <span className="font-bold text-[#4ADE80] uppercase truncate">
                        VERIFIED OPEN
                      </span>
                    </div>
                  </div>

                  {/* Issue Labels */}
                  {selectedMatch.issue.labels && selectedMatch.issue.labels.length > 0 && (
                    <div className="space-y-1.5 font-mono text-xs">
                      <span className="text-[11px] text-[#A7A29A] uppercase">GITHUB LABELS:</span>
                      <div className="flex flex-wrap gap-1">
                        {selectedMatch.issue.labels.map((l) => (
                          <span
                            key={l.id || l.name}
                            style={{
                              backgroundColor: l.color ? `#${l.color.replace('#', '')}` : '#2E2E2E',
                              color: '#FFFFFF',
                            }}
                            className="px-2 py-0.5 text-[10px] uppercase font-bold border border-black/20 break-words"
                          >
                            {l.name}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Actions Cluster */}
                  <div className="space-y-2 pt-2">
                    <a
                      href={selectedMatch.issue.htmlUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block"
                    >
                      <Button variant="primary" size="md" className="w-full flex items-center justify-center gap-1.5">
                        <ExternalLink size={15} />
                        <span>OPEN ON GITHUB</span>
                      </Button>
                    </a>

                    <div className="grid grid-cols-2 gap-2">
                      <Button
                        variant="secondary"
                        size="md"
                        onClick={() => handleToggleSave(selectedMatch.issue)}
                        className="w-full flex items-center justify-center gap-1.5"
                      >
                        <Bookmark
                          size={14}
                          fill={isSelectedSaved ? '#F06522' : 'none'}
                          className={isSelectedSaved ? 'text-[#F06522]' : ''}
                        />
                        <span>{isSelectedSaved ? 'IN CRATE' : 'SAVE TO CRATE'}</span>
                      </Button>

                      <Button
                        variant="outline"
                        size="md"
                        onClick={handleCopyLink}
                        className="w-full flex items-center justify-center gap-1.5"
                      >
                        {copiedLink ? <Check size={14} className="text-[#4ADE80]" /> : <Copy size={14} />}
                        <span>{copiedLink ? 'COPIED' : 'COPY LINK'}</span>
                      </Button>
                    </div>
                  </div>

                  {/* Factual Disclaimer Stamp */}
                  <div className="bg-[#0E0E0E] border border-[#2E2E2E] p-3 flex items-start gap-2.5 font-mono text-[11px] text-[#A7A29A]">
                    <span className="text-[#F4C13D] font-bold text-sm leading-none shrink-0">💡</span>
                    <p className="leading-relaxed">
                      Maintainer labels like <em>good first issue</em> indicate welcoming tasks for new repository contributors, but contribution difficulty depends on local setup and domain familiarity.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="border border-[#2E2E2E] bg-[#141414] p-6 text-center font-mono text-xs text-[#A7A29A]">
                  Select an issue card to inspect its verified metadata.
                </div>
              )}
            </div>
          </div>
        )}
      </section>

      {/* ============================================================== */}
      {/* 6. BOTTOM FOOTER STRIP                                         */}
      {/* ============================================================== */}
      <section className="w-full bg-[#0E0E0E] border-t border-[#2E2E2E] px-4 md:px-8 py-3 mt-auto">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-2 font-mono text-xs text-[#666666]">
          <div className="flex items-center space-x-2">
            <span className="w-2 h-2 bg-[#F4C13D] inline-block" />
            <span className="uppercase text-[#A7A29A]">
              REAL OPEN SOURCE OPPORTUNITIES · AUDITED METADATA
            </span>
          </div>
          <div className="flex items-center space-x-3 uppercase text-[11px] text-[#A7A29A]">
            <span>OPEN SOURCE CONTRIBUTOR DISCOVERY ENGINE</span>
          </div>
        </div>
      </section>
      {/* Toast Notification */}
      {toastMessage && (
        <div
          role="status"
          aria-live="polite"
          className="fixed bottom-6 right-6 z-50 bg-[#1A1A1A] border-2 border-[#F06522] text-[#F1ECE2] px-4 py-3 font-mono text-xs shadow-2xl flex items-center space-x-2 animate-in fade-in"
        >
          <CheckCircle2 size={16} className="text-[#F06522]" />
          <span className="font-bold">{toastMessage}</span>
        </div>
      )}
    </div>
  );
}

export default ExplorePage;
