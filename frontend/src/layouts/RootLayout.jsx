import React from 'react';
import { Outlet } from 'react-router-dom';
import { AppHeader } from '../components/navigation/AppHeader';
import { AppFooter } from '../components/navigation/AppFooter';
import { CookieConsentBanner } from '../components/common/CookieConsentBanner';

export function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col bg-[#0D0D0D] text-[#F1ECE2] antialiased selection:bg-[#F06522] selection:text-[#0D0D0D]">
      {/* Main Header */}
      <AppHeader />

      {/* Page Content */}
      <main className="flex-1 flex flex-col">
        <Outlet />
      </main>

      {/* Main Footer */}
      <AppFooter />

      {/* Cookie Consent Banner */}
      <CookieConsentBanner />
    </div>
  );
}
