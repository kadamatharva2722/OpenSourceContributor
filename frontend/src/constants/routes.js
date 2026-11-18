/**
 * OpenSourceContributor - Route Constants
 */
export const ROUTES = {
  HOME: '/',
  EXPLORE: '/explore',
  ISSUE_DETAILS: '/issues/:id',
  HOW_IT_WORKS: '/how-it-works',
  ABOUT: '/about',
  CONTACT: '/contact',
  LOGIN: '/login',
  AUTH_CALLBACK: '/auth/callback',
  DASHBOARD: '/dashboard',
  SAVED_ISSUES: '/saved-issues',
  CRATE: '/crate',
  MY_CONTRIBUTIONS: '/my-contributions',
  SETTINGS: '/settings',
  PRIVACY: '/privacy',
  TERMS: '/terms',
  COOKIE_PREFERENCES: '/cookie-preferences',
  THANK_YOU: '/thank-you',
  NOT_FOUND: '*',
};

export const getIssueDetailsRoute = (id) => `/issues/${id}`;
