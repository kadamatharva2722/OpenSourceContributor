import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { savedIssueService } from '../services/savedIssueService';
import { contributionService } from '../services/contributionService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  Bookmark,
  Trash2,
  ExternalLink,
  ArrowRight,
  Download,
  Search,
  Filter,
  CheckSquare,
  Square,
  CheckCircle2,
  Play,
  Copy,
  Check,
  RotateCcw,
  Star,
  Layers,
  AlertCircle,
  X,
  FileCode,
  Clock,
  Sparkles,
  Loader2,
} from 'lucide-react';

export function SavedIssuesPage() {
  useDocumentTitle('Saved Issues');
  const navigate = useNavigate();

  // Storage states
  const [savedIssues, setSavedIssues] = useState([]);
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState(null);

  // Filter and search states
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRepo, setSelectedRepo] = useState('ALL');
  const [sortBy, setSortBy] = useState('RECENTLY_SAVED');

  // Selection states for bulk actions
  const [selectedIds, setSelectedIds] = useState([]);

  // Sleeve preview drawer state
  const [previewIssue, setPreviewIssue] = useState(null);

  // Confirmation modal states
  const [showClearModal, setShowClearModal] = useState(false);
  const [showBulkRemoveModal, setShowBulkRemoveModal] = useState(false);

  // Transient user feedback toast
  const [toastMessage, setToastMessage] = useState(null);

  // Trigger brief toast notification
  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => {
      setToastMessage(null);
    }, 3200);
  };

  // Load saved issues and contributions from real backend APIs
  const loadCrateData = async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const [issues, userContribs] = await Promise.all([
        savedIssueService.getSavedIssues(),
        contributionService.getContributions(),
      ]);
      setSavedIssues(issues);
      setContributions(userContribs);
    } catch (err) {
      console.error('Failed to load crate data:', err);
      if (err?.status === 401) {
        navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.SAVED_ISSUES)}`, { replace: true });
        return;
      }
      setErrorMessage(err.message || 'Failed to load saved crate data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCrateData();
  }, []);

  // Compute dynamically available repos from saved issues
  const availableRepos = useMemo(() => {
    const repos = new Set();
    savedIssues.forEach((issue) => {
      if (issue.repositoryFullName) repos.add(issue.repositoryFullName);
    });
    return ['ALL', ...Array.from(repos).sort()];
  }, [savedIssues]);

  // Filter and sort saved issues
  const filteredIssues = useMemo(() => {
    let result = [...savedIssues];

    // Keyword search filter (title, repo, issueNumber)
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase().trim();
      result = result.filter(
        (issue) =>
          issue.title?.toLowerCase().includes(q) ||
          issue.repositoryFullName?.toLowerCase().includes(q) ||
          String(issue.issueNumber).includes(q)
      );
    }

    // Repository filter
    if (selectedRepo !== 'ALL') {
      result = result.filter((issue) => issue.repositoryFullName === selectedRepo);
    }

    // Sorting
    switch (sortBy) {
      case 'REPO_ASC':
        result.sort((a, b) => a.repositoryFullName.localeCompare(b.repositoryFullName));
        break;
      case 'ISSUE_NUM_DESC':
        result.sort((a, b) => b.issueNumber - a.issueNumber);
        break;
      case 'ISSUE_NUM_ASC':
        result.sort((a, b) => a.issueNumber - b.issueNumber);
        break;
      case 'RECENTLY_SAVED':
      default:
        result.sort((a, b) => new Date(b.savedAt) - new Date(a.savedAt));
        break;
    }

    return result;
  }, [savedIssues, searchQuery, selectedRepo, sortBy]);

  // Handle single item remove
  const handleRemoveSingle = async (id, issueNumber) => {
    try {
      await savedIssueService.deleteSavedIssue(id);
      setSelectedIds((prev) => prev.filter((item) => item !== String(id)));
      if (previewIssue && String(previewIssue.id) === String(id)) {
        setPreviewIssue(null);
      }
      await loadCrateData();
      showToast(`REMOVED ISSUE #${issueNumber} FROM CRATE`);
    } catch (err) {
      console.error('Failed to remove issue:', err);
      showToast('FAILED TO REMOVE ISSUE');
    }
  };

  // Handle bulk remove of selected items
  const handleConfirmBulkRemove = async () => {
    try {
      await Promise.all(selectedIds.map((id) => savedIssueService.deleteSavedIssue(id)));
      const count = selectedIds.length;
      setSelectedIds([]);
      setShowBulkRemoveModal(false);
      if (previewIssue && selectedIds.includes(String(previewIssue.id))) {
        setPreviewIssue(null);
      }
      await loadCrateData();
      showToast(`REMOVED ${count} ISSUES FROM CRATE`);
    } catch (err) {
      console.error('Failed to bulk remove issues:', err);
      showToast('FAILED TO REMOVE SELECTED ISSUES');
    }
  };

  // Handle clear entire crate
  const handleConfirmClearAll = async () => {
    try {
      await Promise.all(savedIssues.map((issue) => savedIssueService.deleteSavedIssue(issue.id)));
      setSelectedIds([]);
      setPreviewIssue(null);
      setShowClearModal(false);
      await loadCrateData();
      showToast('CLEARED ALL ISSUES FROM CRATE');
    } catch (err) {
      console.error('Failed to clear crate:', err);
      showToast('FAILED TO CLEAR CRATE');
    }
  };

  // Multi-selection toggle helpers
  const handleToggleSelect = (id) => {
    const strId = String(id);
    setSelectedIds((prev) =>
      prev.includes(strId) ? prev.filter((item) => item !== strId) : [...prev, strId]
    );
  };

  const handleSelectAllFiltered = () => {
    if (selectedIds.length === filteredIssues.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredIssues.map((issue) => String(issue.id)));
    }
  };

  // Promote single issue to active contribution workspace
  const handleStartContribution = async (issue) => {
    try {
      await contributionService.createContribution({
        githubIssueId: issue.githubIssueId,
        repositoryFullName: issue.repositoryFullName,
        issueNumber: issue.issueNumber,
        title: issue.title,
        status: 'IN_PROGRESS',
        notes: 'Moved from saved crate to active workspace.',
      });
      await loadCrateData();
      showToast(`PROMOTED #${issue.issueNumber} (${issue.repositoryFullName}) TO ACTIVE WORKSPACE`);
    } catch (err) {
      console.error('Failed to promote issue:', err);
      showToast('FAILED TO PROMOTE ISSUE TO WORKSPACE');
    }
  };

  // Bulk promote selected issues to active contributions
  const handleBulkStartWork = async () => {
    const issuesToPromote = savedIssues.filter((issue) => selectedIds.includes(String(issue.id)));
    try {
      await Promise.all(
        issuesToPromote.map((issue) =>
          contributionService.createContribution({
            githubIssueId: issue.githubIssueId,
            repositoryFullName: issue.repositoryFullName,
            issueNumber: issue.issueNumber,
            title: issue.title,
            status: 'IN_PROGRESS',
            notes: 'Bulk promoted from saved crate to active workspace.',
          })
        )
      );
      await loadCrateData();
      showToast(`PROMOTED ${issuesToPromote.length} ISSUES TO IN_PROGRESS`);
    } catch (err) {
      console.error('Failed to bulk promote:', err);
      showToast('FAILED TO PROMOTE SOME ISSUES');
    }
  };

  // Export crate collection to JSON file
  const handleExportJSON = (issuesToExport = savedIssues, filename = null) => {
    const payload = {
      crateName: 'OpenSourceContributor Saved Crate Archive',
      version: '1.0',
      exportedAt: new Date().toISOString(),
      sourceMode: 'AUTHENTICATED_POSTGRESQL_REPOSITORY',
      totalCount: issuesToExport.length,
      issues: issuesToExport.map((issue) => ({
        id: issue.id,
        githubIssueId: issue.githubIssueId,
        repositoryFullName: issue.repositoryFullName,
        issueNumber: issue.issueNumber,
        title: issue.title,
        issueUrl: issue.issueUrl,
        savedAt: issue.savedAt,
      })),
    };

    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(payload, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute(
      'download',
      filename || `osc_saved_crate_${new Date().toISOString().slice(0, 10)}.json`
    );
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    showToast('CRATE EXPORTED AS JSON');
  };

  // Copy JSON to clipboard
  const handleCopyJSON = async () => {
    const payload = {
      crateName: 'OpenSourceContributor Saved Crate',
      exportedAt: new Date().toISOString(),
      issues: savedIssues,
    };
    try {
      await navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
      showToast('CRATE JSON COPIED TO CLIPBOARD');
    } catch {
      showToast('FAILED TO COPY TO CLIPBOARD');
    }
  };

  // Reset all active filters
  const handleResetFilters = () => {
    setSearchQuery('');
    setSelectedRepo('ALL');
    setSortBy('RECENTLY_SAVED');
  };

  const isFilterActive =
    searchQuery.trim() !== '' ||
    selectedRepo !== 'ALL' ||
    sortBy !== 'RECENTLY_SAVED';

  // Check if an issue is currently active in user contributions
  const getContributionStatus = (githubIssueId) => {
    const item = contributions.find((c) => String(c.githubIssueId) === String(githubIssueId));
    return item ? item.status : null;
  };

  // Compute crate summary statistics
  const totalSavedCount = savedIssues.length;
  const inWorkspaceCount = savedIssues.filter((i) =>
    contributions.some((c) => String(c.githubIssueId) === String(i.githubIssueId))
  ).length;

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center min-h-[60vh] bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-sm w-full text-center space-y-4">
          <Loader2 size={32} className="animate-spin text-[#F06522] mx-auto" />
          <div className="font-mono text-xs text-[#F4C13D] uppercase tracking-wider font-bold">
            LOADING SAVED CRATE...
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
            onClick={loadCrateData}
            className="underline hover:text-[#F1ECE2] uppercase"
          >
            RETRY
          </button>
        </div>
      )}

      {/* 3. Page Editorial Header */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2 text-xs font-mono text-[#F06522] uppercase tracking-widest">
              <Bookmark size={14} className="fill-[#F06522]" />
              <span>SAVED ISSUES ARCHIVE // CRATE INDEX</span>
            </div>
            <h1 className="font-heading text-4xl md:text-5xl text-[#F1ECE2] uppercase tracking-tight">
              SAVED ISSUES CRATE.
            </h1>
            <p className="font-sans text-sm md:text-base text-[#A7A29A] max-w-2xl leading-relaxed">
              Personal collection of bookmarked open-source tasks. Filter candidate records,
              inspect repository dossiers, export JSON manifests, and promote issues into your active contribution workspace.
            </p>
          </div>

          {/* Quick Header Actions */}
          <div className="flex flex-wrap items-center gap-2.5">
            <Link to={ROUTES.EXPLORE}>
              <Button variant="secondary" size="sm">
                <Search size={13} className="mr-1.5" />
                EXPLORE CATALOG
              </Button>
            </Link>
            <Link to={ROUTES.DASHBOARD}>
              <Button variant="secondary" size="sm">
                <Layers size={13} className="mr-1.5 text-[#F4C13D]" />
                WORKSPACE DASHBOARD
              </Button>
            </Link>
            {totalSavedCount > 0 && (
              <>
                <Button variant="outline" size="sm" onClick={() => handleExportJSON()}>
                  <Download size={13} className="mr-1.5" />
                  EXPORT JSON
                </Button>
                <Button variant="outline" size="sm" onClick={handleCopyJSON} title="Copy JSON to clipboard">
                  <Copy size={13} className="mr-1.5" />
                  COPY
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowClearModal(true)}
                  className="hover:border-[#FF8585] hover:text-[#FF8585]"
                >
                  <Trash2 size={13} className="mr-1.5 text-[#FF8585]" />
                  CLEAR CRATE
                </Button>
              </>
            )}
          </div>
        </div>

        {/* Crate Summary Metrics Ribbon */}
        {totalSavedCount > 0 && (
          <div className="max-w-7xl mx-auto grid grid-cols-2 sm:grid-cols-3 gap-3 mt-8 pt-6 border-t border-[#222222]">
            <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5">
              <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
                TOTAL IN CRATE
              </span>
              <span className="font-heading text-2xl text-[#F1ECE2]">{totalSavedCount}</span>
              <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
                Bookmarked tasks
              </span>
            </div>
            <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5">
              <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
                ACTIVE IN WORKSPACE
              </span>
              <span className="font-heading text-2xl text-[#00D26A]">{inWorkspaceCount}</span>
              <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
                In progress / PR
              </span>
            </div>
            <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3.5">
              <span className="text-[10px] font-mono text-[#A7A29A] uppercase tracking-wider block">
                REPOSITORIES
              </span>
              <span className="font-heading text-2xl text-[#F4C13D]">
                {availableRepos.filter((r) => r !== 'ALL').length}
              </span>
              <span className="text-[10px] font-mono text-[#666666] block mt-0.5">
                Unique repos bookmarked
              </span>
            </div>
          </div>
        )}
      </section>

      {/* 4. Filter Toolbar & Search Bar */}
      {totalSavedCount > 0 && (
        <section className="border-b border-[#2E2E2E] bg-[#111111] px-4 md:px-8 py-4">
          <div className="max-w-7xl mx-auto flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            {/* Search Input */}
            <div className="relative flex-1 max-w-md">
              <Search
                size={14}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-[#A7A29A]"
              />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search crate by title, #number, or repo..."
                className="w-full bg-[#1A1A1A] border border-[#2E2E2E] pl-9 pr-8 py-2 text-xs font-mono text-[#F1ECE2] placeholder-[#666666] focus:outline-none focus:border-[#F06522] rounded-none"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[#A7A29A] hover:text-[#F1ECE2]"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            {/* Filter Dropdowns */}
            <div className="flex flex-wrap items-center gap-2.5 text-xs font-mono">
              {/* Repository */}
              <div className="flex items-center space-x-1.5 bg-[#1A1A1A] border border-[#2E2E2E] px-2.5 py-1.5">
                <span className="text-[#A7A29A] text-[10px] uppercase">REPO:</span>
                <select
                  value={selectedRepo}
                  onChange={(e) => setSelectedRepo(e.target.value)}
                  className="bg-transparent text-[#F1ECE2] focus:outline-none cursor-pointer uppercase font-mono text-xs max-w-[180px] truncate"
                >
                  {availableRepos.map((repo) => (
                    <option key={repo} value={repo} className="bg-[#1A1A1A] text-[#F1ECE2]">
                      {repo}
                    </option>
                  ))}
                </select>
              </div>

              {/* Sort By */}
              <div className="flex items-center space-x-1.5 bg-[#1A1A1A] border border-[#2E2E2E] px-2.5 py-1.5">
                <span className="text-[#A7A29A] text-[10px] uppercase">SORT:</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="bg-transparent text-[#F1ECE2] focus:outline-none cursor-pointer uppercase font-mono text-xs"
                >
                  <option value="RECENTLY_SAVED" className="bg-[#1A1A1A] text-[#F1ECE2]">
                    RECENTLY SAVED
                  </option>
                  <option value="REPO_ASC" className="bg-[#1A1A1A] text-[#F1ECE2]">
                    REPO (A-Z)
                  </option>
                  <option value="ISSUE_NUM_DESC" className="bg-[#1A1A1A] text-[#F1ECE2]">
                    ISSUE # (NEWEST)
                  </option>
                  <option value="ISSUE_NUM_ASC" className="bg-[#1A1A1A] text-[#F1ECE2]">
                    ISSUE # (OLDEST)
                  </option>
                </select>
              </div>

              {/* Reset Filters */}
              {isFilterActive && (
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="flex items-center space-x-1 text-[#F06522] hover:text-[#F4C13D] border border-[#2E2E2E] bg-[#1A1A1A] px-2.5 py-1.5 text-xs font-mono uppercase"
                  title="Reset all active filters"
                >
                  <RotateCcw size={12} />
                  <span>RESET</span>
                </button>
              )}
            </div>
          </div>
        </section>
      )}

      {/* 5. Bulk Action Toolbar Ribbon (When 1 or more items selected) */}
      {selectedIds.length > 0 && (
        <section className="bg-[#1C1B1B] border-b border-[#F4C13D] px-4 md:px-8 py-3 animate-in fade-in">
          <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs font-mono">
            <div className="flex items-center space-x-3">
              <span className="bg-[#F4C13D] text-[#0D0D0D] font-bold px-2 py-0.5">
                {selectedIds.length} SELECTED
              </span>
              <span className="text-[#F1ECE2]">
                of {filteredIssues.length} items in current view
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Button variant="yellow" size="sm" onClick={handleBulkStartWork}>
                <Play size={13} className="mr-1.5 fill-[#0D0D0D]" />
                START WORK ({selectedIds.length})
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  const selectedIssuesList = savedIssues.filter((i) =>
                    selectedIds.includes(String(i.id))
                  );
                  handleExportJSON(
                    selectedIssuesList,
                    `osc_selected_issues_${new Date().toISOString().slice(0, 10)}.json`
                  );
                }}
              >
                <Download size={13} className="mr-1.5" />
                EXPORT SELECTED
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowBulkRemoveModal(true)}
                className="hover:border-[#FF8585] hover:text-[#FF8585]"
              >
                <Trash2 size={13} className="mr-1.5 text-[#FF8585]" />
                REMOVE SELECTED ({selectedIds.length})
              </Button>
              <button
                type="button"
                onClick={() => setSelectedIds([])}
                className="text-[#A7A29A] hover:text-[#F1ECE2] underline px-2 py-1"
              >
                DESELECT ALL
              </button>
            </div>
          </div>
        </section>
      )}

      {/* 6. Main Tracklist Section & Empty States */}
      <main className="flex-1 px-4 md:px-8 py-8 max-w-7xl mx-auto w-full">
        {errorMessage ? (
          <div className="border border-[#93000A] bg-[#1A1414] p-8 md:p-14 text-center max-w-2xl mx-auto space-y-4 font-mono">
            <AlertCircle size={32} className="mx-auto text-[#FF8585]" />
            <div className="space-y-1">
              <span className="text-xs text-[#FF8585] uppercase tracking-widest font-bold block">
                FAILED TO COMMUNICATE WITH SERVER
              </span>
              <h3 className="font-heading text-2xl text-[#FFDAD6] uppercase">
                COULD NOT RETRIEVE SAVED CRATE
              </h3>
              <p className="text-xs text-[#E2D2D2] max-w-md mx-auto leading-relaxed">
                {errorMessage}
              </p>
            </div>
            <div className="pt-2 flex justify-center gap-3">
              <Button variant="primary" size="sm" onClick={loadCrateData}>
                RETRY REQUEST
              </Button>
            </div>
          </div>
        ) : totalSavedCount === 0 ? (
          /* Empty State A: Nothing saved in crate at all */
          <div className="border border-[#2E2E2E] bg-[#141414] p-8 md:p-14 text-center max-w-3xl mx-auto space-y-6">
            <div className="w-16 h-16 border-2 border-dashed border-[#F4C13D] mx-auto flex items-center justify-center text-[#F4C13D]">
              <Bookmark size={32} />
            </div>

            <div className="space-y-2">
              <span className="text-xs font-mono text-[#F4C13D] uppercase tracking-widest block">
                CATALOG ARCHIVE // CRATE EMPTY
              </span>
              <h2 className="font-heading text-3xl md:text-4xl text-[#F1ECE2] uppercase">
                YOUR CRATE IS CURRENTLY EMPTY.
              </h2>
              <p className="font-sans text-sm md:text-base text-[#A7A29A] max-w-lg mx-auto leading-relaxed">
                You have not saved any open-source issues to your crate yet. Search the catalog,
                filter by your familiar technology stack, and bookmark candidate opportunities.
              </p>
            </div>

            <div className="pt-2">
              <Link to={ROUTES.EXPLORE}>
                <Button variant="primary" size="lg">
                  <Search size={16} className="mr-2" />
                  EXPLORE ISSUES CATALOG
                </Button>
              </Link>
            </div>
          </div>
        ) : filteredIssues.length === 0 ? (
          /* Empty State B: Crate has items, but active search/filter returned 0 results */
          <div className="border border-[#2E2E2E] bg-[#141414] p-8 text-center max-w-2xl mx-auto space-y-4">
            <Filter size={28} className="mx-auto text-[#A7A29A]" />
            <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase">
              NO SAVED ISSUES MATCH ACTIVE FILTERS
            </h3>
            <p className="font-mono text-xs text-[#A7A29A]">
              We couldn't find any issues matching "{searchQuery}" with the current filters.
            </p>
            <Button variant="secondary" size="sm" onClick={handleResetFilters}>
              <RotateCcw size={13} className="mr-1.5" />
              RESET FILTERS
            </Button>
          </div>
        ) : (
          /* Dense Editorial Tracklist Table */
          <div className="border border-[#2E2E2E] bg-[#141414] shadow-xl overflow-hidden">
            {/* Table Header Bar */}
            <div className="bg-[#1C1B1B] border-b border-[#2E2E2E] px-4 py-3 flex items-center justify-between text-[11px] font-mono text-[#A7A29A] uppercase tracking-wider">
              <div className="flex items-center space-x-3">
                <button
                  type="button"
                  onClick={handleSelectAllFiltered}
                  className="text-[#A7A29A] hover:text-[#F1ECE2] p-0.5"
                  title={
                    selectedIds.length === filteredIssues.length
                      ? 'Deselect all'
                      : 'Select all in view'
                  }
                >
                  {selectedIds.length > 0 && selectedIds.length === filteredIssues.length ? (
                    <CheckSquare size={16} className="text-[#F4C13D]" />
                  ) : (
                    <Square size={16} />
                  )}
                </button>
                <span>RECORD / REPOSITORY / TITLE</span>
              </div>
              <div className="hidden md:flex items-center space-x-12">
                <span>SAVED DATE</span>
                <span>WORKFLOW &amp; ACTIONS</span>
              </div>
            </div>

            {/* Tracklist Items */}
            <div className="divide-y divide-[#2E2E2E]">
              {filteredIssues.map((issue) => {
                const isSelected = selectedIds.includes(String(issue.id));
                const contribStatus = getContributionStatus(issue.githubIssueId);
                const isPreviewing = previewIssue && String(previewIssue.id) === String(issue.id);

                return (
                  <div
                    key={issue.id}
                    className={`p-4 transition-colors flex flex-col lg:flex-row lg:items-center justify-between gap-4 ${
                      isPreviewing
                        ? 'bg-[#1F1E1A] border-l-4 border-l-[#F4C13D]'
                        : isSelected
                        ? 'bg-[#1B1917] border-l-2 border-l-[#F06522]'
                        : 'hover:bg-[#1A1A1A]'
                    }`}
                  >
                    {/* Left Column: Select + Details */}
                    <div className="flex items-start space-x-3 min-w-0 flex-1">
                      <button
                        type="button"
                        onClick={() => handleToggleSelect(issue.id)}
                        className="mt-1 text-[#A7A29A] hover:text-[#F1ECE2] shrink-0"
                      >
                        {isSelected ? (
                          <CheckSquare size={16} className="text-[#F4C13D]" />
                        ) : (
                          <Square size={16} />
                        )}
                      </button>

                      <div className="min-w-0 flex-1 space-y-1">
                        {/* Meta Ribbon */}
                        <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
                          <span className="text-[#F06522] font-bold">#{issue.issueNumber}</span>
                          <span className="text-[#F4C13D] font-semibold">{issue.repositoryFullName}</span>
                          {contribStatus && (
                            <span className="text-[10px] bg-[#00D26A] text-[#0D0D0D] font-bold px-1.5 py-0.5 uppercase tracking-wider">
                              WORK: {contribStatus.replace('_', ' ')}
                            </span>
                          )}
                        </div>

                        {/* Issue Title */}
                        <h3 className="font-sans text-base font-semibold text-[#F1ECE2] leading-snug hover:text-[#F06522] transition-colors">
                          <a href={issue.issueUrl} target="_blank" rel="noopener noreferrer">
                            {issue.title}
                          </a>
                        </h3>
                      </div>
                    </div>

                    {/* Right Column: Date and Interactive Action Buttons */}
                    <div className="flex flex-wrap sm:flex-nowrap items-center justify-between lg:justify-end gap-3 shrink-0 pt-3 lg:pt-0 border-t lg:border-t-0 border-[#222222]">
                      <div className="text-xs font-mono text-[#A7A29A] flex items-center space-x-1">
                        <Clock size={12} className="text-[#666666]" />
                        <span>{new Date(issue.savedAt).toLocaleDateString()}</span>
                      </div>

                      {/* Interactive Actions */}
                      <div className="flex items-center space-x-2">
                        {/* Start Work / Active Button */}
                        {contribStatus ? (
                          <Link to={ROUTES.DASHBOARD}>
                            <Button variant="secondary" size="sm" title="View in Contributor Workspace">
                              <Check size={13} className="mr-1 text-[#00D26A]" />
                              ACTIVE
                            </Button>
                          </Link>
                        ) : (
                          <Button
                            variant="yellow"
                            size="sm"
                            onClick={() => handleStartContribution(issue)}
                            title="Promote to active contribution workspace"
                          >
                            <Play size={12} className="mr-1 fill-[#0D0D0D]" />
                            START WORK
                          </Button>
                        )}

                        {/* GitHub External Link */}
                        <a
                          href={issue.issueUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-[#A7A29A] hover:text-[#F1ECE2] p-1.5 border border-[#2E2E2E] bg-[#1A1A1A] hover:border-[#F1ECE2] transition-colors"
                          title="View original issue on GitHub"
                        >
                          <ExternalLink size={14} />
                        </a>

                        {/* Remove from Crate */}
                        <button
                          type="button"
                          onClick={() => handleRemoveSingle(issue.id, issue.issueNumber)}
                          className="text-[#FF8585] hover:bg-[#2A1616] p-1.5 border border-[#2E2E2E] bg-[#1A1A1A] hover:border-[#FF8585] transition-colors"
                          title="Remove from Crate"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Table Footer Status Bar */}
            <div className="bg-[#1C1B1B] border-t border-[#2E2E2E] px-4 py-2.5 flex flex-col sm:flex-row sm:items-center justify-between text-[11px] font-mono text-[#A7A29A] gap-2">
              <div className="flex items-center space-x-2">
                <span>VIEWING: {filteredIssues.length} {filteredIssues.length === 1 ? 'RECORD' : 'RECORDS'}</span>
              </div>
              <div className="flex items-center space-x-4">
                <span className="text-[#00D26A]">VERIFIED GITHUB ISSUES</span>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* 7. Confirmation Modal: Clear Entire Crate */}
      {showClearModal && (
        <div className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#141414] border-2 border-[#FF8585] p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center space-x-2 text-[#FF8585] font-mono text-xs font-bold uppercase tracking-wider">
              <AlertCircle size={16} />
              <span>CRITICAL CONFIRMATION</span>
            </div>
            <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase">
              CLEAR ALL SAVED ISSUES?
            </h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              This will remove all {savedIssues.length} bookmarked records from your crate.
              This action cannot be undone. (Active contributions in your workspace will be preserved).
            </p>
            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-[#2E2E2E]">
              <Button variant="secondary" size="sm" onClick={() => setShowClearModal(false)}>
                CANCEL
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleConfirmClearAll}
                className="bg-[#2A1616] border-[#FF8585] text-[#FF8585] hover:bg-[#3D1E1E]"
              >
                YES, CLEAR CRATE
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* 8. Confirmation Modal: Bulk Remove Selected */}
      {showBulkRemoveModal && (
        <div className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#141414] border-2 border-[#FF8585] p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center space-x-2 text-[#FF8585] font-mono text-xs font-bold uppercase tracking-wider">
              <AlertCircle size={16} />
              <span>BULK REMOVE CONFIRMATION</span>
            </div>
            <h3 className="font-heading text-2xl text-[#F1ECE2] uppercase">
              REMOVE {selectedIds.length} SELECTED ISSUES?
            </h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Are you sure you want to remove the {selectedIds.length} selected items from your saved crate?
            </p>
            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-[#2E2E2E]">
              <Button variant="secondary" size="sm" onClick={() => setShowBulkRemoveModal(false)}>
                CANCEL
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleConfirmBulkRemove}
                className="bg-[#2A1616] border-[#FF8585] text-[#FF8585] hover:bg-[#3D1E1E]"
              >
                REMOVE SELECTED
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SavedIssuesPage;
