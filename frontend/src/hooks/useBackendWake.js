import { useState, useEffect } from 'react';
import { wakeBackend, getWakeStatus, subscribeWakeStatus } from '../services/systemService';

/**
 * React hook to trigger backend wake-up on application mount and observe connection status.
 *
 * Guarantees:
 * - Executes immediately when the root application mounts.
 * - Executes only once per browser session.
 * - Fully deduplicated across React StrictMode double-mounting.
 * - Non-blocking to initial page render.
 * - Zero periodic polling or keep-alive cycles.
 *
 * @returns {{
 *   status: 'idle' | 'waking' | 'ready' | 'error',
 *   isWaking: boolean,
 *   isReady: boolean,
 *   isError: boolean
 * }}
 */
export function useBackendWake() {
  const [status, setStatus] = useState(() => getWakeStatus());

  useEffect(() => {
    const unsubscribe = subscribeWakeStatus((newStatus) => {
      setStatus(newStatus);
    });

    // Fire wake request (safe, asynchronous, non-blocking)
    wakeBackend();

    return () => {
      unsubscribe();
    };
  }, []);

  return {
    status,
    isWaking: status === 'waking',
    isReady: status === 'ready',
    isError: status === 'error',
    retry: wakeBackend,
  };
}
