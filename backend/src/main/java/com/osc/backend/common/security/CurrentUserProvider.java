package com.osc.backend.common.security;

/**
 * Contract for resolving the current authenticated user context.
 * Guarantees that local database user ID is retrieved from security context,
 * preventing frontend user ID spoofing.
 */
public interface CurrentUserProvider {

    /**
     * Obtains the authenticated application's local database user ID.
     *
     * @return non-null local database user ID.
     * @throws com.osc.backend.common.exception.UnauthenticatedException if not authenticated.
     */
    Long getCurrentUserId();

    /**
     * Obtains the authenticated application principal.
     *
     * @return non-null AppUserPrincipal.
     * @throws com.osc.backend.common.exception.UnauthenticatedException if not authenticated.
     */
    AppUserPrincipal getCurrentPrincipal();
}
