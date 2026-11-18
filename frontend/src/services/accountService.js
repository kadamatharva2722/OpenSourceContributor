import { apiClient } from './apiClient';

/**
 * Account Service
 * Handles user account lifecycle operations including permanent account deletion.
 */
export const accountService = {
  /**
   * Permanently deletes the authenticated contributor's account, preferences,
   * saved issues, contributions, and AI usage records.
   * Automatically attaches credentials and X-XSRF-TOKEN header.
   *
   * @returns {Promise<{message: string}>}
   */
  async deleteAccount() {
    return await apiClient('/api/v1/account', {
      method: 'DELETE',
    });
  },
};
