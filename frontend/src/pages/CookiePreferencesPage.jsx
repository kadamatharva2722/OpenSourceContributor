import React, { useState, useEffect } from 'react';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { CheckCircle2, Cookie, ShieldCheck, Info } from 'lucide-react';

const CONSENT_KEY = 'osc_cookie_consent';
const PREFS_KEY = 'osc_cookie_prefs';

export function CookiePreferencesPage() {
  useDocumentTitle('Cookie Preferences');
  const [preferences, setPreferences] = useState({
    essential: true, // always required
    offlineCache: true,
    analytics: false,
  });
  const [saved, setSaved] = useState(false);
  const [savedStatus, setSavedStatus] = useState(null);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(CONSENT_KEY) || localStorage.getItem(PREFS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        setPreferences({
          essential: true,
          offlineCache: parsed.offlineCache !== undefined ? Boolean(parsed.offlineCache) : (parsed.themeCache ?? true),
          analytics: parsed.analytics !== undefined ? Boolean(parsed.analytics) : (parsed.telemetry ?? false),
        });
        setSavedStatus(parsed.status || 'custom');
      }
    } catch {
      // LocalStorage access may be restricted
    }
  }, []);

  const persist = (updatedPrefs, status = 'custom') => {
    const payload = {
      status,
      essential: true,
      offlineCache: Boolean(updatedPrefs.offlineCache),
      analytics: Boolean(updatedPrefs.analytics),
      timestamp: new Date().toISOString(),
    };
    try {
      const serialized = JSON.stringify(payload);
      localStorage.setItem(CONSENT_KEY, serialized);
      localStorage.setItem(PREFS_KEY, serialized);
    } catch {
      // LocalStorage write failure
    }
    setPreferences(updatedPrefs);
    setSavedStatus(status);
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  const handleSave = (e) => {
    e.preventDefault();
    persist(preferences, 'custom');
  };

  const handleAcceptAll = () => {
    persist({ essential: true, offlineCache: true, analytics: true }, 'accepted');
  };

  const handleEssentialsOnly = () => {
    persist({ essential: true, offlineCache: false, analytics: false }, 'essentials_only');
  };

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      <section className="border-b border-[#2E2E2E] bg-[#141414] px-4 md:px-8 py-12">
        <div className="max-w-4xl mx-auto space-y-2">
          <div className="flex items-center space-x-2 text-xs font-mono text-[#F4C13D] uppercase tracking-widest">
            <Cookie size={14} />
            <span>STORAGE PREFERENCES // PRIVACY CONTROLS</span>
          </div>
          <h1 className="font-heading text-4xl uppercase tracking-tight text-[#F1ECE2]">
            COOKIE & STORAGE PREFERENCES
          </h1>
          <p className="font-mono text-xs text-[#A7A29A]">
            Manage browser local storage, session state, and telemetry caching. Source of truth is unified across all preferences.
          </p>
        </div>
      </section>

      <section className="px-4 md:px-8 py-12 max-w-4xl mx-auto w-full">
        {/* Quick action buttons */}
        <div className="flex flex-wrap items-center justify-between gap-4 p-4 mb-6 border border-[#2E2E2E] bg-[#141414]">
          <div className="flex items-center space-x-2 text-xs font-mono text-[#A7A29A]">
            <ShieldCheck size={16} className="text-[#F4C13D]" />
            <span>CURRENT STATUS: <strong className="text-[#F1ECE2] uppercase">{savedStatus || 'NOT YET SET'}</strong></span>
          </div>
          <div className="flex items-center gap-3">
            <Button
              type="button"
              variant="secondary"
              size="sm"
              onClick={handleEssentialsOnly}
              className="text-xs"
            >
              ESSENTIALS ONLY
            </Button>
            <Button
              type="button"
              variant="primary"
              size="sm"
              onClick={handleAcceptAll}
              className="text-xs font-bold"
            >
              ACCEPT ALL
            </Button>
          </div>
        </div>

        <form onSubmit={handleSave} className="space-y-6">
          {saved && (
            <div className="bg-[#182319] border border-[#2E7D32] p-4 flex items-center space-x-3 text-xs font-mono text-[#E8F5E9]">
              <CheckCircle2 size={16} className="text-[#2E7D32]" />
              <span>STORAGE PREFERENCES UPDATED AND RECORDED TO osc_cookie_consent.</span>
            </div>
          )}

          {/* Essential Storage */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-2">
            <div className="flex items-center justify-between">
              <label htmlFor="essential-storage" className="font-heading text-lg text-[#F1ECE2] cursor-default">
                1. ESSENTIAL SESSION STORAGE
              </label>
              <span className="text-[11px] font-mono text-[#F4C13D] border border-[#F4C13D] px-2 py-0.5">
                REQUIRED
              </span>
            </div>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Maintains your authentication session (<code className="text-[#F4C13D]">JSESSIONID</code>) and security tokens (<code className="text-[#F4C13D]">XSRF-TOKEN</code>). Cannot be disabled without breaking application authentication and security.
            </p>
            <input
              id="essential-storage"
              type="checkbox"
              checked={preferences.essential}
              disabled
              className="sr-only"
              aria-label="Essential storage (required)"
            />
          </div>

          {/* Offline Crate Cache */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-2">
            <div className="flex items-center justify-between">
              <label htmlFor="offline-crate-cache" className="font-heading text-lg text-[#F1ECE2] cursor-pointer">
                2. OFFLINE CRATE CACHE (LOCALSTORAGE)
              </label>
              <label htmlFor="offline-crate-cache" className="relative inline-flex items-center cursor-pointer">
                <input
                  id="offline-crate-cache"
                  type="checkbox"
                  checked={preferences.offlineCache}
                  onChange={(e) => setPreferences({ ...preferences, offlineCache: e.target.checked })}
                  className="rounded-none border-[#2E2E2E] text-[#F06522] focus:ring-0 cursor-pointer h-4 w-4"
                />
              </label>
            </div>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Persists your bookmarked issues across browser sessions even when disconnected from GitHub APIs.
            </p>
          </div>

          {/* Analytics / Telemetry */}
          <div className="border border-[#2E2E2E] bg-[#141414] p-6 space-y-2">
            <div className="flex items-center justify-between">
              <label htmlFor="analytics-telemetry" className="font-heading text-lg text-[#F1ECE2] cursor-pointer">
                3. ANONYMIZED PERFORMANCE TELEMETRY
              </label>
              <label htmlFor="analytics-telemetry" className="relative inline-flex items-center cursor-pointer">
                <input
                  id="analytics-telemetry"
                  type="checkbox"
                  checked={preferences.analytics}
                  onChange={(e) => setPreferences({ ...preferences, analytics: e.target.checked })}
                  className="rounded-none border-[#2E2E2E] text-[#F06522] focus:ring-0 cursor-pointer h-4 w-4"
                />
              </label>
            </div>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Enables anonymous query latency and cache performance metrics if telemetry is enabled by the platform owner.
            </p>
            <div className="flex items-center space-x-2 pt-1 text-[11px] font-mono text-[#A7A29A]">
              <Info size={13} className="text-[#F4C13D]" />
              <span>NOTE: No third-party tracking scripts are active. Analytics remains strictly dormant until configured.</span>
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <Button type="submit" variant="primary" size="md">
              SAVE CUSTOM PREFERENCES
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default CookiePreferencesPage;
