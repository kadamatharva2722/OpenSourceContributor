/**
 * OpenSourceContributor - Discovery Service
 * Handles both public guest issue browsing and authenticated AI-powered semantic search:
 * - Guest Mode: GET /api/v1/discovery/public (No AI, no quota, IP rate-limited)
 * - Authenticated Mode: POST /api/v1/discovery/search (Groq AI, 20 daily quota, CSRF protected)
 */

import { apiClient, getCookie } from './apiClient';
import { getCsrfToken } from './authService';

/**
 * Maximum accessible search window in GitHub REST Search API (1,000 results per query).
 */
export const GITHUB_MAX_SEARCH_RESULTS = 1000;

/**
 * Validates natural language search prompt for AI discovery before submission.
 * Enforces backend constraint: min 3, max 500 characters, non-blank.
 *
 * @param {string} prompt
 * @returns {{ valid: boolean, error?: string }}
 */
export function validateSearchPrompt(prompt) {
  if (!prompt || typeof prompt !== 'string') {
    return { valid: false, error: 'Search prompt must not be blank' };
  }
  const trimmed = prompt.trim();
  if (trimmed.length === 0) {
    return { valid: false, error: 'Search prompt must not be blank' };
  }
  if (trimmed.length < 3) {
    return { valid: false, error: 'Search prompt must be at least 3 characters' };
  }
  if (trimmed.length > 500) {
    return { valid: false, error: 'Search prompt cannot exceed 500 characters' };
  }
  return { valid: true };
}

/**
 * Validates public discovery parameters.
 * Enforces requirement: at least one filter must be provided.
 *
 * @param {Object} params
 * @returns {{ valid: boolean, error?: string }}
 */
export function validatePublicFilter(params = {}) {
  const { query, language, repository, labels } = params;
  const hasQuery = query && typeof query === 'string' && query.trim().length > 0;
  const hasLang = language && typeof language === 'string' && language.trim().length > 0 && language !== 'ALL';
  const hasRepo = repository && typeof repository === 'string' && repository.trim().length > 0;
  const hasLabels = labels && (
    (Array.isArray(labels) && labels.length > 0) ||
    (typeof labels === 'string' && labels.trim().length > 0 && labels !== 'ALL')
  );

  if (!hasQuery && !hasLang && !hasRepo && !hasLabels) {
    return {
      valid: false,
      error: 'Please enter search keywords or select at least one filter (language, label, or repository).',
    };
  }
  return { valid: true };
}

/**
 * Browses public GitHub issues as an anonymous guest.
 * Calls GET /api/v1/discovery/public
 * Bypasses Groq LLM, PostgreSQL quota deduction, and user authentication.
 *
 * @param {Object} options
 * @param {string} [options.query] Keywords in issue title/body
 * @param {string} [options.language] Programming language filter
 * @param {string} [options.repository] Repository full name (owner/repo)
 * @param {string[]|string} [options.labels] GitHub labels
 * @param {string} [options.state='open'] State (open/closed)
 * @param {string} [options.sort='updated'] Sort field
 * @param {string} [options.order='desc'] Sort order
 * @param {number} [options.page=1] Page number (1-based)
 * @param {number} [options.perPage=30] Items per page
 * @returns {Promise<DiscoverySearchResponse>}
 */
export async function browsePublicIssues(options = {}) {
  const validation = validatePublicFilter(options);
  if (!validation.valid) {
    const error = new Error(validation.error);
    error.status = 400;
    error.isValidationError = true;
    throw error;
  }

  const {
    query,
    language,
    repository,
    labels,
    state = 'open',
    sort = 'updated',
    order = 'desc',
    page = 1,
    perPage = 30,
  } = options;

  const params = new URLSearchParams();
  if (query && query.trim()) params.append('query', query.trim());
  if (language && language.trim() && language !== 'ALL') params.append('language', language.trim());
  if (repository && repository.trim()) params.append('repository', repository.trim());

  if (labels) {
    if (Array.isArray(labels)) {
      labels.forEach((l) => {
        if (l && l.trim()) params.append('labels', l.trim());
      });
    } else if (typeof labels === 'string' && labels.trim() && labels !== 'ALL') {
      params.append('labels', labels.trim());
    }
  }

  if (state) params.append('state', state);
  if (sort) params.append('sort', sort);
  if (order) params.append('order', order);

  const safePerPage = Math.min(100, Math.max(1, parseInt(perPage, 10) || 30));
  const maxAccessiblePage = Math.ceil(GITHUB_MAX_SEARCH_RESULTS / safePerPage);
  const safePage = Math.min(Math.max(1, parseInt(page, 10) || 1), maxAccessiblePage);

  params.append('page', String(safePage));
  params.append('perPage', String(safePerPage));

  const qs = params.toString();
  return await apiClient(`/api/v1/discovery/public${qs ? `?${qs}` : ''}`);
}

/**
 * Discovers GitHub issues via AI natural language interpretation and GitHub REST API.
 * Calls POST /api/v1/discovery/search
 * Requires authenticated user session and consumes 1 daily AI search quota (out of 20).
 *
 * @param {Object} options
 * @param {string} options.prompt Natural language prompt describing skills and desired issues
 * @param {number} [options.page=1] 1-based page number
 * @param {number} [options.perPage=30] Number of issues per page (1-100)
 * @returns {Promise<DiscoverySearchResponse>}
 */
export async function searchAiIssues({ prompt, page = 1, perPage = 30 }) {
  const validation = validateSearchPrompt(prompt);
  if (!validation.valid) {
    const validationError = new Error(validation.error);
    validationError.status = 400;
    validationError.isValidationError = true;
    throw validationError;
  }

  const safePerPage = Math.min(100, Math.max(1, parseInt(perPage, 10) || 30));
  const maxAccessiblePage = Math.ceil(GITHUB_MAX_SEARCH_RESULTS / safePerPage);
  const safePage = Math.min(Math.max(1, parseInt(page, 10) || 1), maxAccessiblePage);

  const payload = {
    prompt: prompt.trim(),
    page: safePage,
    perPage: safePerPage,
  };

  // Ensure CSRF token is present in cookies before sending mutating POST request
  if (!getCookie('XSRF-TOKEN')) {
    try {
      await getCsrfToken();
    } catch (csrfErr) {
      console.warn('[DiscoveryService] Prefetching CSRF token failed:', csrfErr.message);
    }
  }

  try {
    return await apiClient('/api/v1/discovery/search', {
      method: 'POST',
      body: payload,
    });
  } catch (err) {
    // If CSRF token expired or missing (403), refresh once and retry
    if (err.status === 403 && !err._retried) {
      try {
        await getCsrfToken();
        const retryRes = await apiClient('/api/v1/discovery/search', {
          method: 'POST',
          body: payload,
        });
        return retryRes;
      } catch (retryErr) {
        retryErr._retried = true;
        throw retryErr;
      }
    }
    throw err;
  }
}

// Backwards-compatible alias for existing imports
export const searchIssues = searchAiIssues;
