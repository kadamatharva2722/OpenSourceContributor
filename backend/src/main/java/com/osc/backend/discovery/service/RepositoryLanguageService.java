package com.osc.backend.discovery.service;

import java.util.Optional;

/**
 * Service for verifying and caching the primary programming language of GitHub repositories.
 *
 * <p>SEMANTIC AND ARCHITECTURAL SPECIFICATION:
 * <ul>
 *   <li><b>Repository Primary Language vs. Issue Language:</b> GitHub tracks a detected primary
 *       language per repository (based on code byte count via {@code GET /repos/{owner}/{repo}}),
 *       not per issue. Verifying a repository's primary language confirms the dominant language of
 *       the codebase, but does not guarantee that every issue in a multi-language or polyglot repository
 *       pertains strictly to that language.</li>
 *   <li><b>Languages Endpoint Consideration:</b> GitHub also offers {@code GET /repos/{owner}/{repo}/languages}
 *       which lists all detected languages and byte counts. Calling this endpoint for every issue search
 *       would double API requests and rapidly exhaust unauthenticated rate limits (60 requests/hour),
 *       without providing per-issue language certainty. Hence, primary language verification via the
 *       standard repository metadata endpoint is the sound, bounded choice.</li>
 *   <li><b>Cache Scope:</b> Node-local in-memory cache bounded with TTL. In multi-instance deployments,
 *       each backend instance maintains its own bounded local cache. External caching (e.g. Redis)
 *       is intentionally deferred until clustering requirements dictate it.</li>
 * </ul>
 */
public interface RepositoryLanguageService {

    /**
     * Resolves the verified primary programming language of the given GitHub repository.
     * Uses in-memory cache to prevent redundant GitHub REST API requests.
     *
     * @param repositoryFullName full repository name in "owner/repo" format
     * @return Optional containing verified primary language (e.g. "Java"), or empty if unverified or unavailable
     */
    Optional<String> getRepositoryLanguage(String repositoryFullName);

    /**
     * Alias for {@link #getRepositoryLanguage(String)}, emphasizing primary language semantics.
     *
     * @param repositoryFullName full repository name in "owner/repo" format
     * @return Optional containing verified primary language, or empty if unverified
     */
    default Optional<String> getRepositoryPrimaryLanguage(String repositoryFullName) {
        return getRepositoryLanguage(repositoryFullName);
    }

    /**
     * Verifies if the repository's primary language matches the requested programming language.
     *
     * @param repositoryFullName full repository name in "owner/repo" format
     * @param requestedLanguage  target language to check against (e.g. "Java")
     * @return true if repository primary language is verified and matches requested language; false if unverified or different
     */
    boolean isLanguageMatching(String repositoryFullName, String requestedLanguage);

    /**
     * Checks if the repository's verified primary language is known and does NOT match the requested language.
     *
     * @param repositoryFullName full repository name in "owner/repo" format
     * @param requestedLanguage  target language to check against (e.g. "Java")
     * @return true if repository primary language is verified and definitely differs from requestedLanguage;
     *         false if requestedLanguage is null/empty, or if repository language is unverified
     */
    boolean isLanguageMismatch(String repositoryFullName, String requestedLanguage);

    /**
     * Clears all cached repository metadata.
     */
    void clearCache();
}
