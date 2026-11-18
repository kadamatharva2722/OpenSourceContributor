import React, { useState, useEffect, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { getCurrentUser } from '../services/authService';
import { accountService } from '../services/accountService';
import { userPreferenceService } from '../services/userPreferenceService';
import { savedIssueService } from '../services/savedIssueService';
import { contributionService } from '../services/contributionService';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import {
  Sliders,
  Save,
  CheckCircle2,
  User,
  UserX,
  Code2,
  Shield,
  RotateCcw,
  Download,
  Trash2,
  AlertCircle,
  X,
  Layers,
  Bookmark,
  GitPullRequest,
  Sparkles,
  Loader2,
  Github,
} from 'lucide-react';

const AVAILABLE_LANGUAGES = [
  'TypeScript',
  'JavaScript',
  'Rust',
  'Java',
  'Python',
  'Go',
  'C++',
  'Kotlin',
  'Ruby',
  'PHP',
  'Swift',
];

const AVAILABLE_DOMAINS = [
  'Developer Tools',
  'Web Frameworks',
  'Compilers & Bundlers',
  'Database Drivers',
  'CLI Utilities',
  'Machine Learning',
  'API & Networking',
  'Testing & CI',
];

const EXPERIENCE_LEVELS = [
  {
    id: 'Beginner',
    label: 'BEGINNER',
    badge: 'Good First Issue',
    desc: 'Focus on first contributions, clear documentation, test fixtures, and guided issues.',
  },
  {
    id: 'Intermediate',
    label: 'INTERMEDIATE',
    badge: 'Help Wanted',
    desc: 'Feature additions, bug fixes, client refactors, and moderate code footprint (~40 lines).',
  },
  {
    id: 'Advanced',
    label: 'ADVANCED',
    badge: 'Core Architecture',
    desc: 'Performance optimization, compiler ASTs, concurrency issues, and distributed systems.',
  },
];

export function SettingsPage() {
  useDocumentTitle('Settings');
  const navigate = useNavigate();

  // Profile & Preferences State
  const [userProfile, setUserProfile] = useState(null);
  const [preferences, setPreferences] = useState({
    preferredLanguages: [],
    preferredDomains: [],
    difficulty: 'Intermediate',
  });
  const [initialPreferences, setInitialPreferences] = useState({
    preferredLanguages: [],
    preferredDomains: [],
    difficulty: 'Intermediate',
  });

  // Workspace data states for metrics & export
  const [savedIssues, setSavedIssues] = useState([]);
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);

  // UI & Feedback states
  const [toastMessage, setToastMessage] = useState(null);
  const [showClearModal, setShowClearModal] = useState(false);
  const [showResetModal, setShowResetModal] = useState(false);
  const [showDeleteAccountModal, setShowDeleteAccountModal] = useState(false);
  const [deleteConfirmUsername, setDeleteConfirmUsername] = useState('');
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  // Trigger feedback toast
  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => {
      setToastMessage(null);
    }, 3200);
  };

  // Load workspace and preference data from real backend APIs
  const loadData = async () => {
    setLoading(true);
    try {
      const [user, prefs, saved, conts] = await Promise.all([
        getCurrentUser(),
        userPreferenceService.getPreferences(),
        savedIssueService.getSavedIssues(),
        contributionService.getContributions(),
      ]);
      setUserProfile(user);
      setPreferences(prefs);
      setInitialPreferences(prefs);
      setSavedIssues(saved);
      setContributions(conts);
    } catch (err) {
      console.error('Failed to load settings data:', err);
      if (err?.status === 401) {
        navigate(`${ROUTES.LOGIN}?returnUrl=${encodeURIComponent(ROUTES.SETTINGS)}`, { replace: true });
        return;
      }
      showToast('FAILED TO LOAD SETTINGS DATA');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Keyboard accessibility & focus management for modal dialogs
  const clearModalRef = useFocusTrap(showClearModal, () => setShowClearModal(false));
  const resetModalRef = useFocusTrap(showResetModal, () => setShowResetModal(false));
  const deleteModalRef = useFocusTrap(
    showDeleteAccountModal,
    () => {
      if (!isDeletingAccount) setShowDeleteAccountModal(false);
    }
  );

  // Track unsaved modifications
  const hasUnsavedChanges = useMemo(() => {
    return JSON.stringify(preferences) !== JSON.stringify(initialPreferences);
  }, [preferences, initialPreferences]);

  // Toggle Language Selection
  const toggleLanguage = (lang) => {
    const exists = preferences.preferredLanguages.includes(lang);
    let updated;
    if (exists) {
      updated = preferences.preferredLanguages.filter((l) => l !== lang);
    } else {
      updated = [...preferences.preferredLanguages, lang];
    }
    setPreferences({ ...preferences, preferredLanguages: updated });
  };

  // Toggle Domain Interest Selection
  const toggleInterest = (interest) => {
    const exists = preferences.preferredDomains.includes(interest);
    let updated;
    if (exists) {
      updated = preferences.preferredDomains.filter((i) => i !== interest);
    } else {
      updated = [...preferences.preferredDomains, interest];
    }
    setPreferences({ ...preferences, preferredDomains: updated });
  };

  // Save Preferences to backend API
  const handleSave = async (e) => {
    if (e) e.preventDefault();
    try {
      const updated = await userPreferenceService.updatePreferences(preferences);
      setPreferences(updated);
      setInitialPreferences(updated);
      showToast('DISCOVERY PREFERENCES SAVED');
    } catch (err) {
      console.error('Failed to save preferences:', err);
      showToast('FAILED TO SAVE PREFERENCES');
    }
  };

  // Reset to Defaults
  const handleConfirmReset = async () => {
    const defaultPrefs = {
      preferredLanguages: ['TypeScript', 'Java', 'Python'],
      preferredDomains: ['Developer Tools', 'Web Frameworks'],
      difficulty: 'Intermediate',
    };
    try {
      const updated = await userPreferenceService.updatePreferences(defaultPrefs);
      setPreferences(updated);
      setInitialPreferences(updated);
      setShowResetModal(false);
      showToast('SETTINGS RESTORED TO RECOMMENDED DEFAULTS');
    } catch (err) {
      console.error('Failed to reset preferences:', err);
      showToast('FAILED TO RESET PREFERENCES');
    }
  };

  // Clear Entire Workspace
  const handleConfirmClearWorkspace = async () => {
    try {
      await Promise.all([
        ...savedIssues.map((issue) => savedIssueService.deleteSavedIssue(issue.id)),
        ...contributions.map((c) => contributionService.deleteContribution(c.id)),
      ]);
      setSavedIssues([]);
      setContributions([]);
      setShowClearModal(false);
      showToast('SAVED CRATE AND CONTRIBUTION LEDGER CLEARED');
    } catch (err) {
      console.error('Failed to clear workspace:', err);
      showToast('FAILED TO CLEAR SOME RECORDS');
    }
  };

  // Open Delete Account Modal
  const handleOpenDeleteModal = () => {
    setDeleteConfirmUsername('');
    setDeleteError(null);
    setShowDeleteAccountModal(true);
  };

  // Execute Permanent Account Deletion
  const handleDeleteAccount = async () => {
    const requiredUsername = userProfile?.username || '';
    if (deleteConfirmUsername.trim() !== requiredUsername) {
      setDeleteError(`Please enter your exact GitHub username "${requiredUsername}" to confirm deletion.`);
      return;
    }

    setIsDeletingAccount(true);
    setDeleteError(null);
    try {
      await accountService.deleteAccount();
      // On successful deletion, redirect to the public landing page
      window.location.href = ROUTES.HOME;
    } catch (err) {
      console.error('Account deletion failed:', err);
      setIsDeletingAccount(false);
      if (err?.status === 403 && err?.message && err.message.toLowerCase().includes('re-authentication')) {
        setDeleteError('Recent authentication is required. Please sign in again before deleting your account.');
      } else {
        setDeleteError(err?.message || 'Failed to delete account. Please try again or re-authenticate.');
      }
    }
  };

  // Export Full User Workspace Archive JSON
  const handleExportFullVault = () => {
    const vault = {
      archiveName: 'OpenSourceContributor Complete Contributor Vault',
      exportDate: new Date().toISOString(),
      schemaVersion: '1.0',
      profile: userProfile,
      preferences: preferences,
      savedIssuesCrate: {
        total: savedIssues.length,
        records: savedIssues,
      },
      contributionLedger: {
        total: contributions.length,
        records: contributions,
      },
    };

    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(vault, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute(
      'download',
      `osc_complete_vault_${new Date().toISOString().slice(0, 10)}.json`
    );
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    showToast('EXPORTED COMPLETE WORKSPACE VAULT JSON');
  };

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center min-h-[60vh] bg-[#0D0D0D] text-[#F1ECE2]">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-sm w-full text-center space-y-4">
          <Loader2 size={32} className="animate-spin text-[#F06522] mx-auto" />
          <div className="font-mono text-xs text-[#F4C13D] uppercase tracking-wider font-bold">
            LOADING SETTINGS &amp; PREFERENCES...
          </div>
        </div>
      </div>
    );
  }

  const displayName = userProfile?.displayName || userProfile?.username || 'Contributor';
  const handleName = userProfile?.username ? `@${userProfile.username}` : '@contributor';

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2] min-h-screen">
      {/* Toast Notification Banner */}
      {toastMessage && (
        <div className="fixed top-20 right-4 md:right-8 z-50 bg-[#F4C13D] text-[#0D0D0D] px-4 py-2.5 font-mono text-xs font-bold uppercase tracking-wider border border-[#F4C13D] shadow-2xl flex items-center space-x-2 animate-in fade-in slide-in-from-top-2">
          <CheckCircle2 size={16} />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* 3. Page Header Section */}
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2 text-xs font-mono text-[#F4C13D] uppercase tracking-widest">
              <Sliders size={14} />
              <span>CONTRIBUTOR WORKSPACE // CONFIGURATION</span>
            </div>
            <h1 className="font-heading text-4xl md:text-5xl text-[#F1ECE2] uppercase tracking-tight">
              ACCOUNT &amp; STACK SETTINGS.
            </h1>
            <p className="font-sans text-sm md:text-base text-[#A7A29A] max-w-2xl leading-relaxed">
              Configure your developer profile, preferred technology runtimes, discovery criteria,
              and workspace data vault. Changes are saved to your account in the backend database.
            </p>
          </div>

          {/* Quick Header Actions */}
          <div className="flex flex-wrap items-center gap-2.5">
            {hasUnsavedChanges && (
              <span className="bg-[#F4C13D] text-[#0D0D0D] font-mono text-[10px] font-bold px-2 py-1 uppercase tracking-wider">
                UNSAVED CHANGES
              </span>
            )}
            <Button variant="primary" size="md" onClick={handleSave} className="flex items-center gap-1.5">
              <Save size={14} />
              SAVE SETTINGS
            </Button>
            <Button
              variant="secondary"
              size="md"
              onClick={() => setShowResetModal(true)}
              className="flex items-center gap-1.5"
            >
              <RotateCcw size={14} />
              RESET DEFAULTS
            </Button>
          </div>
        </div>
      </section>

      {/* 4. Settings Form Sections */}
      <main className="flex-1 px-4 md:px-8 py-8 max-w-7xl mx-auto w-full">
        <form onSubmit={handleSave} className="space-y-8">
          {/* Section 1: Contributor Profile */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2">
                <User size={16} className="text-[#F06522]" />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  CONTRIBUTOR IDENTITY
                </h2>
              </div>
              <Badge variant="green" size="xs">
                GITHUB OAUTH VERIFIED
              </Badge>
            </div>

            <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 p-3 bg-[#1A1A1A] border border-[#2A2A2A]">
              {userProfile?.avatarUrl ? (
                <img
                  src={userProfile.avatarUrl}
                  alt={displayName ? `${displayName}'s GitHub account avatar` : 'GitHub account avatar'}
                  decoding="async"
                  width="56"
                  height="56"
                  className="w-14 h-14 aspect-square border border-[#F4C13D] object-cover shrink-0"
                />
              ) : (
                <div className="w-14 h-14 bg-[#252525] border border-[#F4C13D] text-[#F4C13D] font-heading text-2xl flex items-center justify-center shrink-0">
                  {displayName.substring(0, 2).toUpperCase()}
                </div>
              )}
              <div className="space-y-0.5">
                <div className="font-sans font-bold text-base text-[#F1ECE2]">{displayName}</div>
                <div className="font-mono text-xs text-[#F4C13D]">{handleName}</div>
                <p className="font-mono text-[11px] text-[#A7A29A]">
                  Identity derived from your authenticated GitHub account.
                </p>
              </div>
            </div>

            {userProfile?.bio && (
              <div className="font-mono text-xs text-[#A7A29A] p-3 bg-[#1A1A1A] border border-[#2A2A2A]">
                <span className="text-[#F1ECE2] font-bold block mb-1">GITHUB BIO:</span>
                <p>{userProfile.bio}</p>
              </div>
            )}
          </div>

          {/* Section 2: Preferred Languages & Runtimes */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2">
                <Code2 size={16} className="text-[#F4C13D]" />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  PRIMARY LANGUAGES &amp; RUNTIMES
                </h2>
              </div>
              <span className="font-mono text-xs text-[#F4C13D] font-bold">
                {preferences.preferredLanguages?.length || 0} SELECTED
              </span>
            </div>

            <p className="font-sans text-xs text-[#A7A29A]">
              Select the programming environments you are active in. Issues matching these languages
              will receive prioritized relevance in search and recommendations.
            </p>

            <div className="flex flex-wrap gap-2 pt-2">
              {AVAILABLE_LANGUAGES.map((lang) => {
                const isSelected = preferences.preferredLanguages?.includes(lang);
                return (
                  <button
                    key={lang}
                    type="button"
                    onClick={() => toggleLanguage(lang)}
                    className={`px-3 py-1.5 font-mono text-xs uppercase tracking-wider border transition-colors ${
                      isSelected
                        ? 'bg-[#F06522] text-[#0D0D0D] border-[#F06522] font-bold'
                        : 'bg-[#1A1A1A] text-[#A7A29A] border-[#2E2E2E] hover:border-[#666666] hover:text-[#F1ECE2]'
                    }`}
                  >
                    {lang}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Section 3: Technical Domains & Specializations */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2">
                <Sliders size={16} className="text-[#5B86F2]" />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  DOMAIN INTERESTS
                </h2>
              </div>
              <span className="font-mono text-xs text-[#5B86F2] font-bold">
                {preferences.preferredDomains?.length || 0} SELECTED
              </span>
            </div>

            <div className="flex flex-wrap gap-2 pt-2">
              {AVAILABLE_DOMAINS.map((domain) => {
                const isSelected = preferences.preferredDomains?.includes(domain);
                return (
                  <button
                    key={domain}
                    type="button"
                    onClick={() => toggleInterest(domain)}
                    className={`px-3 py-1.5 font-mono text-xs uppercase tracking-wider border transition-colors ${
                      isSelected
                        ? 'bg-[#5B86F2] text-[#0D0D0D] border-[#5B86F2] font-bold'
                        : 'bg-[#1A1A1A] text-[#A7A29A] border-[#2E2E2E] hover:border-[#666666] hover:text-[#F1ECE2]'
                    }`}
                  >
                    {domain}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Section 4: Target Difficulty Level */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2">
                <Sparkles size={16} className="text-[#F4C13D]" />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  TARGET CONTRIBUTION LEVEL
                </h2>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
              {EXPERIENCE_LEVELS.map((lvl) => {
                const isSelected = preferences.difficulty === lvl.id;
                return (
                  <div
                    key={lvl.id}
                    onClick={() => setPreferences({ ...preferences, difficulty: lvl.id })}
                    className={`p-4 border cursor-pointer transition-colors space-y-2 ${
                      isSelected
                        ? 'bg-[#1B1917] border-[#F4C13D]'
                        : 'bg-[#1A1A1A] border-[#2E2E2E] hover:border-[#444444]'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-heading text-lg text-[#F1ECE2] uppercase">{lvl.label}</span>
                      <Badge variant={isSelected ? 'yellow' : 'surface'} size="xs">
                        {lvl.badge}
                      </Badge>
                    </div>
                    <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                      {lvl.desc}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 5: Workspace Telemetry & Storage Footprint */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-4">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2">
                <Layers size={16} className="text-[#00D26A]" />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  WORKSPACE STORAGE &amp; DATA VAULT
                </h2>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 font-mono text-xs">
              <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3 space-y-1">
                <span className="text-[10px] text-[#A7A29A] uppercase block">SAVED ISSUES CRATE</span>
                <span className="font-heading text-2xl text-[#F06522]">{savedIssues.length}</span>
                <span className="text-[10px] text-[#666666] block">Bookmarked candidate records</span>
              </div>

              <div className="border border-[#2E2E2E] bg-[#1A1A1A] p-3 space-y-1">
                <span className="text-[10px] text-[#A7A29A] uppercase block">CONTRIBUTION LEDGER</span>
                <span className="font-heading text-2xl text-[#5B86F2]">{contributions.length}</span>
                <span className="text-[10px] text-[#666666] block">Active lifecycle records</span>
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-[#222222]">
              <div className="font-mono text-xs text-[#A7A29A]">
                DATA PORTABILITY: Export your complete contributor vault anytime as JSON.
              </div>
              <Button variant="outline" size="sm" onClick={handleExportFullVault}>
                <Download size={13} className="mr-1.5" />
                EXPORT FULL VAULT (JSON)
              </Button>
            </div>
          </div>

          {/* Section 6: Account & Privacy */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-5">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 text-[#FF8585]">
              <div className="flex items-center space-x-2">
                <Shield size={16} />
                <h2 className="font-heading text-xl text-[#F1ECE2] uppercase">
                  ACCOUNT &amp; PRIVACY
                </h2>
              </div>
              <Badge variant="red" size="xs">
                SECURITY CONTROLS
              </Badge>
            </div>

            {/* Connected GitHub Identity Banner */}
            <div className="p-3 bg-[#1A1A1A] border border-[#2A2A2A] flex flex-col sm:flex-row sm:items-center justify-between gap-3 font-mono text-xs">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-[#252525] border border-[#2E2E2E] text-[#F1ECE2]">
                  <Github size={18} />
                </div>
                <div>
                  <div className="text-[10px] text-[#A7A29A] uppercase tracking-wider">
                    CONNECTED GITHUB ACCOUNT
                  </div>
                  <div className="font-bold text-[#F1ECE2] text-sm">
                    @{userProfile?.username || 'unknown'}
                  </div>
                </div>
              </div>
              <div className="text-[11px] text-[#A7A29A] sm:text-right max-w-sm">
                Authenticated via GitHub OAuth. Account deletion purges your OpenSourceContributor data and does not modify your GitHub account.
              </div>
            </div>

            <div className="font-mono text-xs space-y-3 text-[#A7A29A]">
              <p>
                All preferences, bookmarked issues, and contribution logs are securely bound to your authenticated
                account. No data is shared with third-party tracking or advertising networks.
              </p>
              <div className="flex items-center space-x-4 text-[11px] pt-1">
                <Link to={ROUTES.PRIVACY} className="text-[#5B86F2] hover:underline">
                  Privacy Policy
                </Link>
                <span>·</span>
                <Link to={ROUTES.TERMS} className="text-[#5B86F2] hover:underline">
                  Terms of Service
                </Link>
                <span>·</span>
                <Link to={ROUTES.COOKIE_PREFERENCES} className="text-[#5B86F2] hover:underline">
                  Cookie Preferences
                </Link>
              </div>
            </div>

            {/* Action 1: Purge Workspace Telemetry */}
            <div className="border border-[#3D1E1E] bg-[#1F1414] p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 font-mono text-xs">
              <div>
                <span className="text-[#FF8585] font-bold uppercase block mb-1">
                  PURGE WORKSPACE RECORDS
                </span>
                <p className="text-[#A7A29A] text-[11px]">
                  Permanently deletes all saved issues and contribution ledger entries associated with your account while keeping your profile.
                </p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowClearModal(true)}
                className="border-[#FF8585] text-[#FF8585] hover:bg-[#2A1616] shrink-0"
              >
                <Trash2 size={13} className="mr-1.5" />
                CLEAR WORKSPACE DATA
              </Button>
            </div>

            {/* Action 2: Delete My Account */}
            <div className="border-2 border-[#93000A] bg-[#221010] p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 font-mono text-xs">
              <div className="space-y-1">
                <div className="flex items-center space-x-2 text-[#FFDAD6]">
                  <UserX size={15} />
                  <span className="font-heading text-base uppercase text-[#FFDAD6] font-bold">
                    DELETE MY ACCOUNT
                  </span>
                </div>
                <p className="text-[#D0CBC1] text-[11px] leading-relaxed max-w-xl">
                  Permanently deletes your user account, preferences, saved issues crate, contribution records, and AI search quota logs. Active sessions will be terminated immediately.
                </p>
              </div>
              <Button
                variant="danger"
                size="md"
                onClick={handleOpenDeleteModal}
                className="shrink-0 flex items-center gap-1.5 font-bold shadow-lg"
              >
                <Trash2 size={14} />
                <span>DELETE MY ACCOUNT</span>
              </Button>
            </div>
          </div>

          {/* Save Floating Bar */}
          <div className="border-t border-[#2E2E2E] pt-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="font-mono text-xs text-[#A7A29A]">
              {hasUnsavedChanges ? (
                <span className="text-[#F4C13D] font-bold">
                  ● You have unsaved changes in your preferences.
                </span>
              ) : (
                <span>✓ All configuration settings are up to date.</span>
              )}
            </div>

            <div className="flex items-center space-x-3">
              {hasUnsavedChanges && (
                <Button
                  variant="ghost"
                  size="md"
                  onClick={() => setPreferences(initialPreferences)}
                >
                  DISCARD CHANGES
                </Button>
              )}
              <Button type="submit" variant="primary" size="lg" className="flex items-center gap-1.5">
                <Save size={16} />
                <span>SAVE CONFIGURATION</span>
              </Button>
            </div>
          </div>
        </form>
      </main>

      {/* Confirmation Modal: Purge All Workspace Data */}
      {showClearModal && (
        <div
          ref={clearModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="clear-modal-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#FF8585] p-6 max-w-md w-full space-y-4 shadow-2xl animate-in fade-in max-h-[90vh] overflow-y-auto">
            <div className="flex items-center space-x-2 text-[#FF8585] font-mono text-xs font-bold uppercase tracking-wider">
              <AlertCircle size={16} />
              <span>DANGER ZONE CONFIRMATION</span>
            </div>
            <h3 id="clear-modal-title" className="font-heading text-2xl text-[#F1ECE2] uppercase">
              PURGE ALL WORKSPACE DATA?
            </h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              This action will permanently erase your {savedIssues.length} saved issues and
              {contributions.length} contribution ledger records from the database. This action cannot be undone.
            </p>
            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-[#2E2E2E]">
              <Button variant="secondary" size="sm" onClick={() => setShowClearModal(false)}>
                CANCEL
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleConfirmClearWorkspace}
                className="bg-[#2A1616] border-[#FF8585] text-[#FF8585] hover:bg-[#3D1E1E]"
              >
                YES, PURGE WORKSPACE
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal: Reset Preferences to Defaults */}
      {showResetModal && (
        <div
          ref={resetModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="reset-modal-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/85 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#F4C13D] p-6 max-w-md w-full space-y-4 shadow-2xl animate-in fade-in max-h-[90vh] overflow-y-auto">
            <div className="flex items-center space-x-2 text-[#F4C13D] font-mono text-xs font-bold uppercase tracking-wider">
              <AlertCircle size={16} />
              <span>RESET CONFIRMATION</span>
            </div>
            <h3 id="reset-modal-title" className="font-heading text-2xl text-[#F1ECE2] uppercase">
              RESET SETTINGS TO DEFAULTS?
            </h3>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              This will restore your language preferences and discovery domains to recommended defaults.
            </p>
            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-[#2E2E2E]">
              <Button variant="secondary" size="sm" onClick={() => setShowResetModal(false)}>
                CANCEL
              </Button>
              <Button variant="yellow" size="sm" onClick={handleConfirmReset}>
                RESET TO DEFAULTS
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal: Permanent Account Deletion */}
      {showDeleteAccountModal && (
        <div
          ref={deleteModalRef}
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-modal-title"
          className="fixed inset-0 z-50 bg-[#0D0D0D]/90 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
        >
          <div className="bg-[#141414] border-2 border-[#DC2626] p-6 max-w-lg w-full space-y-4 shadow-2xl animate-in fade-in my-8 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3">
              <div className="flex items-center space-x-2 text-[#FF8585] font-mono text-xs font-bold uppercase tracking-wider">
                <AlertCircle size={16} />
                <span>PERMANENT ACCOUNT DELETION // DANGER ZONE</span>
              </div>
              <button
                type="button"
                onClick={() => setShowDeleteAccountModal(false)}
                className="text-[#A7A29A] hover:text-[#F1ECE2] transition-colors p-2 min-h-[44px] min-w-[44px] flex items-center justify-center"
                aria-label="Close delete account modal"
                disabled={isDeletingAccount}
              >
                <X size={18} />
              </button>
            </div>

            <h3 id="delete-modal-title" className="font-heading text-2xl text-[#F1ECE2] uppercase tracking-tight">
              DELETE OPENSOURCECONTRIBUTOR ACCOUNT?
            </h3>

            <div className="bg-[#1F1414] border border-[#3D1E1E] p-3 text-[#FFDAD6] font-mono text-xs space-y-1.5">
              <div className="font-bold uppercase tracking-wider flex items-center gap-1.5 text-[#FF8585]">
                <AlertCircle size={14} />
                <span>THIS ACTION IS PERMANENT AND CANNOT BE UNDONE</span>
              </div>
              <p className="text-[11px] leading-relaxed text-[#D0CBC1]">
                Executing account deletion will permanently erase:
              </p>
              <ul className="list-disc list-inside text-[11px] text-[#A7A29A] space-y-0.5 pl-1">
                <li>Your contributor identity, profile attributes, and GitHub ID reference</li>
                <li>All language runtimes, domain preferences, and skill level configurations</li>
                <li>All {savedIssues.length} saved issues in your bookmarked Crate</li>
                <li>All {contributions.length} issue contribution lifecycle records in your Ledger</li>
                <li>All daily AI semantic search usage counts and quota tracking history</li>
                <li>All active browser sessions across all devices (invalidated immediately)</li>
              </ul>
            </div>

            <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-3 font-mono text-[11px] text-[#A7A29A] space-y-1">
              <span className="text-[#F1ECE2] font-bold block uppercase">
                EXTERNAL GITHUB ACCOUNT INTEGRITY
              </span>
              <p>
                Your external GitHub account (<span className="text-[#F4C13D]">@{userProfile?.username}</span>) is completely separate and will <strong>not</strong> be modified, deleted, or altered.
              </p>
            </div>

            <div className="bg-[#1A1A1A] border border-[#2E2E2E] p-3 font-mono text-[11px] text-[#A7A29A] space-y-1">
              <span className="text-[#F1ECE2] font-bold block uppercase">
                DATA RETENTION &amp; HOSTING LOGS
              </span>
              <p>
                As detailed in our <Link to={ROUTES.PRIVACY} className="text-[#5B86F2] underline" target="_blank" rel="noopener noreferrer">Privacy Policy</Link>, deleting your account permanently purges all personal data from our PostgreSQL database and expires in-memory rate-limiting data. It does not remove external network and connection logs maintained independently by the hosting provider.
              </p>
            </div>

            {/* Explicit Confirmation Input */}
            <div className="space-y-2 pt-2">
              <label htmlFor="delete-confirm-username" className="block font-mono text-xs text-[#F1ECE2]">
                To confirm permanent deletion, type your GitHub username{' '}
                <span className="text-[#F06522] font-bold select-all">
                  {userProfile?.username}
                </span>{' '}
                below:
              </label>
              <input
                id="delete-confirm-username"
                type="text"
                value={deleteConfirmUsername}
                onChange={(e) => setDeleteConfirmUsername(e.target.value)}
                placeholder={userProfile?.username || ''}
                className="w-full bg-[#0D0D0D] border border-[#2E2E2E] focus:border-[#DC2626] focus:ring-1 focus:ring-[#DC2626] px-3 py-2 font-mono text-xs text-[#F1ECE2] placeholder-[#666666] outline-none"
                autoComplete="off"
                spellCheck="false"
                disabled={isDeletingAccount}
              />
            </div>

            {deleteError && (
              <div className="bg-[#2A1616] border border-[#DC2626] p-2.5 font-mono text-xs text-[#FF8585] flex items-center space-x-2">
                <AlertCircle size={14} className="shrink-0" />
                <span>{deleteError}</span>
              </div>
            )}

            <div className="flex items-center justify-end space-x-3 pt-4 border-t border-[#2E2E2E]">
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setShowDeleteAccountModal(false)}
                disabled={isDeletingAccount}
              >
                CANCEL
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleDeleteAccount}
                disabled={deleteConfirmUsername.trim() !== (userProfile?.username || '') || isDeletingAccount}
                className="flex items-center gap-1.5"
              >
                {isDeletingAccount ? (
                  <>
                    <Loader2 size={13} className="animate-spin" />
                    <span>DELETING ACCOUNT...</span>
                  </>
                ) : (
                  <>
                    <Trash2 size={13} />
                    <span>PERMANENTLY DELETE ACCOUNT</span>
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default SettingsPage;
