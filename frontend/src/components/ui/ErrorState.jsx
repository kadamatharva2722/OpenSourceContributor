import React from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from './Button';

export function ErrorState({
  title = 'INDEX RETRIEVAL FAULT',
  message = 'Failed to load records from the registry ledger.',
  onRetry,
}) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-6 text-center border border-[#93000A] bg-[#201F1F]">
      <AlertTriangle className="w-10 h-10 text-[#FF8585] mb-3" />
      <h3 className="font-heading text-lg text-[#FF8585] mb-1">{title}</h3>
      <p className="font-mono text-xs text-[#A7A29A] max-w-md mb-5">{message}</p>
      {onRetry && (
        <Button variant="danger" size="sm" onClick={onRetry}>
          RE-TRANSMIT REQUEST
        </Button>
      )}
    </div>
  );
}
