/**
 * OpenSourceContributor - API Client
 * Configured for Spring Boot backend session authentication.
 * Uses credentials: 'include' for secure HttpOnly cookie persistence
 * and automatically sends CSRF tokens on state-changing requests.
 */

const BASE_URL =
  import.meta.env.VITE_API_URL ||
  import.meta.env.VITE_BACKEND_URL ||
  (import.meta.env.PROD
    ? 'https://opensourcecontributor-backend.onrender.com'
    : '');

export function getBaseUrl() {
  return BASE_URL;
}

let memoryCsrfToken = null;
let csrfFetchPromise = null;

export function clearCsrfToken() {
  memoryCsrfToken = null;
  csrfFetchPromise = null;
}

/**
 * Proactively fetches active CSRF token from the backend /api/v1/auth/csrf endpoint.
 * Works seamlessly in both same-origin (proxied) and cross-origin (direct) configurations
 * by storing the JSON token in memory when document.cookie is partitioned or cross-origin.
 */
export async function fetchCsrfToken(force = false) {
  if (memoryCsrfToken && !force) {
    return memoryCsrfToken;
  }
  if (csrfFetchPromise && !force) {
    return csrfFetchPromise;
  }

  csrfFetchPromise = (async () => {
    try {
      const response = await fetch(`${BASE_URL}/api/v1/auth/csrf`, {
        method: 'GET',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
        },
      });
      if (response.ok && response.status !== 204) {
        const data = await response.json();
        if (data && data.token) {
          memoryCsrfToken = data.token;
          return memoryCsrfToken;
        }
      }
    } catch (err) {
      // Proactive CSRF token fetch fallback
    } finally {
      csrfFetchPromise = null;
    }
    return memoryCsrfToken;
  })();

  return csrfFetchPromise;
}

export function getCookie(name) {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match ? decodeURIComponent(match[3]) : null;
}

export async function apiClient(endpoint, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  // Attach CSRF token on mutating requests if present in memory or cookies
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    let xsrfToken = memoryCsrfToken;
    if (!xsrfToken && !BASE_URL) {
      xsrfToken = getCookie('XSRF-TOKEN');
    }
    if (!xsrfToken) {
      xsrfToken = await fetchCsrfToken();
    }
    if (!xsrfToken) {
      xsrfToken = getCookie('XSRF-TOKEN');
    }
    if (xsrfToken) {
      defaultHeaders['X-XSRF-TOKEN'] = xsrfToken;
    }
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData) && !(body instanceof Blob)) {
    body = JSON.stringify(body);
  }

  const config = {
    ...options,
    method,
    body,
    credentials: 'include', // Crucial for cross-origin session cookies (JSESSIONID)
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(`${BASE_URL}${endpoint}`, config);

    // Handle 403 Forbidden: differentiate CSRF failures from genuine authorization rejections
    if (response.status === 403) {
      const isMutating = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method);
      const isCsrfError = response.headers.get('x-csrf-error') === 'true';

      // 1. Retry only when the response identifies a CSRF failure via x-csrf-error on a mutating request and not retried yet
      if (isMutating && isCsrfError && !options._csrfRetry) {
        clearCsrfToken();
        const freshToken = await fetchCsrfToken(true);
        if (freshToken) {
          // Preserve original request body, custom headers, and credentials while updating CSRF header
          const retriedHeaders = { ...(options.headers || {}) };
          Object.keys(retriedHeaders).forEach((k) => {
            if (k.toLowerCase() === 'x-xsrf-token') {
              delete retriedHeaders[k];
            }
          });
          retriedHeaders['X-XSRF-TOKEN'] = freshToken;

          // Retried at most once via _csrfRetry: true
          return apiClient(endpoint, {
            ...options,
            _csrfRetry: true,
            headers: retriedHeaders,
          });
        }
      }

      // 2. Genuine authorization failures (or already-retried CSRF failures) are returned immediately
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || `Request failed with status ${response.status}`);
      error.status = response.status;
      error.data = errorData;
      throw error;
    }

    if (response.status === 204) {
      return null;
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const error = new Error(errorData.message || `Request failed with status ${response.status}`);
      error.status = response.status;
      error.data = errorData;
      throw error;
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }
    return await response.text();
  } catch (err) {
    if (options.silent) {
      return null;
    }
    console.warn(`[API Client] Call to ${endpoint} failed:`, err.message);
    throw err;
  }
}
