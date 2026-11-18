import React from 'react';
import {
  ExternalLink,
  Bookmark,
  MessageSquare,
  Sparkles,
  GitPullRequest,
  CheckCircle2,
  Calendar,
  User,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';

/**
 * Calculates readable text color based on background hex code.
 * Uses standard YIQ formula for optical luminance.
 */
function getLabelContrastColor(hexColor) {
  if (!hexColor) return '#FFFFFF';
  const cleanHex = hexColor.replace('#', '');
  if (cleanHex.length !== 6) return '#FFFFFF';
  const r = parseInt(cleanHex.substring(0, 2), 16);
  const g = parseInt(cleanHex.substring(2, 4), 16);
  const b = parseInt(cleanHex.substring(4, 6), 16);
  const yiq = (r * 299 + g * 587 + b * 114) / 1000;
  return yiq >= 135 ? '#0D0D0D' : '#FFFFFF';
}

/**
 * Converts ISO date string to human-friendly relative time.
 */
function formatRelativeTime(dateString) {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    if (diffMs < 0) return 'just now';
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffDays > 30) {
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
    if (diffDays > 0) return `${diffDays}d ago`;
    if (diffHours > 0) return `${diffHours}h ago`;
    if (diffMins > 0) return `${diffMins}m ago`;
    return 'just now';
  } catch {
    return dateString;
  }
}

/**
 * Strips complex markdown formatting and truncates for card snippet.
 */
function getBodySnippet(body, maxLength = 175) {
  if (!body) return 'No description provided by issue author.';
  const cleaned = body
    .replace(/```[\s\S]*?```/g, ' [code] ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/#+\s/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/[*_~>]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  if (cleaned.length <= maxLength) return cleaned;
  return cleaned.slice(0, maxLength).trim() + '...';
}

/**
 * Reusable Issue Card component displaying verified GitHub issue metadata,
 * match scores, transparent factual explanations, and action triggers.
 */
export function IssueCard({
  issueMatch,
  isSelected = false,
  isSaved = false,
  onSelect,
  onToggleSave,
}) {
  if (!issueMatch || !issueMatch.issue) {
    return null;
  }

  const { issue, matchExplanation, matchedCriteria = [], matchScore } = issueMatch;
  const labels = issue.labels || [];
  const isOpen = (issue.state || 'open').toLowerCase() === 'open';

  // Check if issue has beginner labels to show factual guidance
  const hasBeginnerLabel = labels.some((l) => {
    const name = (l.name || '').toLowerCase();
    return (
      name.includes('good first issue') ||
      name.includes('first-timers-only') ||
      name.includes('beginner') ||
      name.includes('easy') ||
      name.includes('starter')
    );
  });

  const formattedScore =
    matchScore !== null && matchScore !== undefined
      ? `${Math.round(matchScore)}% MATCH`
      : 'RELEVANT';

  return (
    <article
      onClick={() => onSelect && onSelect(issueMatch)}
      className={`relative border transition-all duration-150 flex flex-col justify-between cursor-pointer group ${
        isSelected
          ? 'bg-[#181818] border-[#F06522] shadow-[0_0_15px_rgba(240,101,34,0.15)]'
          : 'bg-[#141414] border-[#2E2E2E] hover:border-[#A7A29A] hover:bg-[#1A1A1A]'
      }`}
    >
      {/* Top Identity & Status Ribbon */}
      <div className="p-4 md:p-5 pb-3 space-y-3">
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center space-x-2 font-mono text-xs">
            <span className="text-[#F4C13D] font-bold group-hover:text-[#F06522] transition-colors truncate max-w-[240px]">
              {issue.repositoryFullName}
            </span>
            <span className="text-[#666666]">#</span>
            <span className="text-[#A7A29A] font-bold">{issue.number}</span>
          </div>

          <div className="flex items-center space-x-2 shrink-0">
            {/* Match Score Badge (AI Mode) or Factual Match Badge (Guest Mode) */}
            {matchScore !== null && matchScore !== undefined ? (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-[#1F1F1F] text-[#F4C13D] border border-[#F4C13D] font-mono text-[10px] font-bold uppercase tracking-wider">
                <Sparkles size={11} className="text-[#F4C13D]" />
                <span>{Math.round(matchScore)}% MATCH</span>
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-[#1C1B1B] text-[#5B86F2] border border-[#2E2E2E] font-mono text-[10px] font-bold uppercase tracking-wider">
                <CheckCircle2 size={11} className="text-[#5B86F2]" />
                <span>FACTUAL MATCH</span>
              </span>
            )}

            {/* Issue State Badge */}
            <span
              className={`inline-flex items-center px-2 py-0.5 font-mono text-[10px] font-bold uppercase tracking-wider border ${
                isOpen
                  ? 'bg-[#19221A] text-[#4ADE80] border-[#2E7D32]'
                  : 'bg-[#251A1A] text-[#FF8585] border-[#93000A]'
              }`}
            >
              {isOpen ? 'OPEN' : 'CLOSED'}
            </span>
          </div>
        </div>

        {/* Issue Title */}
        <h3 className="font-heading text-lg md:text-xl uppercase text-[#F1ECE2] leading-snug group-hover:text-[#F4C13D] transition-colors break-words">
          {issue.title}
        </h3>

        {/* Body Snippet */}
        <p className="font-mono text-xs text-[#A7A29A] leading-relaxed line-clamp-2">
          {getBodySnippet(issue.body)}
        </p>

        {/* GitHub Labels Row */}
        {labels.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-1">
            {labels.slice(0, 5).map((label) => {
              const bg = label.color ? `#${label.color.replace('#', '')}` : '#2E2E2E';
              const text = getLabelContrastColor(label.color);
              return (
                <span
                  key={label.id || label.name}
                  style={{ backgroundColor: bg, color: text }}
                  title={label.description || label.name}
                  className="px-2 py-0.5 font-mono text-[10px] font-bold uppercase tracking-wide truncate max-w-[180px] border border-black/20"
                >
                  {label.name}
                </span>
              );
            })}
            {labels.length > 5 && (
              <span className="px-1.5 py-0.5 bg-[#1E1E1E] text-[#A7A29A] font-mono text-[10px] border border-[#2E2E2E]">
                +{labels.length - 5} MORE
              </span>
            )}
          </div>
        )}
      </div>

      {/* Transparent Match Explanation & Criteria Section */}
      <div className="mx-4 md:mx-5 my-2 p-3 bg-[#0E0E0E] border border-[#262626] space-y-2 font-mono text-xs">
        <div className="flex items-center justify-between text-[11px] text-[#F4C13D] font-bold uppercase tracking-wider">
          <span className="flex items-center gap-1.5">
            <ShieldCheck size={13} className="text-[#5B86F2]" />
            <span>
              {matchScore !== null && matchScore !== undefined
                ? 'AI MATCH EXPLANATION'
                : 'FACTUAL MATCH CRITERIA'}
            </span>
          </span>
          <span className="text-[#666666] text-[10px]">
            {matchScore !== null && matchScore !== undefined
              ? 'AI INFERRED & VERIFIED'
              : 'VERIFIED GITHUB DATA'}
          </span>
        </div>

        <p className="text-[#F1ECE2] text-[11px] leading-relaxed opacity-95">
          {matchExplanation || 'Matched your search query parameters.'}
        </p>

        {/* Matched Criteria Pills */}
        {matchedCriteria.length > 0 && (
          <div className="flex flex-wrap gap-1 pt-1">
            {matchedCriteria.map((criterion, idx) => (
              <span
                key={idx}
                className="bg-[#1C1B1B] text-[#5B86F2] border border-[#2E2E2E] px-1.5 py-0.5 text-[10px] uppercase font-bold"
              >
                ✓ {criterion}
              </span>
            ))}
          </div>
        )}

        {/* Beginner Label Neutral Disclaimer */}
        {hasBeginnerLabel && (
          <div className="flex items-start gap-1.5 pt-1 text-[10px] text-[#A7A29A] border-t border-[#1F1F1F]">
            <AlertCircle size={11} className="shrink-0 mt-0.5 text-[#F4C13D]" />
            <span className="leading-tight">
              Beginner-friendly label reflects maintainer categorization; it does not guarantee task simplicity.
            </span>
          </div>
        )}
      </div>

      {/* Card Footer: Metadata Ledger & Actions */}
      <div className="p-4 md:p-5 pt-2 border-t border-[#202020] bg-[#111111] flex flex-wrap items-center justify-between gap-3 font-mono text-xs">
        {/* Author & Activity Telemetry */}
        <div className="flex items-center space-x-3 text-[#A7A29A] text-[11px]">
          {issue.authorAvatarUrl ? (
            <img
              src={issue.authorAvatarUrl}
              alt={issue.authorUsername ? `${issue.authorUsername}'s GitHub profile picture` : 'Issue author avatar'}
              loading="lazy"
              decoding="async"
              width="16"
              height="16"
              className="w-4 h-4 aspect-square rounded-full border border-[#2E2E2E] inline-block"
              onError={(e) => {
                e.currentTarget.style.display = 'none';
              }}
            />
          ) : (
            <User size={13} className="text-[#A7A29A]" />
          )}
          <span className="text-[#F1ECE2] font-semibold">{issue.authorUsername || 'maintainer'}</span>
          <span>•</span>
          <span title={issue.updatedAt || issue.createdAt}>
            {formatRelativeTime(issue.updatedAt || issue.createdAt)}
          </span>
          <span>•</span>
          <span className="flex items-center gap-1 text-[#A7A29A]">
            <MessageSquare size={12} />
            <span>{issue.commentsCount || 0}</span>
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-2 ml-auto" onClick={(e) => e.stopPropagation()}>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              onToggleSave && onToggleSave(issue);
            }}
            title={isSaved ? 'Remove from Saved Crate' : 'Save to Crate'}
            aria-label={isSaved ? `Remove issue #${issue.number} from Saved Crate` : `Save issue #${issue.number} to Crate`}
            className={`min-h-[40px] min-w-[40px] p-2 border flex items-center justify-center transition-colors ${
              isSaved
                ? 'bg-[#F06522] text-[#0D0D0D] border-[#F06522]'
                : 'bg-[#1C1B1B] text-[#A7A29A] border-[#2E2E2E] hover:text-[#F1ECE2] hover:border-[#A7A29A]'
            }`}
          >
            <Bookmark size={15} fill={isSaved ? '#0D0D0D' : 'none'} />
          </button>

          <a
            href={issue.htmlUrl}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`Open issue #${issue.number} on GitHub`}
            className="min-h-[40px] inline-flex items-center gap-1.5 px-3 py-2 bg-[#1C1B1B] text-[#F1ECE2] hover:bg-[#F4C13D] hover:text-[#0D0D0D] border border-[#2E2E2E] hover:border-[#F4C13D] font-mono text-[11px] uppercase font-bold transition-colors"
          >
            <span>GITHUB</span>
            <ExternalLink size={12} />
          </a>
        </div>
      </div>
    </article>
  );
}

export default IssueCard;
