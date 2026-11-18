import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../../constants/routes';
import { Button } from '../ui/Button';
import { Cookie } from 'lucide-react';

const CONSENT_KEY = 'osc_cookie_consent';
const PREFS_KEY = 'osc_cookie_prefs';

export function CookieConsentBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    try {
      const consent = localStorage.getItem(CONSENT_KEY);
      if (!consent) {
        setVisible(true);
      }
    } catch {
      // LocalStorage unavailable in restricted iframe/browser modes
      setVisible(false);
    }
  }, []);

  const saveConsent = (status, offlineCache) => {
    const payload = {
      status,
      essential: true,
      offlineCache,
      analytics: false, // Analytics remains strictly disabled until configured by platform owner
      timestamp: new Date().toISOString(),
    };
    try {
      const serialized = JSON.stringify(payload);
      localStorage.setItem(CONSENT_KEY, serialized);
      localStorage.setItem(PREFS_KEY, serialized);
    } catch {
      // Ignore storage errors
    }
    setVisible(false);
  };

  if (!visible) return null;

  return (
    <div
      role="region"
      aria-label="Cookie and storage consent banner"
      className="fixed bottom-0 left-0 right-0 z-50 bg-[#141414] border-t-2 border-[#F06522] p-4 md:p-5 shadow-2xl"
    >
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-start md:items-center justify-between gap-4 font-mono text-xs">
        <div className="flex items-start space-x-3 text-[#D0CBC1]">
          <Cookie size={18} className="text-[#F4C13D] shrink-0 mt-0.5" />
          <p className="leading-relaxed">
            We use strictly essential first-party session and security cookies (<code className="text-[#F4C13D]">JSESSIONID</code>,{' '}
            <code className="text-[#F4C13D]">XSRF-TOKEN</code>) to authenticate contributor sessions and prevent CSRF. Zero third-party marketing or cross-site tracking cookies are used.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5 shrink-0 self-end md:self-auto">
          <Link
            to={ROUTES.COOKIE_PREFERENCES}
            className="text-[#A7A29A] hover:text-[#F1ECE2] uppercase text-[11px] underline px-2 py-1"
          >
            CUSTOMIZE
          </Link>
          <Button
            variant="secondary"
            size="sm"
            onClick={() => saveConsent('essentials_only', false)}
            className="whitespace-nowrap text-[11px]"
          >
            ESSENTIALS ONLY
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={() => saveConsent('accepted', true)}
            className="font-bold whitespace-nowrap text-[11px]"
          >
            ACCEPT ALL
          </Button>
        </div>
      </div>
    </div>
  );
}

export default CookieConsentBanner;
