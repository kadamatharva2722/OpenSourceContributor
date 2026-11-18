import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { getCurrentUser } from '../services/authService';
import { savedIssueService } from '../services/savedIssueService';
import { contributionService } from '../services/contributionService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  Bookmark,
  GitPullRequest,
  Sliders,
  ExternalLink,
  User,
  CheckCircle2,
  Clock,
  Trash2,
  ArrowRight,
  Play,
  AlertCircle,
  Sparkles,
  Shield,
  Loader2,
} from 'lucide-react';

const STATUS_OPTIONS = [
  { value: 'PLANNING', label: 'PLANNING' },
  { value: 'IN_PROGRESS', label: 'IN PROGRESS' },
  { value: 'PR_OPENED', label: 'PR OPENED' },
  { value: 'COMPLETED', label: 'COMPLETED' },
];

export function DashboardPage() {
  useDocumentTitle('Dashboard');
  const navigate = useNavigate();
  const [userProfile, setUserProfile] = useState(null);
  const [savedIssues, setSavedIssues] = useState([]);
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);

  // Load all real dashboard data from backend APIs
  const loadDashboardData = async () => {
    setLoading(true);
    setErrorMessage(null);

    try {
      // 1. Fetch authenticated user profile
      const user = await getCurrentUser();
      setUserProfile(user);

      // 2. Fetch user's saved issues
      const saved = await savedIssueService.getSavedIssues();
      setSavedIssues(saved);

      // 3. Fetch user's active contributions
      const conts = await contributionService.getContributions();
      setContributions(conts);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      if (err?.status === 401) {
        navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.DASHBOARD)}`, { replace: true });
        return;
      }
      setErrorMessage(err.message || 'Failed to load contributor workspace data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Update contribution status directly from dashboard
  const handleUpdateStatus = async (item, newStatus) => {
    try {
      await contributionService.updateContribution(item.id, { status: newStatus });
      await loadDashboardData();
    } catch (err) {
      console.error('Failed to update contribution status:', err);
    }
  };

  // Remove saved issue from crate
  const handleRemoveSaved = async (savedId) => {
    try {
      await savedIssueService.deleteSavedIssue(savedId);
      await loadDashboardData();
    } catch (err) {
      console.error('Failed to remove saved issue:', err);
    }
  };

  // Move saved issue directly to active contribution ledger
  const handleStartContribution = async (issue) => {
    try {
      await contributionService.createContribution({
        githubIssueId: issue.githubIssueId,
        repositoryFullName: issue.repositoryFullName,
        issueNumber: issue.issueNumber,
        title: issue.title,
        status: 'IN_PROGRESS',
        notes: 'Promoted from saved crate into active workspace.',
      });
      await loadDashboardData();
    } catch (err) {
      console.error('Failed to start contribution:', err);
    }
  };

  // Metrics computation from real backend JPA data
  const savedCount = savedIssues.length;
  const planningCount = contributions.filter((c) => c.status === 'PLANNING').length;
  const inProgressCount = contributions.filter((c) => c.status === 'IN_PROGRESS').length;
  const prOpenedCount = contributions.filter((c) => c.status === 'PR_OPENED').length;
  const completedCount = contributions.filter((c) => c.status === 'COMPLETED').length;

  const activeContributions = contributions.filter((c) => c.status !== 'COMPLETED');
  const completedContributions = contributions.filter((c) => c.status === 'COMPLETED');

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PLANNING':
        return 'bg-[#1C1B1B] text-[#A7A29A] border border-[#3E3E3E]';
      case 'IN_PROGRESS':
        return 'bg-[#F4C13D] text-[#0D0D0D] border border-[#F4C13D] font-bold';
      case 'PR_OPENED':
        return 'bg-[#5B86F2] text-[#0D0D0D] border border-[#5B86F2] font-bold';
      case 'COMPLETED':
        return 'bg-[#2E7D32] text-[#F1ECE2] border border-[#2E7D32] font-bold';
      default:
        return 'bg-[#1C1B1B] text-[#A7A29A] border border-[#2E2E2E]';
    }
  };

  const getInitials = (name, username) => {
    const raw = name || username || 'U';
    return raw
      .split(' ')
      .map((n) => n[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();
  };

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center min-h-[60vh] bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-sm w-full text-center space-y-4">
          <Loader2 size={32} className="animate-spin text-[#F06522] mx-auto" />
          <div className="font-mono text-xs text-[#F4C13D] uppercase tracking-wider font-bold">
            LOADING CONTRIBUTOR WORKSPACE...
          </div>
        </div>
      </div>
    );
  }

  const displayName = userProfile?.displayName || userProfile?.username || 'Contributor';
  const handleName = userProfile?.username ? `@${userProfile.username}` : '@contributor';

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* Error Alert Banner if any API failed */}
      {errorMessage && (
        <div className="bg-[#2A1616] border-b border-[#FF8585] text-[#FF8585] px-4 py-2 text-xs font-mono flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle size={14} />
            <span>ERROR: {errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={loadDashboardData}
            className="underline hover:text-[#F1ECE2] uppercase"
          >
            RETRY
          </button>
        </div>
      )}

      {/* ============================================================== */}
      {/* EDITORIAL DASHBOARD HEADER                                     */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] bg-[#0E0E0E] px-4 md:px-8 py-8 md:py-12">
        <div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          {/* Contributor Profile Card */}
          <div className="flex items-center space-x-4 sm:space-x-6">
            {userProfile?.avatarUrl ? (
              <img
                src={userProfile.avatarUrl}
                alt={displayName ? `${displayName}'s contributor profile avatar` : 'Contributor profile avatar'}
                decoding="async"
                width="80"
                height="80"
                className="w-16 h-16 sm:w-20 sm:h-20 aspect-square border-2 border-[#F06522] object-cover shrink-0 select-none"
              />
            ) : (
              <div className="w-16 h-16 sm:w-20 sm:h-20 bg-[#161616] border-2 border-[#F06522] flex items-center justify-center font-heading text-2xl sm:text-3xl text-[#F06522] shrink-0 select-none">
                {getInitials(userProfile?.displayName, userProfile?.username)}
              </div>
            )}

            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-mono text-xs text-[#F4C13D] uppercase tracking-widest font-bold">
                  CONTRIBUTOR PROFILE
                </span>
                <span className="bg-[#1C1B1B] text-[#00D26A] border border-[#2E2E2E] px-2 py-0.5 text-[10px] font-mono uppercase font-bold flex items-center gap-1">
                  <Shield size={10} />
                  <span>VERIFIED GITHUB SESSION</span>
                </span>
              </div>

              <h1 className="font-heading text-3xl sm:text-4xl md:text-5xl uppercase tracking-tight text-[#F1ECE2] leading-none">
                {displayName}
              </h1>

              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 font-mono text-xs text-[#A7A29A] pt-0.5">
                <span className="text-[#F1ECE2]">{handleName}</span>
                {userProfile?.bio && (
                  <>
                    <span>·</span>
                    <span className="max-w-md truncate">{userProfile.bio}</span>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* Quick Actions Cluster */}
          <div className="flex flex-wrap items-center gap-2.5 font-mono text-xs">
            <Link to={ROUTES.EXPLORE}>
              <Button variant="primary" size="md" className="flex items-center gap-1.5">
                <Sparkles size={14} />
                <span>EXPLORE ISSUES</span>
              </Button>
            </Link>
            <Link to={ROUTES.SAVED_ISSUES}>
              <Button variant="secondary" size="md" className="flex items-center gap-1.5">
                <Bookmark size={14} />
                <span>VIEW CRATE ({savedCount})</span>
              </Button>
            </Link>
            <Link to={ROUTES.MY_CONTRIBUTIONS}>
              <Button variant="secondary" size="md" className="flex items-center gap-1.5">
                <GitPullRequest size={14} />
                <span>FULL LEDGER</span>
              </Button>
            </Link>
            <Link to={ROUTES.SETTINGS}>
              <Button variant="outline" size="md" className="flex items-center gap-1.5">
                <Sliders size={14} />
                <span>SETTINGS</span>
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 3. CONTRIBUTION STATUS OVERVIEW (REAL COUNTS)                 */}
      {/* ============================================================== */}
      <section className="border-b border-[#2E2E2E] px-4 md:px-8 py-8 max-w-7xl mx-auto w-full">
        <div className="space-y-3 mb-4">
          <span className="font-mono text-xs text-[#F4C13D] font-bold uppercase tracking-widest block">
            WORKFLOW OVERVIEW
          </span>
          <h2 className="font-heading text-2xl uppercase text-[#F1ECE2] tracking-tight">
            CONTRIBUTION STATUS COUNTS
          </h2>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 font-mono">
          {/* 1. Saved In Crate */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 border-l-4 border-l-[#F06522] space-y-1">
            <div className="flex items-center justify-between text-[#A7A29A]">
              <span className="text-[10px] uppercase font-bold tracking-wider">SAVED IN CRATE</span>
              <Bookmark size={14} className="text-[#F06522]" />
            </div>
            <div className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] leading-none pt-1">
              {savedCount}
            </div>
            <span className="text-[10px] text-[#666666] block">BOOKMARKED CANDIDATES</span>
          </div>

          {/* 2. Planning */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 border-l-4 border-l-[#A7A29A] space-y-1">
            <div className="flex items-center justify-between text-[#A7A29A]">
              <span className="text-[10px] uppercase font-bold tracking-wider">PLANNING</span>
              <Clock size={14} className="text-[#A7A29A]" />
            </div>
            <div className="font-heading text-3xl sm:text-4xl text-[#F1ECE2] leading-none pt-1">
              {planningCount}
            </div>
            <span className="text-[10px] text-[#666666] block">UNDER EVALUATION</span>
          </div>

          {/* 3. In Progress */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 border-l-4 border-l-[#F4C13D] space-y-1">
            <div className="flex items-center justify-between text-[#A7A29A]">
              <span className="text-[10px] uppercase font-bold tracking-wider">IN PROGRESS</span>
              <Play size={14} className="text-[#F4C13D]" />
            </div>
            <div className="font-heading text-3xl sm:text-4xl text-[#F4C13D] leading-none pt-1">
              {inProgressCount}
            </div>
            <span className="text-[10px] text-[#666666] block">ACTIVE CODING BRANCH</span>
          </div>

          {/* 4. PR Opened */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 border-l-4 border-l-[#5B86F2] space-y-1">
            <div className="flex items-center justify-between text-[#A7A29A]">
              <span className="text-[10px] uppercase font-bold tracking-wider">PR OPENED</span>
              <GitPullRequest size={14} className="text-[#5B86F2]" />
            </div>
            <div className="font-heading text-3xl sm:text-4xl text-[#5B86F2] leading-none pt-1">
              {prOpenedCount}
            </div>
            <span className="text-[10px] text-[#666666] block">AWAITING UPSTREAM REVIEW</span>
          </div>

          {/* 5. Completed */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-4 border-l-4 border-l-[#2E7D32] space-y-1 col-span-2 sm:col-span-1">
            <div className="flex items-center justify-between text-[#A7A29A]">
              <span className="text-[10px] uppercase font-bold tracking-wider">COMPLETED</span>
              <CheckCircle2 size={14} className="text-[#2E7D32]" />
            </div>
            <div className="font-heading text-3xl sm:text-4xl text-[#2E7D32] leading-none pt-1">
              {completedCount}
            </div>
            <span className="text-[10px] text-[#666666] block">MERGED / RESOLVED</span>
          </div>
        </div>
      </section>

      {/* ============================================================== */}
      {/* 4. MAIN WORKSPACE (Active Tracker + Recent Saved Crate)         */}
      {/* ============================================================== */}
      <section className="px-4 md:px-8 py-8 max-w-7xl mx-auto w-full flex-1">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">

          {/* ------------------------------------------------------------ */}
          {/* LEFT COLUMN: Active Contribution Tracker (7 cols)            */}
          {/* ------------------------------------------------------------ */}
          <div className="lg:col-span-7 space-y-6">
            <div className="flex items-center justify-between pb-2 border-b border-[#2E2E2E]">
              <div className="flex items-center space-x-2">
                <GitPullRequest size={16} className="text-[#F06522]" />
                <h3 className="font-heading text-xl text-[#F1ECE2] uppercase tracking-tight">
                  ACTIVE CONTRIBUTION TRACKER
                </h3>
              </div>
              <Link
                to={ROUTES.MY_CONTRIBUTIONS}
                className="font-mono text-xs text-[#F4C13D] hover:underline uppercase font-bold"
              >
                OPEN LEDGER →
              </Link>
            </div>

            {/* Active Contribution Items */}
            {activeContributions.length === 0 ? (
              <div className="border border-[#2E2E2E] bg-[#141414] p-8 text-center space-y-3 font-mono">
                <AlertCircle size={24} className="mx-auto text-[#A7A29A]" />
                <div className="space-y-1">
                  <h4 className="font-heading text-lg text-[#F1ECE2] uppercase">
                    NO ACTIVE CONTRIBUTIONS IN PROGRESS
                  </h4>
                  <p className="text-xs text-[#A7A29A] max-w-md mx-auto leading-relaxed">
                    You currently have no issues being worked on. Pick an issue from your saved crate or explore open opportunities.
                  </p>
                </div>
                <div className="pt-2">
                  <Link to={ROUTES.EXPLORE}>
                    <Button variant="primary" size="sm">
                      EXPLORE ISSUES
                    </Button>
                  </Link>
                </div>
              </div>
            ) : (
              <div className="border border-[#2E2E2E] bg-[#141414] divide-y divide-[#2E2E2E] font-mono">
                {activeContributions.map((item) => (
                  <div key={item.id} className="p-4 space-y-3 hover:bg-[#1A1A1A] transition-colors">
                    {/* Top Row: Repo, Issue Number, Status Badge */}
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center space-x-2 text-xs">
                        <span className="text-[#F06522] font-bold">#{item.issueNumber}</span>
                        <span className="text-[#A7A29A]">{item.repositoryFullName}</span>
                        {item.branchName && (
                          <span className="bg-[#1C1B1B] text-[#A7A29A] px-1.5 py-0.5 text-[10px] border border-[#2E2E2E]">
                            {item.branchName}
                          </span>
                        )}
                      </div>

                      {/* Interactive Status Selector Dropdown */}
                      <div className="flex items-center space-x-2">
                        <span className="text-[10px] text-[#666666] uppercase hidden sm:inline">
                          UPDATE STATUS:
                        </span>
                        <select
                          value={item.status}
                          onChange={(e) => handleUpdateStatus(item, e.target.value)}
                          className={`text-[10px] font-mono px-2 py-1 uppercase rounded-none cursor-pointer focus:outline-none ${getStatusBadgeClass(
                            item.status
                          )}`}
                        >
                          {STATUS_OPTIONS.map((opt) => (
                            <option
                              key={opt.value}
                              value={opt.value}
                              className="bg-[#141414] text-[#F1ECE2]"
                            >
                              {opt.label}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>

                    {/* Middle Row: Title */}
                    <a
                      href={item.issueUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block group"
                    >
                      <h4 className="font-heading text-base sm:text-lg text-[#F1ECE2] group-hover:text-[#F06522] uppercase tracking-wide transition-colors leading-snug">
                        {item.title}
                      </h4>
                    </a>

                    {/* Bottom Row: Notes, PR Link, GitHub CTA */}
                    <div className="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-[#222222] text-[11px] text-[#A7A29A]">
                      <div className="truncate max-w-sm">
                        {item.notes ? (
                          <span>&gt; NOTE: {item.notes}</span>
                        ) : (
                          <span className="text-[#666666]">&gt; No notes logged yet</span>
                        )}
                      </div>

                      <div className="flex items-center space-x-3 shrink-0">
                        {item.pullRequestUrl && (
                          <a
                            href={item.pullRequestUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-[#5B86F2] hover:underline flex items-center gap-1"
                          >
                            <span>VIEW PR</span>
                            <ExternalLink size={12} />
                          </a>
                        )}
                        <a
                          href={item.issueUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-[#F4C13D] hover:underline uppercase font-bold flex items-center gap-1"
                        >
                          <span>GITHUB ISSUE</span>
                          <ExternalLink size={12} />
                        </a>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Recently Completed Contributions Section */}
            {completedContributions.length > 0 && (
              <div className="space-y-3 pt-4">
                <div className="flex items-center justify-between pb-2 border-b border-[#2E2E2E]">
                  <div className="flex items-center space-x-2">
                    <CheckCircle2 size={16} className="text-[#2E7D32]" />
                    <h3 className="font-heading text-lg text-[#F1ECE2] uppercase tracking-tight">
                      RECENTLY COMPLETED CONTRIBUTIONS ({completedContributions.length})
                    </h3>
                  </div>
                </div>

                <div className="border border-[#2E2E2E] bg-[#121A13] divide-y divide-[#1D2B1F] font-mono text-xs">
                  {completedContributions.map((c) => (
                    <div key={c.id} className="p-3.5 flex items-center justify-between gap-4">
                      <div className="min-w-0">
                        <div className="flex items-center space-x-2 mb-0.5">
                          <span className="text-[#2E7D32] font-bold">#{c.issueNumber}</span>
                          <span className="text-[#A7A29A]">{c.repositoryFullName}</span>
                          <span className="bg-[#1C3320] text-[#A5D6A7] px-1.5 py-0.2 text-[9px] font-bold uppercase">
                            COMPLETED
                          </span>
                        </div>
                        <h5 className="font-sans text-sm text-[#F1ECE2] truncate font-medium">
                          {c.title}
                        </h5>
                      </div>

                      <div className="flex items-center space-x-2 shrink-0">
                        <select
                          value={c.status}
                          onChange={(e) => handleUpdateStatus(c, e.target.value)}
                          className="text-[10px] font-mono px-2 py-0.5 bg-[#1C3320] text-[#A5D6A7] border border-[#2E7D32] rounded-none focus:outline-none"
                        >
                          {STATUS_OPTIONS.map((opt) => (
                            <option
                              key={opt.value}
                              value={opt.value}
                              className="bg-[#141414] text-[#F1ECE2]"
                            >
                              {opt.label}
                            </option>
                          ))}
                        </select>
                        <a
                          href={c.issueUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="p-1.5 border border-[#2E7D32] bg-[#162919] text-[#A5D6A7] hover:text-[#F1ECE2]"
                        >
                          <ExternalLink size={12} />
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* ------------------------------------------------------------ */}
          {/* RIGHT COLUMN: Recent Saved Crate Issues (5 cols)             */}
          {/* ------------------------------------------------------------ */}
          <div className="lg:col-span-5 space-y-6">
            <div className="flex items-center justify-between pb-2 border-b border-[#2E2E2E]">
              <div className="flex items-center space-x-2">
                <Bookmark size={16} className="text-[#F4C13D]" />
                <h3 className="font-heading text-xl text-[#F1ECE2] uppercase tracking-tight">
                  SAVED IN CRATE ({savedCount})
                </h3>
              </div>
              <Link
                to={ROUTES.SAVED_ISSUES}
                className="font-mono text-xs text-[#A7A29A] hover:text-[#F1ECE2] uppercase"
              >
                VIEW ALL →
              </Link>
            </div>

            {savedIssues.length === 0 ? (
              <div className="border border-[#2E2E2E] bg-[#141414] p-8 text-center space-y-3 font-mono">
                <Bookmark size={24} className="mx-auto text-[#666666]" />
                <div className="space-y-1">
                  <h4 className="font-heading text-base text-[#F1ECE2] uppercase">
                    YOUR CRATE IS CURRENTLY EMPTY
                  </h4>
                  <p className="text-xs text-[#A7A29A] leading-relaxed">
                    Bookmark candidate issues while browsing Explore to collect them into your workspace.
                  </p>
                </div>
                <div className="pt-1">
                  <Link to={ROUTES.EXPLORE}>
                    <Button variant="secondary" size="sm">
                      BROWSE CATALOG
                    </Button>
                  </Link>
                </div>
              </div>
            ) : (
              <div className="space-y-3 font-mono">
                {savedIssues.slice(0, 5).map((issue) => (
                  <div
                    key={issue.id}
                    className="border border-[#2E2E2E] bg-[#141414] p-4 space-y-2 hover:border-[#444444] transition-colors"
                  >
                    {/* Top Row: Repo & Badges */}
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-1.5 text-xs">
                        <span className="text-[#F06522] font-bold">#{issue.issueNumber}</span>
                        <span className="text-[#A7A29A] font-medium">{issue.repositoryFullName}</span>
                      </div>
                      <div className="flex items-center space-x-1">
                        <button
                          type="button"
                          onClick={() => handleRemoveSaved(issue.id)}
                          className="text-[#666666] hover:text-[#FF8A80] p-1 transition-colors"
                          title="Remove from Crate"
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>

                    {/* Title */}
                    <a
                      href={issue.issueUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="block group"
                    >
                      <h5 className="font-heading text-sm text-[#F1ECE2] group-hover:text-[#F4C13D] uppercase leading-snug transition-colors">
                        {issue.title}
                      </h5>
                    </a>

                    {/* Actions Row */}
                    <div className="flex items-center justify-between pt-2 border-t border-[#222222] text-[11px]">
                      <span className="text-[#666666]">
                        Saved {new Date(issue.savedAt).toLocaleDateString()}
                      </span>

                      <div className="flex items-center space-x-2">
                        <button
                          type="button"
                          onClick={() => handleStartContribution(issue)}
                          className="inline-flex items-center gap-1 text-[10px] text-[#2E7D32] hover:underline uppercase font-bold"
                          title="Move to Active Contribution Tracker"
                        >
                          <Play size={10} />
                          <span>START WORK</span>
                        </button>
                        <span>·</span>
                        <a
                          href={issue.issueUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-[#F4C13D] hover:underline uppercase font-bold flex items-center gap-1"
                        >
                          <span>GITHUB</span>
                          <ExternalLink size={10} />
                        </a>
                      </div>
                    </div>
                  </div>
                ))}

                {savedIssues.length > 5 && (
                  <div className="text-center pt-2">
                    <Link
                      to={ROUTES.SAVED_ISSUES}
                      className="font-mono text-xs text-[#F4C13D] hover:underline uppercase font-bold"
                    >
                      + VIEW {savedIssues.length - 5} MORE SAVED ISSUES IN CRATE →
                    </Link>
                  </div>
                )}
              </div>
            )}

            {/* Quick Tips Box */}
            <div className="border border-[#2E2E2E] bg-[#141414] p-4 font-mono text-xs space-y-2">
              <span className="text-[#F4C13D] font-bold uppercase tracking-wider block">
                CONTRIBUTOR WORKFLOW TIP
              </span>
              <p className="text-[#A7A29A] text-[11px] leading-relaxed">
                Move candidate tasks from <strong>Planning</strong> to <strong>In Progress</strong> when you fork the repository and branch locally. Reference issue numbers directly in your commit messages.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default DashboardPage;
