import React from 'react';
import { cn } from '../../utils/cn';

export function Card({ children, className, borderAccent, ...props }) {
  const accents = {
    orange: 'border-l-4 border-l-[#F06522]',
    yellow: 'border-l-4 border-l-[#F4C13D]',
    blue: 'border-l-4 border-l-[#5B86F2]',
    pink: 'border-l-4 border-l-[#F29BB8]',
  };

  return (
    <div
      className={cn(
        'bg-[#1A1A1A] border border-[#2E2E2E] rounded-none p-4 md:p-6 text-[#F1ECE2]',
        borderAccent && accents[borderAccent],
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}
