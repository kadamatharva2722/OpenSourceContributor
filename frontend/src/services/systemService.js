import { apiClient } from './apiClient.js';

/**
 * OpenSourceContributor - System Service
 * Manages visitor-triggered backend wake-up for sleeping cloud instances (e.g. Render Free Web Services).
 *
 * Invariants:
 * 1. Single execution per browser session (backed by sessionStorage).
 * 2. In-memory deduplication across React StrictMode double-mounts.
 * 3. Zero periodic polling or background keep-alive timers.
 * 4. Asynchronous and non-blocking to frontend rendering.
 * 5. Graceful error handling without retry loops.
 */

const SESSION_STORAGE_KEY = 'osc_backend_woken_session';

let wakePromise = null;
let wakeStatus = 'idle'; // 'idle' | 'waking' | 'ready' | 'error'
const listeners = new Set();

function notifyListeners() {
  listeners.forEach((listener) => {
    try {
      listener(wakeStatus);
    } catch (e) {
      // Ignore listener error
    }
  });
}

/**
 * Checks whether the backend wake-up has already occurred in the current browser session.
 * @returns {boolean}
 */
export function isBackendWokenInSession() {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return false;
  }
  try {
    return window.sessionStorage.getItem(SESSION_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

/**
 * Persists the woken state to sessionStorage for the remainder of this browser session.
 */
function markBackendWokenInSession() {
  if (typeof window === 'undefined' || !window.sessionStorage) {
    return;
  }
  try {
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, 'true');
  } catch {
    // Quota or storage disabled fallback
  }
}

/**
 * Triggers a lightweight visitor wake-up request to the backend.
 * Guaranteed to execute at most once per browser session.
 *
 * @returns {Promise<'ready' | 'error'>}
 */
export async function wakeBackend() {
  // If already confirmed awake in this browser session, return immediately
  if (isBackendWokenInSession()) {
    wakeStatus = 'ready';
    notifyListeners();
    return 'ready';
  }

  // Deduplicate concurrent or StrictMode invocations
  if (wakePromise) {
    return wakePromise;
  }

  wakeStatus = 'waking';
  notifyListeners();

  wakePromise = (async () => {
    try {
      await apiClient('/api/v1/system/wake', { silent: true });
      wakeStatus = 'ready';
      markBackendWokenInSession();
      notifyListeners();
      return 'ready';
    } catch {
      wakeStatus = 'error';
      notifyListeners();
      return 'error';
    } finally {
      // Clear in-memory promise once settled so subsequent calls/retries can initiate
      // a fresh request if needed, while successful state is guarded by sessionStorage.
      wakePromise = null;
    }
  })();

  return wakePromise;
}

/**
 * Returns current wake status.
 * @returns {'idle' | 'waking' | 'ready' | 'error'}
 */
export function getWakeStatus() {
  if (wakeStatus === 'idle' && isBackendWokenInSession()) {
    return 'ready';
  }
  return wakeStatus;
}

/**
 * Subscribes to wake status changes.
 * @param {Function} callback (status) => void
 * @returns {Function} unsubscribe
 */
export function subscribeWakeStatus(callback) {
  listeners.add(callback);
  callback(getWakeStatus());
  return () => {
    listeners.delete(callback);
  };
}

/**
 * For testing and diagnostic verification.
 */
export function _resetWakeStateForTesting() {
  wakePromise = null;
  wakeStatus = 'idle';
  listeners.clear();
  if (typeof window !== 'undefined' && window.sessionStorage) {
    try {
      window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
    } catch {}
  }
}
