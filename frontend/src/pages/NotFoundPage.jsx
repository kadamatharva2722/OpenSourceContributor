import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { Button } from '../components/ui/Button';
import { AlertTriangle, ArrowLeft, Compass } from 'lucide-react';

export function NotFoundPage() {
  useDocumentTitle('Page Not Found');
  return (
    <div className="flex-1 flex items-center justify-center bg-[#0D0D0D] text-[#F1ECE2] px-4 py-16 md:py-24">
      <div className="w-full max-w-lg border-2 border-[#2E2E2E] bg-[#141414] p-6 sm:p-8 text-center space-y-6">
        <div className="flex items-center justify-between border-b border-[#2E2E2E] pb-3 text-xs font-mono">
          <div className="flex items-center space-x-2 text-[#FF8585]">
            <AlertTriangle size={16} />
            <span className="font-bold tracking-widest uppercase">ERROR 404</span>
          </div>
          <span className="text-[#666666]">HTTP 404 // NOT FOUND</span>
        </div>

        <div className="space-y-3">
          <h1 className="font-heading text-6xl sm:text-7xl md:text-8xl text-[#F06522] tracking-tight leading-none">
            404
          </h1>
          <h2 className="font-heading text-xl sm:text-2xl uppercase tracking-tight text-[#F1ECE2]">
            PAGE NOT FOUND
          </h2>
          <p className="font-mono text-xs sm:text-sm text-[#A7A29A] leading-relaxed max-w-md mx-auto">
            The requested page pathway or resource does not exist, has been removed, or the link may be broken.
          </p>
        </div>

        <div className="bg-[#1C1B1B] border border-[#2E2E2E] p-3 text-left font-mono text-[11px] text-[#A7A29A] space-y-1">
          <div>&gt; STATUS: 404 RESOURCE_NOT_FOUND</div>
          <div>&gt; ACTION: RE-ROUTE TO VERIFIED INDEX ROOT</div>
        </div>

        <div className="pt-2 flex flex-col sm:flex-row gap-3">
          <Link to={ROUTES.HOME} className="flex-1">
            <Button variant="primary" size="md" className="w-full">
              <ArrowLeft size={14} className="mr-1.5" />
              BACK TO HOMEPAGE
            </Button>
          </Link>
          <Link to={ROUTES.EXPLORE} className="flex-1">
            <Button variant="secondary" size="md" className="w-full">
              <Compass size={14} className="mr-1.5" />
              EXPLORE ISSUES
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default NotFoundPage;
