import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ROUTES } from '../../constants/routes';
import { HEADER_NAV_LINKS } from '../../constants/navigation';
import { Button } from '../ui/Button';
import { useFocusTrap } from '../../hooks/useFocusTrap';
import { useBackendWake } from '../../hooks/useBackendWake';
import { getAuthStatus, logoutUser } from '../../services/authService';
import { Menu, X, Github, Bookmark, LayoutDashboard, LogOut } from 'lucide-react';

export function AppHeader() {
  const location = useLocation();
  const { isWaking } = useBackendWake();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    let isMounted = true;
    getAuthStatus()
      .then((status) => {
        if (isMounted) {
          setCurrentUser(status && status.authenticated ? status : null);
        }
      })
      .catch(() => {
        if (isMounted) setCurrentUser(null);
      });

    return () => {
      isMounted = false;
    };
  }, [location.pathname]);

  // Close mobile drawer on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  // Focus trap and keyboard accessibility for mobile drawer
  const drawerRef = useFocusTrap(mobileMenuOpen, () => setMobileMenuOpen(false));

  const handleLogout = async () => {
    try {
      await logoutUser();
      setCurrentUser(null);
      window.location.href = ROUTES.HOME;
    } catch (err) {
      console.warn('Logout failed:', err);
    }
  };

  const isActive = (path) => {
    if (path === '/') return location.pathname === '/';
    if (path === ROUTES.SAVED_ISSUES) {
      return (
        location.pathname === ROUTES.SAVED_ISSUES ||
        location.pathname === ROUTES.CRATE ||
        location.pathname === '/saved'
      );
    }
    return location.pathname.startsWith(path);
  };

  return (
    <header className="w-full bg-[#0D0D0D] border-b border-[#2E2E2E] sticky top-0 z-50">
      {/* Top Banner / Ticker Strip */}
      <div className="bg-[#F4C13D] text-[#0D0D0D] px-4 py-1 flex items-center justify-between font-mono text-[11px] font-bold tracking-widest border-b border-[#0D0D0D] uppercase overflow-hidden whitespace-nowrap">
        <div className="flex items-center space-x-3">
          <span className="truncate">REAL GITHUB ISSUES · CURATED DIFFICULTY · ZERO SYNTHETIC DATA</span>
        </div>
        <div className="hidden md:flex items-center space-x-4 text-[10px]">
          {isWaking ? (
            <span className="flex items-center space-x-1.5 font-bold text-[#0D0D0D]">
              <span className="w-1.5 h-1.5 bg-[#F06522] animate-ping inline-block rounded-full" />
              <span>CONNECTING TO BACKEND...</span>
            </span>
          ) : (
            <span>OPEN SOURCE DISCOVERY</span>
          )}
        </div>
      </div>

      {/* Main Navigation Bar */}
      <div className="max-w-7xl mx-auto px-4 md:px-8 py-3.5 flex items-center justify-between">
        {/* Brand Identity */}
        <Link
          to={ROUTES.HOME}
          className="flex items-center group shrink-0"
          aria-label="OpenSourceContributor Homepage"
        >
          <span className="font-heading text-2xl md:text-3xl tracking-tight text-[#F1ECE2] group-hover:text-[#F06522] transition-colors">
            OPENSOURCE<span className="text-[#F06522] group-hover:text-[#F4C13D]">CONTRIBUTOR</span>
          </span>
        </Link>

        {/* Desktop Navigation Links */}
        <nav className="hidden lg:flex items-center space-x-1">
          {HEADER_NAV_LINKS.map((link) => {
            const active = isActive(link.href);
            return (
              <Link
                key={link.href}
                to={link.href}
                className={`px-3 py-1.5 font-mono text-xs uppercase tracking-wider transition-colors ${
                  active
                    ? 'text-[#0D0D0D] bg-[#F1ECE2] font-bold'
                    : 'text-[#A7A29A] hover:text-[#F1ECE2] hover:bg-[#1A1A1A]'
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* Header Right Actions */}
        <div className="hidden sm:flex items-center space-x-2 md:space-x-3">
          <Link
            to={ROUTES.SAVED_ISSUES}
            className={`flex items-center space-x-1.5 px-2.5 py-1.5 font-mono text-xs uppercase tracking-wider transition-colors ${
              isActive(ROUTES.SAVED_ISSUES)
                ? 'text-[#0D0D0D] bg-[#F1ECE2] font-bold'
                : 'text-[#A7A29A] hover:text-[#F1ECE2] hover:bg-[#1A1A1A]'
            }`}
            title="Saved Issues Crate"
          >
            <Bookmark size={14} />
            <span className="hidden md:inline">CRATE</span>
          </Link>

          <Link
            to={ROUTES.DASHBOARD}
            className={`flex items-center space-x-1.5 px-2.5 py-1.5 font-mono text-xs uppercase tracking-wider transition-colors ${
              isActive(ROUTES.DASHBOARD)
                ? 'text-[#0D0D0D] bg-[#F1ECE2] font-bold'
                : 'text-[#A7A29A] hover:text-[#F1ECE2] hover:bg-[#1A1A1A]'
            }`}
            title="Contributor Dashboard"
          >
            <LayoutDashboard size={14} />
            <span className="hidden md:inline">DASHBOARD</span>
          </Link>

          {currentUser ? (
            <div className="flex items-center space-x-2 border border-[#2E2E2E] bg-[#141414] px-2.5 py-1">
              <span className="w-1.5 h-1.5 bg-[#00D26A] inline-block shrink-0" />
              <span className="font-mono text-xs text-[#F1ECE2] font-bold truncate max-w-[160px]">
                @{currentUser.username}
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="text-[#A7A29A] hover:text-[#FF8585] transition-colors ml-1 p-1.5 min-h-[36px] min-w-[36px] flex items-center justify-center"
                title="Sign out"
                aria-label="Sign out of account"
              >
                <LogOut size={14} />
              </button>
            </div>
          ) : (
            <Link to={ROUTES.LOGIN}>
              <Button variant="secondary" size="sm" className="inline-flex min-h-[40px]">
                <Github size={14} className="mr-1.5" />
                SIGN IN
              </Button>
            </Link>
          )}
        </div>

        {/* Mobile Menu Button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="lg:hidden p-2 text-[#F1ECE2] hover:bg-[#1A1A1A] border border-[#2E2E2E] min-h-[44px] min-w-[44px] flex items-center justify-center"
          aria-label="Toggle navigation menu"
          aria-expanded={mobileMenuOpen}
          aria-controls="mobile-navigation-drawer"
        >
          {mobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* Mobile Navigation Drawer */}
      {mobileMenuOpen && (
        <div
          ref={drawerRef}
          id="mobile-navigation-drawer"
          role="dialog"
          aria-modal="true"
          aria-label="Mobile Navigation Menu"
          className="lg:hidden border-t border-[#2E2E2E] bg-[#0D0D0D] px-4 py-4 space-y-3"
        >
          <nav className="flex flex-col space-y-1">
            {HEADER_NAV_LINKS.map((link) => {
              const active = isActive(link.href);
              return (
                <Link
                  key={link.href}
                  to={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className={`px-3 py-2 font-mono text-sm uppercase tracking-wider transition-colors ${
                    active
                      ? 'text-[#0D0D0D] bg-[#F1ECE2] font-bold'
                      : 'text-[#F1ECE2] hover:bg-[#1A1A1A]'
                  }`}
                >
                  {link.label}
                </Link>
              );
            })}
          </nav>
          <div className="pt-3 border-t border-[#2E2E2E] flex flex-col space-y-2">
            <Link to={ROUTES.DASHBOARD} onClick={() => setMobileMenuOpen(false)}>
              <Button
                variant={isActive(ROUTES.DASHBOARD) ? 'primary' : 'secondary'}
                size="md"
                className="w-full justify-start"
              >
                <LayoutDashboard size={16} className="mr-2" />
                CONTRIBUTOR DASHBOARD
              </Button>
            </Link>
            <Link to={ROUTES.SAVED_ISSUES} onClick={() => setMobileMenuOpen(false)}>
              <Button
                variant={isActive(ROUTES.SAVED_ISSUES) ? 'primary' : 'secondary'}
                size="md"
                className="w-full justify-start"
              >
                <Bookmark size={16} className="mr-2" />
                SAVED ISSUES CRATE
              </Button>
            </Link>
            {currentUser ? (
              <div className="flex items-center justify-between border border-[#2E2E2E] bg-[#141414] px-3 py-2 font-mono text-xs">
                <div className="flex items-center space-x-2">
                  <span className="w-2 h-2 bg-[#00D26A] inline-block shrink-0" />
                  <span className="text-[#F1ECE2] font-bold truncate max-w-[200px]">
                    @{currentUser.username}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setMobileMenuOpen(false);
                    handleLogout();
                  }}
                  className="text-[#A7A29A] hover:text-[#FF8585] transition-colors flex items-center space-x-1"
                >
                  <LogOut size={14} />
                  <span>SIGN OUT</span>
                </button>
              </div>
            ) : (
              <Link to={ROUTES.LOGIN} onClick={() => setMobileMenuOpen(false)}>
                <Button variant="secondary" size="md" className="w-full justify-start">
                  <Github size={16} className="mr-2" />
                  SIGN IN WITH GITHUB
                </Button>
              </Link>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
