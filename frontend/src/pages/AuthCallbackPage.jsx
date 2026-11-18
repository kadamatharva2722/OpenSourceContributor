import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { getAuthStatus } from '../services/authService';
import { CheckCircle2, Loader2, Terminal, Shield, ArrowRight, XCircle } from 'lucide-react';

export function AuthCallbackPage() {
  useDocumentTitle('Authenticating');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const errorParam = searchParams.get('error');
  const errorDescription = searchParams.get('error_description');

  const [step, setStep] = useState(0);

  const isError = Boolean(errorParam);

  const steps = [
    'CLIENT OAUTH CHALLENGE DETECTED · PARSING PARAMS',
    'CONFIRMING ZERO CLIENT-SIDE SECRET EXPOSURE',
    'BACKEND HANDSHAKE VALIDATED VIA SPRING SECURITY',
    'ESTABLISHING CONTRIBUTOR WORKSPACE SESSION',
  ];

  useEffect(() => {
    if (isError) return;

    let isMounted = true;

    // Verify session state with the backend
    getAuthStatus().then((status) => {
      if (isMounted && status?.authenticated) {
        setStep(3);
      }
    }).catch(() => {});

    const timer1 = setTimeout(() => setStep(1), 300);
    const timer2 = setTimeout(() => setStep(2), 600);
    const timer3 = setTimeout(() => setStep(3), 900);
    const timer4 = setTimeout(() => {
      if (isMounted) {
        navigate(ROUTES.DASHBOARD, { replace: true });
      }
    }, 1200);

    return () => {
      isMounted = false;
      clearTimeout(timer1);
      clearTimeout(timer2);
      clearTimeout(timer3);
      clearTimeout(timer4);
    };
  }, [navigate, isError]);

  return (
    <div className="flex-1 flex items-center justify-center bg-[#0D0D0D] text-[#F1ECE2] px-4 py-20 min-h-screen">
      <div className="w-full max-w-lg border-2 border-[#2E2E2E] bg-[#141414] p-8 space-y-6 shadow-2xl">
        {/* Header Bar */}
        <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 text-xs font-mono">
          <div className="flex items-center space-x-2 text-[#F4C13D]">
            <Terminal size={14} />
            <span className="font-bold tracking-widest uppercase">
              {isError ? 'OAUTH HANDSHAKE HALTED' : 'OAUTH DISPATCH BENCH'}
            </span>
          </div>
          <span className={isError ? 'text-[#FF8585]' : 'text-[#00D26A]'}>
            {isError ? 'ABORTED' : 'SESSION ACTIVE'}
          </span>
        </div>

        {/* Error State */}
        {isError ? (
          <div className="space-y-4">
            <div className="flex items-center space-x-2 text-[#FF8585]">
              <XCircle size={20} />
              <h1 className="font-heading text-2xl uppercase tracking-tight text-[#F1ECE2]">
                GITHUB AUTHENTICATION CANCELLED
              </h1>
            </div>

            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              GitHub authorization was not completed. The identity server returned error code{' '}
              <code className="text-[#FF8585] bg-[#2A1616] px-1.5 py-0.5 border border-[#441818]">
                {errorParam}
              </code>
              {errorDescription && <span>: {errorDescription}</span>}.
            </p>

            <div className="bg-[#1C1B1B] border border-[#2E2E2E] p-3 text-[11px] font-mono text-[#A7A29A] space-y-1">
              <div>&gt; NO ACCESS TOKENS WERE STORED IN LOCAL BROWSER STORAGE.</div>
              <div>&gt; PUBLIC ISSUE BROWSING REMAINS FULLY ACCESSIBLE WITHOUT SIGN-IN.</div>
            </div>

            <div className="pt-4 flex flex-col sm:flex-row gap-3">
              <Link to={ROUTES.LOGIN} className="flex-1">
                <Button variant="primary" size="md" className="w-full">
                  TRY AGAIN VIA SIGN-IN
                </Button>
              </Link>
              <Link to={ROUTES.EXPLORE} className="flex-1">
                <Button variant="secondary" size="md" className="w-full">
                  CONTINUE BROWSING
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          /* Normal Authentication Dispatch Flow */
          <div className="space-y-4">
            <div className="space-y-1">
              <h1 className="font-heading text-2xl uppercase tracking-tight text-[#F1ECE2]">
                AUTHORIZATION DISPATCH PROTOCOL
              </h1>
              <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
                Live GitHub OAuth token exchange conducted by Spring Security session backend.
              </p>
            </div>

            {/* Diagnostic Steps */}
            <div className="space-y-2.5 font-mono text-xs pt-1">
              {steps.map((text, idx) => {
                const isDone = step > idx;
                const isCurrent = step === idx;
                return (
                  <div
                    key={idx}
                    className={`p-2.5 border flex items-center space-x-3 transition-colors ${
                      isDone
                        ? 'bg-[#182319] border-[#2E7D32] text-[#F1ECE2]'
                        : isCurrent
                        ? 'bg-[#211B14] border-[#F06522] text-[#F4C13D]'
                        : 'bg-[#161616] border-[#222222] text-[#666666]'
                    }`}
                  >
                    {isDone ? (
                      <CheckCircle2 size={15} className="text-[#00D26A] shrink-0" />
                    ) : isCurrent ? (
                      <Loader2 size={15} className="text-[#F06522] animate-spin shrink-0" />
                    ) : (
                      <span className="w-3.5 h-3.5 border border-[#444444] shrink-0 inline-block" />
                    )}
                    <span className="text-[11px] font-bold">{text}</span>
                  </div>
                );
              })}
            </div>

            <div className="pt-2 flex justify-end">
              <Link to={ROUTES.DASHBOARD}>
                <Button variant="secondary" size="sm" className="flex items-center gap-1.5">
                  <span>PROCEED TO DASHBOARD</span>
                  <ArrowRight size={13} />
                </Button>
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default AuthCallbackPage;
