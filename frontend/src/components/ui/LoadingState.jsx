import React from 'react';

export function LoadingState({ message = 'SEARCHING GITHUB ISSUES...' }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4 text-center">
      <div className="w-12 h-12 border-2 border-[#2E2E2E] border-t-[#F06522] animate-spin mb-4" />
      <p className="font-mono text-xs uppercase tracking-widest text-[#F1ECE2]">{message}</p>
    </div>
  );
}
