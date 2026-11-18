import { apiClient } from './apiClient';

/**
 * Service for communicating with the Spring Boot Contributions API (/api/v1/contributions).
 * Session-based authentication is strictly enforced by Spring Security.
 */
export const contributionService = {
  /**
   * Retrieves contributions for the authenticated contributor, optionally filtered by status.
   * @param {string|null} [status] - Optional ContributionStatus (PLANNING, IN_PROGRESS, PR_OPENED, COMPLETED)
   * @returns {Promise<Array>}
   */
  async getContributions(status = null) {
    const endpoint = status && status !== 'ALL'
      ? `/api/v1/contributions?status=${encodeURIComponent(status)}`
      : '/api/v1/contributions';
    const data = await apiClient(endpoint);
    return Array.isArray(data) ? data : [];
  },

  /**
   * Retrieves a single contribution record by internal ID.
   * @param {number|string} id
   */
  async getContributionById(id) {
    return await apiClient(`/api/v1/contributions/${id}`);
  },

  /**
   * Logs a new contribution tracking record.
   * @param {{githubIssueId: number, repositoryFullName: string, issueNumber: number, title: string, status?: string, branchName?: string, pullRequestUrl?: string, notes?: string}} data
   */
  async createContribution({
    githubIssueId,
    repositoryFullName,
    issueNumber,
    title,
    status = 'PLANNING',
    branchName = '',
    pullRequestUrl = '',
    notes = '',
  }) {
    return await apiClient('/api/v1/contributions', {
      method: 'POST',
      body: {
        githubIssueId: Number(githubIssueId),
        repositoryFullName: String(repositoryFullName),
        issueNumber: Number(issueNumber),
        title: String(title),
        status: status || 'PLANNING',
        branchName: branchName || undefined,
        pullRequestUrl: pullRequestUrl || undefined,
        notes: notes || undefined,
      },
    });
  },

  /**
   * Updates details (status, branchName, pullRequestUrl, notes) for an existing contribution.
   * @param {number|string} id
   * @param {{status?: string, branchName?: string, pullRequestUrl?: string, notes?: string}} data
   */
  async updateContribution(id, { status, branchName, pullRequestUrl, notes }) {
    return await apiClient(`/api/v1/contributions/${id}`, {
      method: 'PATCH',
      body: {
        status: status || undefined,
        branchName: branchName !== undefined ? branchName : undefined,
        pullRequestUrl: pullRequestUrl !== undefined ? pullRequestUrl : undefined,
        notes: notes !== undefined ? notes : undefined,
      },
    });
  },

  /**
   * Deletes a contribution tracking record by internal ID.
   * @param {number|string} id
   */
  async deleteContribution(id) {
    return await apiClient(`/api/v1/contributions/${id}`, {
      method: 'DELETE',
    });
  },
};
