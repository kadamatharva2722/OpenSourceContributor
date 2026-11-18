import { useEffect, useRef } from 'react';

const FOCUSABLE_SELECTORS = [
  'a[href]',
  'button:not([disabled])',
  'textarea:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

/**
 * Hook to trap keyboard focus within a modal dialog and restore focus upon closing.
 *
 * @param {boolean} isOpen Whether the modal dialog is currently open
 * @param {Function} [onClose] Optional callback triggered on Escape key press
 * @returns {React.RefObject} Ref to attach to the dialog's root/container element
 */
export function useFocusTrap(isOpen, onClose) {
  const containerRef = useRef(null);
  const previousActiveElementRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;

    // Store the element that triggered the modal opening
    previousActiveElementRef.current = document.activeElement;

    const container = containerRef.current;
    if (!container) return;

    // Focus the first focusable element inside the modal
    const focusFirst = () => {
      const focusable = container.querySelectorAll(FOCUSABLE_SELECTORS);
      if (focusable.length > 0) {
        focusable[0].focus();
      } else {
        container.setAttribute('tabindex', '-1');
        container.focus();
      }
    };

    // Slight timeout ensures modal DOM is rendered before focusing
    const timer = setTimeout(focusFirst, 50);

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && typeof onClose === 'function') {
        e.stopPropagation();
        onClose();
        return;
      }

      if (e.key !== 'Tab') return;

      const focusables = Array.from(container.querySelectorAll(FOCUSABLE_SELECTORS));
      if (focusables.length === 0) {
        e.preventDefault();
        return;
      }

      const firstElement = focusables[0];
      const lastElement = focusables[focusables.length - 1];

      if (e.shiftKey) {
        if (document.activeElement === firstElement || !container.contains(document.activeElement)) {
          e.preventDefault();
          lastElement.focus();
        }
      } else {
        if (document.activeElement === lastElement || !container.contains(document.activeElement)) {
          e.preventDefault();
          firstElement.focus();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      clearTimeout(timer);
      document.removeEventListener('keydown', handleKeyDown);

      // Restore focus to triggering element on close
      if (previousActiveElementRef.current && typeof previousActiveElementRef.current.focus === 'function') {
        previousActiveElementRef.current.focus();
      }
    };
  }, [isOpen, onClose]);

  return containerRef;
}

export default useFocusTrap;
