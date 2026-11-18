import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams, Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { getAuthStatus, getOAuthLoginUrl } from '../services/authService';
import {
  Lock,
  ArrowRight,
  Shield,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ExternalLink,
  SlidersHorizontal,
  Compass,
  FileText,
  RotateCcw,
  Sparkles,
} from 'lucide-react';

export function LoginPage() {
  useDocumentTitle('Sign In');
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  // Return destination after login
  const returnUrl = searchParams.get('returnUrl') || location.state?.from?.pathname || ROUTES.DASHBOARD;
  const initialError = searchParams.get('error');

  // Auth UI state: 'default' | 'loading' | 'redirect' | 'success' | 'failure' | 'cancelled'
  const [authState, setAuthState] = useState(() => {
    if (initialError) return 'failure';
    return 'default';
  });

  // Verify active authentication session on mount (e.g. returning from OAuth redirect)
  useEffect(() => {
    let isMounted = true;

    async function verifyAuthSession() {
      try {
        const status = await getAuthStatus();
        if (!isMounted) return;

        if (status && status.authenticated) {
          setAuthState('success');
          const timer = setTimeout(() => {
            if (isMounted) {
              navigate(returnUrl, { replace: true });
            }
          }, 600);
          return () => clearTimeout(timer);
        } else {
          if (initialError) {
            setAuthState('failure');
          } else {
            setAuthState('default');
          }
        }
      } catch (err) {
        if (!isMounted) return;
        if (initialError) {
          setAuthState('failure');
        } else {
          setAuthState('default');
        }
      }
    }

    verifyAuthSession();
    return () => {
      isMounted = false;
    };
  }, [navigate, returnUrl, initialError]);

  // Handle click on primary "Continue with GitHub" button
  const handleInitiateGitHubAuth = () => {
    setAuthState('loading');

    const timer1 = setTimeout(() => {
      setAuthState('redirect');
      window.location.href = getOAuthLoginUrl();
    }, 400);

    return () => clearTimeout(timer1);
  };

  return (
    <div className="flex-1 flex flex-col bg-[#0D0D0D] text-[#F1ECE2]">
      {/* Top Banner / Ticker Strip */}
      <section className="w-full bg-[#F4C13D] text-[#0D0D0D] px-4 md:px-8 py-1.5 flex items-center justify-between overflow-hidden text-xs font-mono font-bold uppercase tracking-wider select-none border-b border-[#2E2E2E]">
        <div className="flex items-center space-x-3 whitespace-nowrap truncate">
          <span className="inline-flex items-center space-x-1.5 bg-[#0D0D0D] text-[#F4C13D] px-2 py-0.5 text-[10px]">
            <span className="w-1.5 h-1.5 bg-[#F4C13D] inline-block" />
            <span>AUTHENTICATION</span>
          </span>
          <span className="hidden md:inline text-[11px] opacity-90">·</span>
          <span className="hidden md:inline text-[11px] opacity-90">
            SECURE GITHUB OAUTH 2.0
          </span>
          <span className="hidden md:inline text-[11px] opacity-90">·</span>
          <span className="hidden md:inline text-[11px] opacity-90">
            READ-ONLY IDENTITY SCOPE (ZERO REPOSITORY WRITE PERMISSIONS)
          </span>
        </div>
      </section>

      {/* Main Workspace Container */}
      <div className="w-full flex-1 flex flex-col items-center px-4 md:px-8 py-8 md:py-12">
        <div className="w-full max-w-6xl flex flex-col gap-6">

          {/* ============================================================== */}
          {/* MAIN TWO-COLUMN CARD LAYOUT (55% Left / 45% Right)             */}
          {/* ============================================================== */}
          <div className="grid grid-cols-1 lg:grid-cols-12 border border-[#2E2E2E] bg-[#0E0E0E]">

            {/* ------------------------------------------------------------ */}
            {/* LEFT COLUMN: Editorial Value Proposition (~55% width -> col 7) */}
            {/* ------------------------------------------------------------ */}
            <div className="lg:col-span-7 p-6 md:p-10 lg:p-12 border-b lg:border-b-0 lg:border-r border-[#2E2E2E] bg-[#141414] flex flex-col justify-between gap-8">
              <div className="space-y-6">
                {/* Micro Label */}
                <div className="flex items-center gap-2 font-mono text-xs text-[#A7A29A] uppercase tracking-wider">
                  <span className="w-2.5 h-2.5 bg-[#F06522] inline-block" />
                  <span>OPEN SOURCE DISCOVERY / AUTHENTICATION</span>
                </div>

                {/* Massive Anton Headline */}
                <h1 className="font-heading text-4xl sm:text-5xl md:text-6xl lg:text-7xl uppercase text-[#F1ECE2] tracking-tight leading-[0.9]">
                  YOUR NEXT<br />
                  <span className="text-[#F06522]">CONTRIBUTION</span><br />
                  STARTS HERE.
                </h1>

                {/* Subtitle */}
                <p className="font-mono text-xs sm:text-sm text-[#A7A29A] max-w-xl leading-relaxed">
                  Discover open-source projects, find issues that match your skills, and build your journey as a contributor.
                </p>

                {/* Three Benefit Rows with Colored Rectangular Markers */}
                <div className="space-y-3 pt-4 border-t border-[#222222]">
                  {/* Row 1: Orange */}
                  <div className="flex items-start gap-4 p-3 bg-[#0E0E0E] border border-[#222222]">
                    <span className="w-3 h-5 bg-[#F06522] shrink-0 mt-0.5 inline-block" />
                    <div className="space-y-0.5">
                      <span className="font-mono text-xs text-[#F1ECE2] font-bold uppercase tracking-wider block">
                        DISCOVER
                      </span>
                      <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                        Find relevant opportunities across open-source projects.
                      </p>
                    </div>
                  </div>

                  {/* Row 2: Mustard */}
                  <div className="flex items-start gap-4 p-3 bg-[#0E0E0E] border border-[#222222]">
                    <span className="w-3 h-5 bg-[#F4C13D] shrink-0 mt-0.5 inline-block" />
                    <div className="space-y-0.5">
                      <span className="font-mono text-xs text-[#F1ECE2] font-bold uppercase tracking-wider block">
                        SAVE
                      </span>
                      <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                        Keep interesting GitHub issues in your personal collection.
                      </p>
                    </div>
                  </div>

                  {/* Row 3: Blue */}
                  <div className="flex items-start gap-4 p-3 bg-[#0E0E0E] border border-[#222222]">
                    <span className="w-3 h-5 bg-[#5B86F2] shrink-0 mt-0.5 inline-block" />
                    <div className="space-y-0.5">
                      <span className="font-mono text-xs text-[#F1ECE2] font-bold uppercase tracking-wider block">
                        CONTRIBUTE
                      </span>
                      <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                        Organise and track your contribution journey.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Public Browsing Reassurance Stamp */}
              <div className="pt-4 border-t border-[#222222] font-mono text-xs text-[#666666] leading-relaxed flex items-start gap-2">
                <Compass size={15} className="text-[#F4C13D] shrink-0 mt-0.5" />
                <span>
                  Browsing, searching, and filtering public repositories does not require authentication. Sign in is only needed to persist saved crates, track contributions, and synchronize developer preferences.
                </span>
              </div>
            </div>

            {/* ------------------------------------------------------------ */}
            {/* RIGHT COLUMN: Authentication Panel (~45% width -> col 5)     */}
            {/* ------------------------------------------------------------ */}
            <div className="lg:col-span-5 p-6 md:p-8 lg:p-10 flex flex-col justify-between gap-6 bg-[#161616]">
              <div className="space-y-6">
                {/* Header rail */}
                <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 font-mono text-xs">
                  <span className="text-[#F4C13D] font-bold uppercase tracking-widest">
                    GITHUB AUTHENTICATION / 01
                  </span>
                  <span className="text-[#666666] uppercase">PASSTHROUGH</span>
                </div>

                {/* Section Title */}
                <div className="space-y-1">
                  <h2 className="font-heading text-2xl md:text-3xl uppercase text-[#F1ECE2] tracking-tight">
                    WELCOME TO THE COMMUNITY.
                  </h2>
                  <p className="font-mono text-xs text-[#A7A29A]">
                    Connect your GitHub account to get started.
                  </p>
                </div>

                {/* ========================================================== */}
                {/* AUTH STATE VIEWS CONTAINER                                 */}
                {/* ========================================================== */}
                <div className="space-y-4 pt-2">

                  {/* STATE 1: DEFAULT */}
                  {authState === 'default' && (
                    <div className="space-y-3">
                      <button
                        type="button"
                        onClick={handleInitiateGitHubAuth}
                        className="group w-full h-[58px] bg-[#F06522] hover:bg-[#F4C13D] text-[#0D0D0D] transition-colors flex items-center justify-between px-6 border border-[#F06522] hover:border-[#F4C13D] cursor-pointer"
                      >
                        <div className="flex items-center gap-3">
                          <svg
                            aria-hidden="true"
                            className="w-6 h-6 fill-current transition-transform group-hover:scale-110 shrink-0"
                            viewBox="0 0 24 24"
                          >
                            <path
                              fillRule="evenodd"
                              clipRule="evenodd"
                              d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                            />
                          </svg>
                          <span className="font-mono text-sm tracking-wider font-bold uppercase">
                            CONTINUE WITH GITHUB
                          </span>
                        </div>
                        <ArrowRight size={18} className="group-hover:translate-x-1 transition-transform" />
                      </button>

                      <div className="flex items-center gap-1.5 text-[#666666] font-mono text-xs uppercase justify-center pt-1">
                        <Lock size={13} className="text-[#F4C13D]" />
                        <span>SECURE AUTHENTICATION POWERED BY GITHUB</span>
                      </div>
                    </div>
                  )}

                  {/* STATE 2: LOADING */}
                  {authState === 'loading' && (
                    <div className="space-y-3">
                      <div className="w-full h-[58px] bg-[#1F1F1F] border border-[#2E2E2E] flex items-center justify-center gap-3 px-6 select-none">
                        <div className="w-4 h-4 border-2 border-[#F4C13D] border-t-transparent animate-spin" />
                        <span className="font-mono text-xs font-bold uppercase tracking-wider text-[#F4C13D]">
                          CONNECTING TO GITHUB...
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5 text-[#666666] font-mono text-xs uppercase justify-center">
                        <span className="w-1.5 h-1.5 bg-[#F4C13D] animate-pulse inline-block" />
                        <span>INITIATING SECURE ENCRYPTED HANDSHAKE...</span>
                      </div>
                    </div>
                  )}

                  {/* STATE 3: REDIRECTING */}
                  {authState === 'redirect' && (
                    <div className="space-y-3">
                      <div className="w-full h-[58px] bg-[#0E0E0E] border border-[#F4C13D] text-[#F4C13D] flex items-center justify-between px-6 select-none font-mono">
                        <div className="flex items-center gap-2">
                          <ExternalLink size={18} className="animate-pulse" />
                          <span className="text-xs font-bold uppercase tracking-wider">
                            REDIRECTING TO GITHUB...
                          </span>
                        </div>
                        <span className="text-[11px] text-[#A7A29A]">AUTH.GITHUB.COM</span>
                      </div>

                      <div className="flex items-center gap-1.5 text-[#A7A29A] font-mono text-[11px] uppercase justify-center text-center">
                        <span>TRANSFERRING SESSION TO GITHUB AUTHORIZATION PORTAL</span>
                      </div>

                      <div className="pt-2 text-center">
                        <button
                          type="button"
                          onClick={() => setAuthState('default')}
                          className="font-mono text-xs text-[#F06522] hover:underline uppercase font-bold"
                        >
                          CANCEL REDIRECT &amp; RETURN
                        </button>
                      </div>
                    </div>
                  )}

                  {/* STATE 4: SUCCESS (Session active indication) */}
                  {authState === 'success' && (
                    <div className="space-y-3 font-mono">
                      <div className="w-full h-[58px] bg-[#122415] border border-[#2E7D32] text-[#2E7D32] flex items-center justify-between px-6 select-none">
                        <div className="flex items-center gap-2">
                          <CheckCircle2 size={18} className="text-[#2E7D32]" />
                          <span className="text-xs font-bold uppercase tracking-wider text-[#F1ECE2]">
                            AUTHENTICATED
                          </span>
                        </div>
                        <span className="px-2 py-0.5 bg-[#2E7D32] text-[#0D0D0D] text-[10px] font-bold uppercase">
                          SUCCESS
                        </span>
                      </div>

                      <div className="flex items-center justify-between p-3 bg-[#0E0E0E] border border-[#2E2E2E] text-xs">
                        <span className="text-[#A7A29A]">WORKSPACE INITIALIZED</span>
                        <Link
                          to={returnUrl}
                          className="text-[#F4C13D] hover:underline font-bold uppercase flex items-center gap-1"
                        >
                          <span>ENTER PLATFORM</span>
                          <ArrowRight size={13} />
                        </Link>
                      </div>
                    </div>
                  )}

                  {/* STATE 5: FAILURE */}
                  {authState === 'failure' && (
                    <div className="space-y-3 font-mono">
                      <div className="w-full p-4 bg-[#261010] border border-[#B71C1C] text-[#FF8A80] flex items-start gap-3">
                        <AlertTriangle size={18} className="text-[#FF5252] shrink-0 mt-0.5" />
                        <div className="space-y-1">
                          <span className="text-xs font-bold uppercase block text-[#FF5252]">
                            AUTHENTICATION FAILED
                          </span>
                          <span className="text-[11px] text-[#A7A29A] leading-relaxed block">
                            Handshake timed out or provider rejected token exchange. Please check network connectivity and retry.
                          </span>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => setAuthState('default')}
                        className="w-full h-10 bg-[#F06522] hover:bg-[#F4C13D] text-[#0D0D0D] font-mono text-xs font-bold uppercase transition-colors flex items-center justify-center gap-2"
                      >
                        <RotateCcw size={14} />
                        <span>RETRY AUTHENTICATION</span>
                      </button>
                    </div>
                  )}

                  {/* STATE 6: CANCELLED */}
                  {authState === 'cancelled' && (
                    <div className="space-y-3 font-mono">
                      <div className="w-full p-4 bg-[#141414] border border-[#2E2E2E] text-[#A7A29A] flex items-start gap-3">
                        <XCircle size={18} className="text-[#F4C13D] shrink-0 mt-0.5" />
                        <div className="space-y-1">
                          <span className="text-xs font-bold uppercase block text-[#F1ECE2]">
                            USER CANCELLED AUTHENTICATION
                          </span>
                          <span className="text-[11px] text-[#888888] leading-relaxed block">
                            The GitHub sign-in window was closed before completion. No profile credentials were exchanged.
                          </span>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => setAuthState('default')}
                        className="w-full h-10 bg-[#1C1C1C] hover:bg-[#2A2A2A] text-[#F1ECE2] font-mono text-xs font-bold uppercase transition-colors flex items-center justify-center gap-2 border border-[#2E2E2E]"
                      >
                        <RotateCcw size={14} />
                        <span>RETURN TO LOGIN FORM</span>
                      </button>
                    </div>
                  )}
                </div>

                {/* Permissions & Security Note */}
                <div className="p-3 bg-[#0E0E0E] border-l-2 border-[#F4C13D] font-mono text-xs text-[#A7A29A] leading-relaxed">
                  We only request the permissions needed to provide the features you choose to use. Zero repository write access or source code mutation is ever requested.
                </div>
              </div>

              {/* Bottom Onboarding & Legal Disclaimer */}
              <div className="space-y-4 pt-4 border-t border-[#2E2E2E] font-mono">
                <div className="space-y-1">
                  <span className="text-xs text-[#F4C13D] font-bold uppercase tracking-wider block">
                    NEW TO OPEN SOURCE?
                  </span>
                  <p className="text-xs text-[#A7A29A] leading-normal">
                    Start exploring opportunities and discover where your skills fit.
                  </p>
                  <Link
                    to={ROUTES.EXPLORE}
                    className="inline-flex items-center gap-1 text-xs text-[#F06522] hover:text-[#F4C13D] font-bold uppercase tracking-wider pt-1 transition-colors"
                  >
                    <span>EXPLORE ISSUES</span>
                    <ArrowRight size={13} />
                  </Link>
                </div>

                <div className="pt-3 border-t border-[#222222] text-[11px] text-[#666666] uppercase tracking-wider leading-relaxed">
                  BY CONTINUING, YOU AGREE TO OUR{' '}
                  <Link
                    to={ROUTES.TERMS}
                    className="text-[#F1ECE2] hover:text-[#F4C13D] underline underline-offset-4 transition-colors"
                  >
                    TERMS
                  </Link>{' '}
                  AND{' '}
                  <Link
                    to={ROUTES.PRIVACY}
                    className="text-[#F1ECE2] hover:text-[#F4C13D] underline underline-offset-4 transition-colors"
                  >
                    PRIVACY POLICY
                  </Link>.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
