import React from 'react';
import { cn } from '../../utils/cn';

export const Input = React.forwardRef(
  ({ className, type = 'text', label, error, helperText, prefixIcon: PrefixIcon, ...props }, ref) => {
    return (
      <div className="w-full flex flex-col space-y-1">
        {label && (
          <label className="text-xs font-mono uppercase tracking-widest text-[#A7A29A]">
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {PrefixIcon && (
            <div className="absolute left-3 text-[#A7A29A] pointer-events-none">
              <PrefixIcon size={16} />
            </div>
          )}
          <input
            ref={ref}
            type={type}
            className={cn(
              'w-full bg-[#1A1A1A] border border-[#2E2E2E] text-[#F1ECE2] font-mono text-sm px-3 py-2.5 placeholder:text-[#666666] focus:outline-none focus:border-[#F06522] transition-colors rounded-none',
              PrefixIcon && 'pl-9',
              error && 'border-[#93000A] focus:border-[#93000A]',
              className
            )}
            {...props}
          />
        </div>
        {error ? (
          <p className="text-xs font-mono text-[#FF8585]">{error}</p>
        ) : helperText ? (
          <p className="text-xs font-mono text-[#A7A29A]">{helperText}</p>
        ) : null}
      </div>
    );
  }
);

Input.displayName = 'Input';
