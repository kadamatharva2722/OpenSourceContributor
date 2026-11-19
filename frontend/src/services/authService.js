import { apiClient, getBaseUrl, fetchCsrfToken, clearCsrfToken } from './apiClient';

/**
 * Checks current session authentication status with the backend.
 * @returns {Promise<{authenticated: boolean, userId: number|null, username: string|null}>}
 */
export async function getAuthStatus() {
  try {
    const data = await apiClient('/api/v1/auth/status', { silent: true });
    if (data && typeof data.authenticated === 'boolean') {
      return data;
    }
    return { authenticated: false, userId: null, username: null };
  } catch (err) {
    return { authenticated: false, userId: null, username: null };
  }
}

/**
 * Retrieves the full profile of the currently authenticated user.
 */
export async function getCurrentUser() {
  return await apiClient('/api/v1/auth/me');
}

/**
 * Invalidates the current user session and clears authentication cookies.
 */
export async function logoutUser() {
  // First ensure active CSRF token is available
  await fetchCsrfToken().catch(() => {});
  const result = await apiClient('/api/v1/auth/logout', { method: 'POST' });
  clearCsrfToken();
  return result;
}

/**
 * Fetches the active CSRF token and sets the XSRF-TOKEN cookie.
 */
export async function getCsrfToken() {
  const token = await fetchCsrfToken();
  if (token) {
    return { token };
  }
  return await apiClient('/api/v1/auth/csrf');
}

const OAUTH_BACKEND_URL =
  import.meta.env.VITE_BACKEND_URL ||
  import.meta.env.VITE_API_URL ||
  (import.meta.env.PROD
    ? 'https://opensourcecontributor-backend.onrender.com'
    : getBaseUrl());

/**
 * Returns the full backend GitHub OAuth2 initiation URL.
 * In production, navigates directly to the Render backend domain where the OAuth
 * callback is registered, ensuring the JSESSIONID authorization request cookie matches.
 */
export function getOAuthLoginUrl() {
  return `${OAUTH_BACKEND_URL}/oauth2/authorization/github`;
}
