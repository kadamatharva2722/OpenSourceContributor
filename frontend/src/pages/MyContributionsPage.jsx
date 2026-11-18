import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { contributionService } from '../services/contributionService';
import { savedIssueService } from '../services/savedIssueService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  GitPullRequest,
  GitBranch,
  CheckCircle2,
  Clock,
  Plus,
  ExternalLink,
  Edit2,
  Trash2,
  Copy,
  Check,
  Search,
  Filter,
  RotateCcw,
  Bookmark,
  ArrowRight,
  AlertCircle,
  X,
  Layers,
  Terminal,
  Loader2,
} from 'lucide-react';

const VALID_STATUSES = ['PLANNING', 'IN_PROGRESS', 'PR_OPENED', 'COMPLETED'];
const FILTER_TABS = ['ALL', ...VALID_STATUSES];

export function MyContributionsPage() {
  useDocumentTitle('My Contributions');
  const navigate = useNavigate();

  // Ledger and Crate State
  const [contributions, setContributions] = useState([]);
  const [savedIssues, setSavedIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);

  // Filter and Search States
  const [activeStatus, setActiveStatus] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('RECENTLY_UPDATED');

  // Inline / Modal Editing States
  const [editingId, setEditingId] = useState(null);
  const [editForm, setEditForm] = useState({
    status: 'IN_PROGRESS',
    branch: '',
    notes: '',
    prUrl: '',
  });

  // Modal: Log New Contribution
  const [newModalOpen, setNewModalOpen] = useState(false);
  const [newModalTab, setNewModalTab] = useState('SAVED_CRATE'); // 'SAVED_CRATE' or 'CUSTOM'
  const [selectedSavedId, setSelectedSavedId] = useState('');

  // Custom Form state for manual contribution log
  const [customForm, setCustomForm] = useState({
    repo: '',
    number: '',
    title: '',
    status: 'PLANNING',
    branch: '',
    notes: '',
    prUrl: '',
  });

  // Delete confirmation modal state
  const [deleteTargetId, setDeleteTargetId] = useState(null);

  // Transient feedback toast
  const [toastMessage, setToastMessage] = useState(null);
  const [copiedBranchId, setCopiedBranchId] = useState(null);

  // Trigger feedback toast
  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => {
      setToastMessage(null);
    }, 3200);
  };

  // Load contributions and saved issues from real backend APIs
  const loadLedgerData = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const [conts, saved] = await Promise.all([
        contributionService.getContributions(activeStatus === 'ALL' ? null : activeStatus),
        savedIssueService.getSavedIssues(),
      ]);
      setContributions(conts);
      setSavedIssues(saved);
    } catch (err) {
      console.error('Failed to load contribution data:', err);
      if (err?.status === 401) {
        navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.MY_CONTRIBUTIONS)}`, { replace: true });
        return;
      }
      setErrorMessage(err.message || 'Failed to load contribution workspace data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLedgerData();
  }, [activeStatus]);

  // Keyboard accessibility & focus trapping for modal dialogs
  const newModalRef = useFocusTrap(newModalOpen, () => setNewModalOpen(false));
  const editModalRef = useFocusTrap(Boolean(editingId), () => setEditingId(null));
  const deleteModalRef = useFocusTrap(Boolean(deleteTargetId), () => setDeleteTargetId(null));

  // Compute helper branch name if not explicitly set
  const getBranchName = (item) => {
    if (item.branchName && item.branchName.trim()) return item.branchName.trim();
    const slug = (item.title || 'patch')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 24);
    return `contrib/${item.issueNumber || 'fix'}-${slug}`;
  };

  // Copy branch name or checkout command to clipboard
  const handleCopyBranch = async (item, asCheckoutCommand = false) => {
    const branch = getBranchName(item);
    const textToCopy = asCheckoutCommand ? `git checkout -b ${branch}` : branch;
    try {
      await navigator.clipboard.writeText(textToCopy);
      setCopiedBranchId(item.id);
      showToast(asCheckoutCommand ? `COPIED: git checkout -b ${branch}` : `COPIED: ${branch}`);
      setTimeout(() => setCopiedBranchId(null), 2400);
    } catch {
      showToast('FAILED TO COPY TO CLIPBOARD');
    }
  };

  // Start editing a contribution record
  const handleStartEdit = (item) => {
    setEditingId(item.id);
    setEditForm({
      status: item.status || 'IN_PROGRESS',
      branch: item.branchName || getBranchName(item),
      notes: item.notes || '',
      prUrl: item.pullRequestUrl || '',
    });
  };

  // Save edits back to backend API
  const handleSaveEdit = async (id) => {
    try {
      await contributionService.updateContribution(id, {
        status: editForm.status,
        branchName: editForm.branch,
        pullRequestUrl: editForm.prUrl,
        notes: editForm.notes,
      });
      setEditingId(null);
      await loadLedgerData();
      showToast('UPDATED CONTRIBUTION RECORD');
    } catch (err) {
      console.error('Failed to update contribution:', err);
      showToast('FAILED TO UPDATE CONTRIBUTION');
    }
  };

  // Quick inline status change directly from dropdown
  const handleQuickStatusChange = async (item, newStatus) => {
    try {
      await contributionService.updateContribution(item.id, { status: newStatus });
      await loadLedgerData();
      showToast(`UPDATED STATUS TO ${newStatus.replace('_', ' ')}`);
    } catch (err) {
      console.error('Failed to update status:', err);
      showToast('FAILED TO UPDATE STATUS');
    }
  };

  // Confirm and delete contribution
  const handleConfirmDelete = async () => {
    if (!deleteTargetId) return;
    try {
      await contributionService.deleteContribution(deleteTargetId);
      setDeleteTargetId(null);
      if (editingId === deleteTargetId) setEditingId(null);
      await loadLedgerData();
      showToast('REMOVED RECORD FROM CONTRIBUTION LEDGER');
    } catch (err) {
      console.error('Failed to delete contribution:', err);
      showToast('FAILED TO DELETE CONTRIBUTION');
    }
  };

  // Log contribution from Saved Issue
  const handleLogFromSaved = async (e) => {
    e.preventDefault();
    const issue = savedIssues.find((i) => String(i.id) === String(selectedSavedId));
    if (!issue) return;

    const slug = (issue.title || 'patch')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 24);

    try {
      await contributionService.createContribution({
        githubIssueId: issue.githubIssueId,
        repositoryFullName: issue.repositoryFullName,
        issueNumber: issue.issueNumber,
        title: issue.title,
        status: 'IN_PROGRESS',
        branchName: `contrib/${issue.issueNumber}-${slug}`,
        notes: 'Promoted from Saved Crate into active workspace.',
      });

      setNewModalOpen(false);
      setSelectedSavedId('');
      await loadLedgerData();
      showToast(`ADDED #${issue.issueNumber} (${issue.repositoryFullName}) TO LEDGER`);
    } catch (err) {
      console.error('Failed to create contribution from saved:', err);
      showToast('FAILED TO LOG CONTRIBUTION');
    }
  };

  // Log custom manual contribution
  const handleLogCustom = async (e) => {
    e.preventDefault();
    if (!customForm.repo || !customForm.title) return;

    const num = customForm.number ? parseInt(customForm.number, 10) : 1;
    const generatedGithubId = Date.now(); // Numeric ID for tracking

    const slug = customForm.title
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 24);

    try {
      await contributionService.createContribution({
        githubIssueId: generatedGithubId,
        repositoryFullName: customForm.repo.trim(),
        issueNumber: num,
        title: customForm.title.trim(),
        status: customForm.status,
        branchName: customForm.branch.trim() || `contrib/${num}-${slug}`,
        notes: customForm.notes.trim() || 'Manual contribution entry.',
        pullRequestUrl: customForm.prUrl.trim() || undefined,
      });

      setNewModalOpen(false);
      setCustomForm({
        repo: '',
        number: '',
        title: '',
        status: 'PLANNING',
        branch: '',
        notes: '',
        prUrl: '',
      });
      await loadLedgerData();
      showToast(`ADDED #${num} (${customForm.repo}) TO LEDGER`);
    } catch (err) {
      console.error('Failed to log custom contribution:', err);
      showToast('FAILED TO LOG CONTRIBUTION');
    }
  };

  // Filter and sort contributions
  const filteredContributions = useMemo(() => {
    let result = [...contributions];

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (item) =>
          item.title?.toLowerCase().includes(q) ||
          item.repositoryFullName?.toLowerCase().includes(q) ||
          String(item.issueNumber).includes(q) ||
          item.notes?.toLowerCase().includes(q) ||
          item.branchName?.toLowerCase().includes(q)
      );
    }

    switch (sortBy) {
      case 'REPO_ASC':
        result.sort((a, b) => a.repositoryFullName.localeCompare(b.repositoryFullName));
        break;
      case 'ISSUE_NUM_DESC':
        result.sort((a, b) => b.issueNumber - a.issueNumber);
        break;
      case 'RECENTLY_UPDATED':
      default:
        result.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));
        break;
    }

    return result;
  }, [contributions, searchQuery, sortBy]);

  // Counts by status
  const planningCount = contributions.filter((c) => c.status === 'PLANNING').length;
  const inProgressCount = contributions.filter((c) => c.status === 'IN_PROGRESS').length;
  const prOpenedCount = contributions.filter((c) => c.status === 'PR_OPENED').length;
  const completedCount = contributions.filter((c) => c.status === 'COMPLETED').length;

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PLANNING':
        return <Badge variant="surface" size="xs">PLANNING</Badge>;
      case 'IN_PROGRESS':
        return <Badge variant="yellow" size="xs">IN PROGRESS</Badge>;
      case 'PR_OPENED':
        return <Badge variant="blue" size="xs">PR OPENED</Badge>;
      case 'COMPLETED':
        return <Badge variant="green" size="xs">COMPLETED</Badge>;
      default:
        return <Badge variant="surface" size="xs">{status}</Badge>;
    }
  };

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center min-h-[60vh] bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-sm w-full text-center space-y-4">
          <Loader2 size={32} className="animate-spin text-[#F06522] mx-auto" />
          <div className="font-mono text-xs text-[#F4C13D] uppercase tracking-wider font-bold">
            LOADING CONTRIBUTION LEDGER...
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2] min-h-screen">
      {/* Toast Notification Banner */}
      {toastMessage && (
        <div className="fixed top-20 right-4 md:right-8 z-50 bg-[#F4C13D] text-[#0D0D0D] px-4 py-2.5 font-mono text-xs font-bold uppercase tracking-wider border border-[#F4C13D] shadow-2xl flex items-center space-x-2 animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 size={16} />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Error Alert Banner if API failed */}
      {errorMessage && (
        <div className="bg-[#2A1616] border-b border-[#FF8585] text-[#FF8585] px-4 py-2.5 text-xs font-mono flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle size={14} />
            <span>ERROR: {errorMessage}</span>
          </div>
          <button
            type="button"
            onClick={loadLedgerData}
            className="underline hover:text-[#F1ECE2] uppercase"
          >
            RETRY
          </button>
        </div>
      )}

      {/* 3. Page Header Section */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2 text-xs font-mono text-[#F06522] uppercase tracking-widest">
              <GitPullRequest size={14} />
              <span>ACTIVE WORKSPACE // CONTRIBUTION LEDGER</span>
            </div>
            <h1 className="font-heading text-4xl md:text-5xl text-[#F1ECE2] uppercase tracking-tight">
              MY CONTRIBUTIONS.
            </h1>
            <p className="font-sans text-sm md:text-base text-[#A7A29A] max-w-2xl leading-relaxed">
              Track open-source contributions through their entire lifecycle: from planning and local branches
              to submitted pull requests and completed merges.
            </p>
          </div>

          {/* Quick Header Actions */}
          <div className="flex flex-wrap items-center gap-2.5">
            <Button
              variant="primary"
              size="md"
              onClick={() => setNewModalOpen(true)}
              className="flex items-center gap-1.5"
            >
              <Plus size={14} />
              LOG CONTRIBUTION
            </Button>
            <Link to={ROUTES.SAVED_ISSUES}>
              <Button variant="secondary" size="md" className="flex items-center gap-1.5">
                <Bookmark size={14} />
                VIEW CRATE ({savedIssues.length})
              </Button>
            </Link>
            <Link to={ROUTES.DASHBOARD}>
              <Button variant="outline" size="md" className="flex items-center gap-1.5">
                <Layers size={14} />
                DASHBOARD
              </Button>
            </Link>
          </div>
        </div>

        {/* Contribution Status Overview Strip */}
        <div className="max-w-7xl mx-auto grid grid-cols-2 sm:grid-cols-4 gap-3 mt-8 pt-6 border-t border-[#222222]">
          <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5">
            <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
              PLANNING
            </span>
            <span className="font-heading text-2xl text-[#F1ECE2]">{planningCount}</span>
            <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
              Under evaluation
            </span>
          </div>
          <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5 border-l-2 border-l-[#F4C13D]">
            <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
              IN PROGRESS
            </span>
            <span className="font-heading text-2xl text-[#F4C13D]">{inProgressCount}</span>
            <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
              Active coding branches
            </span>
          </div>
          <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5 border-l-2 border-l-[#5B86F2]">
            <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
              PR OPENED
            </span>
            <span className="font-heading text-2xl text-[#5B86F2]">{prOpenedCount}</span>
            <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
              Awaiting review
            </span>
          </div>
          <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5 border-l-2 border-l-[#00D26A]">
            <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
              COMPLETED
            </span>
            <span className="font-heading text-2xl text-[#00D26A]">{completedCount}</span>
            <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
              Merged / resolved
            </span>
          </div>
        </div>
      </section>

      {/* 4. Filter Toolbar & Search */}
      <section className="border-b border-[#2E2E2E] bg-[#111111] px-4 md:px-8 py-3">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-center justify-between gap-4">
          {/* Status Tabs */}
          <div className="flex flex-wrap items-center gap-1 font-mono text-xs">
            {FILTER_TABS.map((tab) => (
              <button
                key={tab}
                type="button"
                onClick={() => setActiveStatus(tab)}
                className={`px-3 py-1.5 uppercase font-bold transition-colors ${
                  activeStatus === tab
                    ? 'bg-[#F06522] text-[#0D0D0D]'
                    : 'bg-[#1A1A1A] text-[#A7A29A] hover:text-[#F1ECE2] border border-[#2E2E2E]'
                }`}
              >
                {tab.replace('_', ' ')}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div className="relative max-w-xs w-full">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#A7A29A]" />
            <input
              type="text"
              id="contribution-search"
              aria-label="Search contributions by repository or keyword"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search contributions..."
              className="w-full bg-[#1A1A1A] border border-[#2E2E2E] pl-9 pr-3 py-1.5 text-xs font-mono text-[#F1ECE2] placeholder-[#666666] focus:outline-none focus:border-[#F06522] rounded-none"
            />
          </div>
        </div>
      </section>

      {/* 5. Main Content Area */}
      <main className="flex-1 px-4 md:px-8 py-8 max-w-7xl mx-auto w-full">
        {errorMessage ? (
          <div className="border border-[#93000A] bg-[#1A1414] p-8 md:p-14 text-center max-w-2xl mx-auto space-y-4 font-mono">
            <AlertCircle size={32} className="mx-auto text-[#FF8585]" />
            <div className="space-y-1">
              <span className="text-xs text-[#FF8585] uppercase tracking-widest font-bold block">
                FAILED TO COMMUNICATE WITH SERVER
              </span>
              <h3 className="font-heading text-2xl text-[#FFDAD6] uppercase">
                COULD NOT RETRIEVE CONTRIBUTION LEDGER
              </h3>
              <p className="text-xs text-[#E2D2D2] max-w-md mx-auto leading-relaxed">
                {errorMessage}
              </p>
            </div>
            <div className="pt-2 flex justify-center gap-3">
              <Button variant="primary" size="sm" onClick={loadLedgerData}>
                RETRY REQUEST
              </Button>
            </div>
          </div>
        ) : filteredContributions.length === 0 ? (
          <div className="border border-[#2E2E2E] bg-[#141414] p-8 md:p-14 text-center max-w-2xl mx-auto space-y-4 font-mono">
            <GitPullRequest size={36} className="mx-auto text-[#A7A29A]" />
            <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase">
              NO CONTRIBUTIONS IN CURRENT VIEW
            </h3>
            <p className="text-xs text-[#A7A29A] leading-relaxed max-w-md mx-auto">
              You haven't logged any contributions matching this filter. Log an issue from your saved crate
              or add a custom record to start tracking.
            </p>
            <div className="pt-2 flex justify-center gap-3">
              <Button variant="primary" size="sm" onClick={() => setNewModalOpen(true)}>
                <Plus size={14} className="mr-1" />
                LOG CONTRIBUTION
              </Button>
              <Link to={ROUTES.EXPLORE}>
                <Button variant="secondary" size="sm">
                  EXPLORE ISSUES
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="space-y-4 font-mono">
            {filteredContributions.map((item) => (
              <div
                key={item.id}
                className="border border-[#2E2E2E] bg-[#141414] p-5 space-y-4 hover:border-[#444444] transition-colors"
              >
                {/* Header: Repo, Number, Status, Actions */}
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center space-x-2 text-xs">
                    <span className="text-[#F06522] font-bold">#{item.issueNumber}</span>
                    <span className="text-[#F4C13D] font-semibold">{item.repositoryFullName}</span>
                    {getStatusBadge(item.status)}
                  </div>

                  <div className="flex items-center space-x-2">
                    {/* Quick status dropdown */}
                    <select
                      value={item.status}
                      onChange={(e) => handleQuickStatusChange(item, e.target.value)}
                      className="text-[10px] font-mono px-2 py-1 bg-[#1C1B1B] text-[#F1ECE2] border border-[#2E2E2E] rounded-none focus:outline-none uppercase"
                    >
                      {VALID_STATUSES.map((st) => (
                        <option key={st} value={st} className="bg-[#141414] text-[#F1ECE2]">
                          {st.replace('_', ' ')}
                        </option>
                      ))}
                    </select>

                    <button
                      type="button"
                      onClick={() => handleStartEdit(item)}
                      className="text-[#A7A29A] hover:text-[#F1ECE2] p-1.5 border border-[#2E2E2E] bg-[#1A1A1A]"
                      title="Edit details"
                    >
                      <Edit2 size={13} />
                    </button>

                    <button
                      type="button"
                      onClick={() => setDeleteTargetId(item.id)}
                      className="text-[#FF8585] hover:bg-[#2A1616] p-1.5 border border-[#2E2E2E] bg-[#1A1A1A]"
                      title="Delete record"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>

                {/* Title */}
                <a
                  href={item.issueUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="block group"
                >
                  <h3 className="font-heading text-lg sm:text-xl text-[#F1ECE2] group-hover:text-[#F06522] uppercase tracking-wide transition-colors leading-snug">
                    {item.title}
                  </h3>
                </a>

                {/* Branch and Notes Details */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs bg-[#1A1A1A] p-3 border border-[#262626]">
                  {/* Branch info */}
                  <div className="flex items-center justify-between space-x-2">
                    <div className="flex items-center space-x-2 text-[#A7A29A] truncate">
                      <GitBranch size={13} className="text-[#F4C13D] shrink-0" />
                      <span className="text-[#F1ECE2] font-mono truncate">{getBranchName(item)}</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleCopyBranch(item, true)}
                      className="text-[10px] text-[#A7A29A] hover:text-[#F4C13D] flex items-center gap-1 shrink-0 bg-[#252525] px-2 py-0.5 border border-[#333333]"
                      title="Copy git checkout -b command"
                    >
                      {copiedBranchId === item.id ? <Check size={10} className="text-[#00D26A]" /> : <Copy size={10} />}
                      <span>GIT COMMAND</span>
                    </button>
                  </div>

                  {/* PR Link or GitHub */}
                  <div className="flex items-center justify-between space-x-2">
                    {item.pullRequestUrl ? (
                      <a
                        href={item.pullRequestUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-[#5B86F2] hover:underline flex items-center gap-1 text-xs truncate"
                      >
                        <ExternalLink size={12} className="shrink-0" />
                        <span className="truncate">PR: {item.pullRequestUrl}</span>
                      </a>
                    ) : (
                      <span className="text-[#666666] text-xs">&gt; No Pull Request linked yet</span>
                    )}

                    <a
                      href={item.issueUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-[#A7A29A] hover:text-[#F1ECE2] flex items-center gap-1 text-[11px] shrink-0"
                    >
                      <span>GITHUB</span>
                      <ExternalLink size={10} />
                    </a>
                  </div>
                </div>

                {/* Notes snippet */}
                {item.notes && (
                  <div className="text-xs text-[#A7A29A] pl-3 border-l-2 border-[#F06522]">
                    &gt; {item.notes}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Modal: Log New Contribution */}
      {newModalOpen && (
        <div
          ref={newModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="new-contrib-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#F4C13D] p-6 max-w-lg w-full space-y-4 shadow-2xl font-mono max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2 text-[#F4C13D] text-xs font-bold uppercase">
                <Plus size={14} />
                <span id="new-contrib-title">LOG NEW CONTRIBUTION</span>
              </div>
              <button
                type="button"
                onClick={() => setNewModalOpen(false)}
                className="text-[#A7A29A] hover:text-[#F1ECE2] p-2 min-h-[44px] min-w-[44px] flex items-center justify-center"
                aria-label="Close new contribution modal"
              >
                <X size={16} />
              </button>
            </div>

            {/* Tab selector: From Saved Crate vs Custom */}
            <div className="flex border-b border-[#2E2E2E] text-xs">
              <button
                type="button"
                onClick={() => setNewModalTab('SAVED_CRATE')}
                className={`flex-1 py-2 text-center uppercase font-bold border-b-2 ${
                  newModalTab === 'SAVED_CRATE'
                    ? 'border-[#F06522] text-[#F06522]'
                    : 'border-transparent text-[#A7A29A] hover:text-[#F1ECE2]'
                }`}
              >
                FROM SAVED CRATE ({savedIssues.length})
              </button>
              <button
                type="button"
                onClick={() => setNewModalTab('CUSTOM')}
                className={`flex-1 py-2 text-center uppercase font-bold border-b-2 ${
                  newModalTab === 'CUSTOM'
                    ? 'border-[#F06522] text-[#F06522]'
                    : 'border-transparent text-[#A7A29A] hover:text-[#F1ECE2]'
                }`}
              >
                CUSTOM ENTRY
              </button>
            </div>

            {newModalTab === 'SAVED_CRATE' ? (
              <form onSubmit={handleLogFromSaved} className="space-y-4 text-xs">
                {savedIssues.length === 0 ? (
                  <div className="p-4 bg-[#1A1A1A] border border-[#2E2E2E] text-center text-[#A7A29A] space-y-2">
                    <p>No saved issues available in crate.</p>
                    <Link to={ROUTES.EXPLORE} className="text-[#F06522] underline uppercase block">
                      Explore issues catalog →
                    </Link>
                  </div>
                ) : (
                  <div>
                    <label htmlFor="select-saved-issue" className="block text-[#A7A29A] uppercase mb-1">
                      SELECT SAVED ISSUE *
                    </label>
                    <select
                      id="select-saved-issue"
                      value={selectedSavedId}
                      onChange={(e) => setSelectedSavedId(e.target.value)}
                      required
                      className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none"
                    >
                      <option value="">-- Choose issue from crate --</option>
                      {savedIssues.map((issue) => (
                        <option key={issue.id} value={issue.id}>
                          #{issue.issueNumber} ({issue.repositoryFullName}) - {issue.title}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="flex justify-end space-x-2 pt-2 border-t border-[#2E2E2E]">
                  <Button variant="secondary" size="sm" type="button" onClick={() => setNewModalOpen(false)}>
                    CANCEL
                  </Button>
                  <Button variant="primary" size="sm" type="submit" disabled={!selectedSavedId}>
                    LOG TO LEDGER
                  </Button>
                </div>
              </form>
            ) : (
              <form onSubmit={handleLogCustom} className="space-y-3 text-xs">
                <div>
                  <label htmlFor="custom-repo" className="block text-[#A7A29A] uppercase mb-1">REPOSITORY (OWNER/REPO) *</label>
                  <input
                    id="custom-repo"
                    type="text"
                    value={customForm.repo}
                    onChange={(e) => setCustomForm({ ...customForm, repo: e.target.value })}
                    placeholder="e.g. facebook/react"
                    required
                    className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label htmlFor="custom-number" className="block text-[#A7A29A] uppercase mb-1">ISSUE NUMBER *</label>
                    <input
                      id="custom-number"
                      type="number"
                      value={customForm.number}
                      onChange={(e) => setCustomForm({ ...customForm, number: e.target.value })}
                      placeholder="e.g. 1024"
                      required
                      className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none"
                    />
                  </div>
                  <div>
                    <label htmlFor="custom-status" className="block text-[#A7A29A] uppercase mb-1">INITIAL STATUS</label>
                    <select
                      id="custom-status"
                      value={customForm.status}
                      onChange={(e) => setCustomForm({ ...customForm, status: e.target.value })}
                      className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none uppercase"
                    >
                      {VALID_STATUSES.map((st) => (
                        <option key={st} value={st}>{st.replace('_', ' ')}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div>
                  <label htmlFor="custom-title" className="block text-[#A7A29A] uppercase mb-1">ISSUE TITLE *</label>
                  <input
                    id="custom-title"
                    type="text"
                    value={customForm.title}
                    onChange={(e) => setCustomForm({ ...customForm, title: e.target.value })}
                    placeholder="e.g. Fix memory leak in request cache"
                    required
                    className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none"
                  />
                </div>

                <div>
                  <label htmlFor="custom-notes" className="block text-[#A7A29A] uppercase mb-1">NOTES / OBJECTIVE</label>
                  <textarea
                    id="custom-notes"
                    rows={2}
                    value={customForm.notes}
                    onChange={(e) => setCustomForm({ ...customForm, notes: e.target.value })}
                    placeholder="Brief notes about the contribution plan..."
                    className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#F06522] rounded-none"
                  />
                </div>

                <div className="flex justify-end space-x-2 pt-2 border-t border-[#2E2E2E]">
                  <Button variant="secondary" size="sm" type="button" onClick={() => setNewModalOpen(false)}>
                    CANCEL
                  </Button>
                  <Button variant="primary" size="sm" type="submit">
                    SAVE TO LEDGER
                  </Button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Modal: Edit Contribution Details */}
      {editingId && (
        <div
          ref={editModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-contrib-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#5B86F2] p-6 max-w-lg w-full space-y-4 shadow-2xl font-mono max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2 text-[#5B86F2] text-xs font-bold uppercase">
                <Edit2 size={14} />
                <span id="edit-contrib-title">EDIT CONTRIBUTION RECORD</span>
              </div>
              <button
                type="button"
                onClick={() => setEditingId(null)}
                className="text-[#A7A29A] hover:text-[#F1ECE2] p-2 min-h-[44px] min-w-[44px] flex items-center justify-center"
                aria-label="Close edit modal"
              >
                <X size={16} />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSaveEdit(editingId);
              }}
              className="space-y-3 text-xs"
            >
              <div>
                <label htmlFor="edit-status" className="block text-[#A7A29A] uppercase mb-1">STATUS</label>
                <select
                  id="edit-status"
                  value={editForm.status}
                  onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}
                  className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#5B86F2] rounded-none uppercase"
                >
                  {VALID_STATUSES.map((st) => (
                    <option key={st} value={st}>{st.replace('_', ' ')}</option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="edit-branch" className="block text-[#A7A29A] uppercase mb-1">LOCAL BRANCH NAME</label>
                <input
                  id="edit-branch"
                  type="text"
                  value={editForm.branch}
                  onChange={(e) => setEditForm({ ...editForm, branch: e.target.value })}
                  className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#5B86F2] rounded-none"
                />
              </div>

              <div>
                <label htmlFor="edit-pr-url" className="block text-[#A7A29A] uppercase mb-1">PULL REQUEST URL</label>
                <input
                  id="edit-pr-url"
                  type="url"
                  value={editForm.prUrl}
                  onChange={(e) => setEditForm({ ...editForm, prUrl: e.target.value })}
                  placeholder="https://github.com/owner/repo/pull/123"
                  className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#5B86F2] rounded-none"
                />
              </div>

              <div>
                <label htmlFor="edit-notes" className="block text-[#A7A29A] uppercase mb-1">WORKSPACE NOTES</label>
                <textarea
                  id="edit-notes"
                  rows={3}
                  value={editForm.notes}
                  onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
                  className="w-full bg-[#1A1A1A] border border-[#2E2E2E] p-2 text-[#F1ECE2] focus:outline-none focus:border-[#5B86F2] rounded-none"
                />
              </div>

              <div className="flex justify-end space-x-2 pt-2 border-t border-[#2E2E2E]">
                <Button variant="secondary" size="sm" type="button" onClick={() => setEditingId(null)}>
                  CANCEL
                </Button>
                <Button variant="primary" size="sm" type="submit">
                  SAVE CHANGES
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteTargetId && (
        <div
          ref={deleteModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-contrib-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#FF8585] p-6 max-w-md w-full space-y-4 shadow-2xl font-mono max-h-[90vh] overflow-y-auto">
            <div className="flex items-center space-x-2 text-[#FF8585] text-xs font-bold uppercase">
              <AlertCircle size={16} />
              <span id="delete-contrib-title">DELETE CONFIRMATION</span>
            </div>
            <h3 className="font-heading text-xl text-[#F1ECE2] uppercase">
              REMOVE CONTRIBUTION RECORD?
            </h3>
            <p className="text-xs text-[#A7A29A]">
              Are you sure you want to remove this record from your workspace contribution ledger?
            </p>
            <div className="flex justify-end space-x-2 pt-2 border-t border-[#2E2E2E]">
              <Button variant="secondary" size="sm" onClick={() => setDeleteTargetId(null)}>
                CANCEL
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleConfirmDelete}
                className="bg-[#2A1616] border-[#FF8585] text-[#FF8585] hover:bg-[#3D1E1E]"
              >
                DELETE RECORD
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MyContributionsPage;
