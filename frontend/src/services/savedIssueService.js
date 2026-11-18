import { apiClient } from './apiClient';

/**
 * Service for communicating with the Spring Boot Saved Issues API (/api/v1/saved-issues).
 * Session-based authentication is enforced on all endpoints by Spring Security.
 */
export const savedIssueService = {
  /**
   * Retrieves all bookmarked issues for the authenticated contributor.
   * @returns {Promise<Array<{id: number, userId: number, githubIssueId: number, repositoryFullName: string, issueNumber: number, title: string, issueUrl: string, savedAt: string}>>}
   */
  async getSavedIssues() {
    const data = await apiClient('/api/v1/saved-issues');
    return Array.isArray(data) ? data : [];
  },

  /**
   * Retrieves a single saved issue by its internal ID.
   * @param {number|string} id
   */
  async getSavedIssueById(id) {
    return await apiClient(`/api/v1/saved-issues/${id}`);
  },

  /**
   * Bookmarks a GitHub issue for the current user.
   * @param {{githubIssueId: number, repositoryFullName: string, issueNumber: number, title: string, issueUrl: string}} issueData
   */
  async saveIssue({ githubIssueId, repositoryFullName, issueNumber, title, issueUrl }) {
    return await apiClient('/api/v1/saved-issues', {
      method: 'POST',
      body: {
        githubIssueId: Number(githubIssueId),
        repositoryFullName: String(repositoryFullName),
        issueNumber: Number(issueNumber),
        title: String(title),
        issueUrl: String(issueUrl),
      },
    });
  },

  /**
   * Removes a saved issue by its internal ID.
   * @param {number|string} id
   */
  async deleteSavedIssue(id) {
    return await apiClient(`/api/v1/saved-issues/${id}`, {
      method: 'DELETE',
    });
  },

  /**
   * Checks whether a specific GitHub issue is already saved by the current user.
   * @param {number|string} githubIssueId
   * @returns {Promise<boolean>}
   */
  async isIssueSaved(githubIssueId) {
    try {
      const res = await apiClient(`/api/v1/saved-issues/check/${githubIssueId}`, { silent: true });
      return Boolean(res && res.saved);
    } catch {
      return false;
    }
  },
};
