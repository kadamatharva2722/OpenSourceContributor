import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../../constants/routes';
import { FOOTER_COLUMNS } from '../../constants/navigation';
import { Terminal, Disc, Shield, ExternalLink } from 'lucide-react';

export function AppFooter() {
  return (
    <footer className="w-full bg-[#0D0D0D] border-t-2 border-[#2E2E2E] mt-auto">
      {/* 3-Column Archival Catalog Ledger */}
      <div className="max-w-7xl mx-auto px-4 md:px-8 py-12 md:py-16">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 md:gap-12">
          {/* Brand Manifesto & System Metadata */}
          <div className="md:col-span-1 space-y-4">
            <Link
              to={ROUTES.HOME}
              className="flex items-center space-x-2 group w-fit"
              aria-label="OpenSourceContributor Homepage"
            >
              <Disc className="w-5 h-5 text-[#F06522] animate-spin group-hover:text-[#F4C13D] transition-colors" style={{ animationDuration: '8s' }} />
              <span className="font-heading text-xl text-[#F1ECE2] tracking-tight group-hover:text-[#F06522] transition-colors">
                OPENSOURCE<span className="text-[#F06522] group-hover:text-[#F4C13D]">CONTRIBUTOR</span>
              </span>
            </Link>
            <p className="font-mono text-xs text-[#A7A29A] leading-relaxed">
              Curated discovery for GitHub open source issues.
              Connecting contributors with high-signal, verified tasks across modern ecosystems.
            </p>
          </div>

          {/* Navigation Columns */}
          {FOOTER_COLUMNS.map((column, idx) => (
            <div key={idx} className="space-y-4">
              <h4 className="font-mono text-xs font-bold tracking-widest text-[#F1ECE2] border-b border-[#2E2E2E] pb-2">
                {column.title}
              </h4>
              <ul className="space-y-2">
                {column.links.map((link, linkIdx) => (
                  <li key={linkIdx}>
                    <Link
                      to={link.href}
                      className="font-mono text-xs text-[#A7A29A] hover:text-[#F06522] hover:underline transition-colors block"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Footer Bottom Strip */}
        <div className="mt-12 pt-6 border-t border-[#2E2E2E] flex flex-col md:flex-row items-start md:items-center justify-between text-xs font-mono text-[#666666] space-y-3 md:space-y-0">
          <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
            <span>© {new Date().getFullYear()} OPENSOURCECONTRIBUTOR</span>
            <span>·</span>
            <span className="text-[#A7A29A]">SOURCE: GITHUB REST API</span>
            <span>·</span>
            <span>ZERO SYNTHETIC ISSUES</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
