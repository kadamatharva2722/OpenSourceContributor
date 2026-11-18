import React from 'react';
import { cn } from '../../utils/cn';

export function Badge({ children, variant = 'surface', size = 'sm', className, ...props }) {
  const baseStyles =
    'inline-flex items-center font-mono font-medium uppercase tracking-wider select-none rounded-none border';

  const variants = {
    orange: 'bg-[#F06522] text-[#0D0D0D] border-[#F06522] font-bold',
    yellow: 'bg-[#F4C13D] text-[#0D0D0D] border-[#F4C13D] font-bold',
    blue: 'bg-[#5B86F2] text-[#0D0D0D] border-[#5B86F2] font-bold',
    pink: 'bg-[#F29BB8] text-[#0D0D0D] border-[#F29BB8] font-bold',
    surface: 'bg-[#201F1F] text-[#F1ECE2] border-[#2E2E2E]',
    outline: 'bg-transparent text-[#A7A29A] border-[#2E2E2E]',
    dark: 'bg-[#0D0D0D] text-[#F1ECE2] border-[#2E2E2E]',
    success: 'bg-[#2E7D32] text-[#E8F5E9] border-[#2E7D32]',
  };

  const sizes = {
    xs: 'px-1.5 py-0.5 text-[10px] leading-tight',
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-xs font-semibold',
  };

  return (
    <span className={cn(baseStyles, variants[variant], sizes[size], className)} {...props}>
      {children}
    </span>
  );
}
