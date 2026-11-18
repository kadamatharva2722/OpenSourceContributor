import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Analytics } from '@vercel/analytics/react';
import { ROUTES } from './constants/routes';
import { useBackendWake } from './hooks/useBackendWake';

// Layout
import { RootLayout } from './layouts/RootLayout';

// Auth Protection
import { ProtectedRoute } from './components/auth/ProtectedRoute';

// Core Discovery Pages (Eagerly Loaded for Immediate First Paint)
import { HomePage } from './pages/HomePage';
import { ExplorePage } from './pages/ExplorePage';

// Lazy Loaded Auxiliary Pages for Optimal Code-Splitting and Fast First Load
const IssueDetailsPage = lazy(() => import('./pages/IssueDetailsPage').then((m) => ({ default: m.IssueDetailsPage })));
const HowItWorksPage = lazy(() => import('./pages/HowItWorksPage').then((m) => ({ default: m.HowItWorksPage })));
const AboutPage = lazy(() => import('./pages/AboutPage').then((m) => ({ default: m.AboutPage })));
const ContactPage = lazy(() => import('./pages/ContactPage').then((m) => ({ default: m.ContactPage })));
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const AuthCallbackPage = lazy(() => import('./pages/AuthCallbackPage').then((m) => ({ default: m.AuthCallbackPage })));
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const SavedIssuesPage = lazy(() => import('./pages/SavedIssuesPage').then((m) => ({ default: m.SavedIssuesPage })));
const MyContributionsPage = lazy(() => import('./pages/MyContributionsPage').then((m) => ({ default: m.MyContributionsPage })));
const SettingsPage = lazy(() => import('./pages/SettingsPage').then((m) => ({ default: m.SettingsPage })));
const PrivacyPage = lazy(() => import('./pages/PrivacyPage').then((m) => ({ default: m.PrivacyPage })));
const TermsPage = lazy(() => import('./pages/TermsPage').then((m) => ({ default: m.TermsPage })));
const CookiePreferencesPage = lazy(() => import('./pages/CookiePreferencesPage').then((m) => ({ default: m.CookiePreferencesPage })));
const ThankYouPage = lazy(() => import('./pages/ThankYouPage').then((m) => ({ default: m.ThankYouPage })));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })));

function PageLoadingFallback() {
  return (
    <div className="flex-1 flex items-center justify-center p-12 bg-[#0D0D0D] text-[#F1ECE2] font-mono text-xs">
      <div className="flex items-center space-x-3 text-[#F06522] border border-[#2E2E2E] bg-[#141414] px-4 py-3">
        <span className="w-2 h-2 bg-[#F06522] animate-pulse" />
        <span className="uppercase tracking-widest font-bold">LOADING VIEW DISPATCH...</span>
      </div>
    </div>
  );
}

export function App() {
  // Trigger lightweight, non-blocking visitor backend wake-up on application mount
  useBackendWake();

  return (
    <>
      <Suspense fallback={<PageLoadingFallback />}>
        <Routes>
          <Route element={<RootLayout />}>
            {/* Core Discovery Pages */}
            <Route path={ROUTES.HOME} element={<HomePage />} />
            <Route path={ROUTES.EXPLORE} element={<ExplorePage />} />
            <Route path={ROUTES.ISSUE_DETAILS} element={<IssueDetailsPage />} />

            {/* Informational Pages */}
            <Route path={ROUTES.HOW_IT_WORKS} element={<HowItWorksPage />} />
            <Route path={ROUTES.ABOUT} element={<AboutPage />} />
            <Route path={ROUTES.CONTACT} element={<ContactPage />} />

            {/* Auth Pages */}
            <Route path={ROUTES.LOGIN} element={<LoginPage />} />
            <Route path={ROUTES.AUTH_CALLBACK} element={<AuthCallbackPage />} />

            {/* Contributor Workspace Pages (Protected - Requires Backend Session) */}
            <Route element={<ProtectedRoute />}>
              <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
              <Route path={ROUTES.SAVED_ISSUES} element={<SavedIssuesPage />} />
              <Route path="/saved" element={<Navigate to={ROUTES.SAVED_ISSUES} replace />} />
              <Route path={ROUTES.CRATE} element={<Navigate to={ROUTES.SAVED_ISSUES} replace />} />
              <Route path={ROUTES.MY_CONTRIBUTIONS} element={<MyContributionsPage />} />
              <Route path="/contributions" element={<Navigate to={ROUTES.MY_CONTRIBUTIONS} replace />} />
              <Route path={ROUTES.SETTINGS} element={<SettingsPage />} />
            </Route>

            {/* Legal & Archival Pages */}
            <Route path={ROUTES.PRIVACY} element={<PrivacyPage />} />
            <Route path={ROUTES.TERMS} element={<TermsPage />} />
            <Route path={ROUTES.COOKIE_PREFERENCES} element={<CookiePreferencesPage />} />
            <Route path={ROUTES.THANK_YOU} element={<ThankYouPage />} />

            {/* 404 Catch-All */}
            <Route path={ROUTES.NOT_FOUND} element={<NotFoundPage />} />
          </Route>
        </Routes>
      </Suspense>
      <Analytics />
    </>
  );
}

export default App;
