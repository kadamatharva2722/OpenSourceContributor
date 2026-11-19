/**
 * OpenSourceContributor - API Client
 * Configured for Spring Boot backend session authentication.
 * Uses credentials: 'include' for secure HttpOnly cookie persistence
 * and automatically sends CSRF tokens on state-changing requests.
 */

const BASE_URL = import.meta.env.VITE_API_URL || import.meta.env.VITE_BACKEND_URL || '';

export function getBaseUrl() {
  return BASE_URL;
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

  // Attach CSRF token on mutating requests if present in cookies
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
    let xsrfToken = getCookie('XSRF-TOKEN');
    if (!xsrfToken) {
      try {
        await fetch(`${BASE_URL}/api/v1/auth/csrf`, { credentials: 'include' });
        xsrfToken = getCookie('XSRF-TOKEN');
      } catch {
        // Proactive CSRF token fetch fallback
      }
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
