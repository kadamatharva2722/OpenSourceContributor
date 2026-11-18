import { apiClient } from './apiClient';

/**
 * Service for communicating with the Spring Boot User Preferences API (/api/v1/preferences).
 * All operations derive user identity strictly from the authenticated Spring Security session.
 */
export const userPreferenceService = {
  /**
   * Retrieves discovery preferences for the current contributor.
   * @returns {Promise<{id: number, userId: number, preferredLanguages: string[], preferredDomains: string[], difficulty: string}>}
   */
  async getPreferences() {
    try {
      const data = await apiClient('/api/v1/preferences', { silent: true });
      if (data && typeof data === 'object') {
        return {
          preferredLanguages: Array.isArray(data.preferredLanguages) ? data.preferredLanguages : [],
          preferredDomains: Array.isArray(data.preferredDomains) ? data.preferredDomains : [],
          difficulty: data.difficulty || 'Intermediate',
        };
      }
    } catch {
      // Fallback defaults if preferences not yet created
    }
    return {
      preferredLanguages: [],
      preferredDomains: [],
      difficulty: 'Intermediate',
    };
  },

  /**
   * Creates or updates discovery preferences for the authenticated user.
   * @param {{preferredLanguages?: string[], preferredDomains?: string[], difficulty?: string}} prefs
   */
  async updatePreferences({ preferredLanguages = [], preferredDomains = [], difficulty = 'Intermediate' }) {
    return await apiClient('/api/v1/preferences', {
      method: 'PUT',
      body: {
        preferredLanguages,
        preferredDomains,
        difficulty,
      },
    });
  },
};
