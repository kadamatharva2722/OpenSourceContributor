import React, { useState, useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { ROUTES } from '../../constants/routes';
import { getAuthStatus } from '../../services/authService';
import { Shield, Loader2 } from 'lucide-react';

/**
 * Route guard component that verifies real backend authentication status via /api/v1/auth/status.
 * Redirects unauthenticated guests to /login while preserving returnUrl.
 * Re-validates on navigation to prevent session back-navigation bypass after logout.
 */
export function ProtectedRoute({ children }) {
  const location = useLocation();
  const [authState, setAuthState] = useState({
    checking: true,
    authenticated: false,
  });

  useEffect(() => {
    let isMounted = true;
    setAuthState((prev) => ({ ...prev, checking: true }));

    getAuthStatus()
      .then((status) => {
        if (!isMounted) return;
        if (status && status.authenticated) {
          setAuthState({ checking: false, authenticated: true });
        } else {
          setAuthState({ checking: false, authenticated: false });
        }
      })
      .catch(() => {
        if (!isMounted) return;
        setAuthState({ checking: false, authenticated: false });
      });

    return () => {
      isMounted = false;
    };
  }, [location.pathname, location.key]);

  if (authState.checking) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center min-h-[60vh] bg-[#0D0D0D] text-[#F1ECE2] px-4">
        <div className="border border-[#2E2E2E] bg-[#141414] p-8 max-w-sm w-full text-center space-y-4">
          <div className="w-12 h-12 border border-[#F4C13D] mx-auto flex items-center justify-center text-[#F4C13D]">
            <Loader2 size={24} className="animate-spin text-[#F06522]" />
          </div>
          <div className="space-y-1 font-mono">
            <div className="flex items-center justify-center gap-1.5 text-xs text-[#F4C13D] uppercase tracking-wider font-bold">
              <Shield size={14} />
              <span>VERIFYING CONTRIBUTOR SESSION</span>
            </div>
            <p className="text-[11px] text-[#A7A29A]">
              Checking backend session authorization...
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (!authState.authenticated) {
    const returnUrl = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`${ROUTES.LOGIN}?returnUrl=${returnUrl}`} replace />;
  }

  return children ? children : <Outlet />;
}

export default ProtectedRoute;
