import React from 'react';
import { cn } from '../../utils/cn';

export const Button = React.forwardRef(
  ({ className, variant = 'primary', size = 'md', children, disabled, type = 'button', ...props }, ref) => {
    const baseStyles =
      'inline-flex items-center justify-center font-mono font-medium tracking-wider uppercase transition-colors select-none focus:outline-none focus:ring-1 focus:ring-[#F06522] disabled:opacity-40 disabled:cursor-not-allowed rounded-none';

    const variants = {
      primary:
        'bg-[#F06522] text-[#0D0D0D] font-bold hover:bg-[#F4C13D] active:bg-[#e05515] border border-[#F06522]',
      secondary:
        'bg-transparent text-[#F1ECE2] border border-[#2E2E2E] hover:border-[#F1ECE2] hover:bg-[#1A1A1A] active:bg-[#201F1F]',
      yellow:
        'bg-[#F4C13D] text-[#0D0D0D] font-bold hover:bg-[#F06522] hover:text-[#0D0D0D] border border-[#F4C13D]',
      blue:
        'bg-[#5B86F2] text-[#0D0D0D] font-bold hover:bg-[#F4C13D] border border-[#5B86F2]',
      outline:
        'border border-[#2E2E2E] bg-[#0D0D0D] text-[#F1ECE2] hover:border-[#A7A29A] hover:text-[#F1ECE2]',
      ghost:
        'bg-transparent text-[#A7A29A] hover:text-[#F1ECE2] hover:bg-[#1A1A1A]',
      danger:
        'bg-[#93000A] text-[#FFDAD6] border border-[#93000A] hover:bg-[#a8000c]',
    };

    const sizes = {
      sm: 'px-2.5 py-1 text-xs tracking-wider',
      md: 'px-4 py-2 text-xs md:text-sm tracking-wider',
      lg: 'px-6 py-3 text-sm md:text-base font-bold tracking-widest',
      icon: 'p-2 w-9 h-9',
    };

    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled}
        className={cn(baseStyles, variants[variant], sizes[size], className)}
        {...props}
      >
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';
