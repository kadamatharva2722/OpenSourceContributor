import React from 'react';
import { ArchiveX } from 'lucide-react';
import { Button } from './Button';

export function EmptyState({
  title = 'NO MATCHING ISSUES FOUND IN CRATE',
  description = 'Try broadening your search query or toggling active technology stack filters.',
  actionLabel = 'RESET FILTERS',
  onAction,
}) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center border border-[#2E2E2E] bg-[#1A1A1A]">
      <ArchiveX className="w-12 h-12 text-[#A7A29A] mb-4 stroke-1" />
      <h3 className="font-heading text-xl text-[#F1ECE2] mb-2">{title}</h3>
      <p className="font-mono text-xs text-[#A7A29A] max-w-md mb-6">{description}</p>
      {onAction && actionLabel && (
        <Button variant="outline" size="sm" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}
