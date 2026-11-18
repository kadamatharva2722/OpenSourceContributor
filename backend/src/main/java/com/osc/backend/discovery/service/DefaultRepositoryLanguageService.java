package com.osc.backend.discovery.service;

import com.osc.backend.github.model.GitHubRepository;
import com.osc.backend.github.provider.GitHubIssueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production implementation of {@link RepositoryLanguageService} with bounded in-memory caching and TTL.
 *
 * <p>Concurrency & Performance Guarantees:
 * <ul>
 *   <li><b>Non-blocking cache reads:</b> Valid cached entries are returned immediately without locking.</li>
 *   <li><b>De-duplicated concurrent network fetches:</b> In-flight requests for the same repository are de-duplicated
 *       via an in-flight future map, preventing concurrent threads from triggering duplicate GitHub API calls.</li>
 *   <li><b>Bounded Cache Size & TTL:</b> Successful resolutions are cached for 1 hour; failures (404, rate limit, timeout)
 *       are cached for a transient 2-minute window so that recovered network states can resolve without indefinite lockout.</li>
 *   <li><b>Node-local scope:</b> Caching is in-memory per JVM instance; external caching (e.g. Redis) is avoided.</li>
 * </ul>
 */
@Service
public class DefaultRepositoryLanguageService implements RepositoryLanguageService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRepositoryLanguageService.class);

    public static final int MAX_CACHE_ENTRIES = 1000;
    public static final Duration SUCCESS_TTL = Duration.ofHours(1);
    public static final Duration FAILURE_TTL = Duration.ofMinutes(2);

    private record CachedLanguageEntry(Optional<String> language, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final GitHubIssueProvider gitHubIssueProvider;
    private final Map<String, CachedLanguageEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Optional<String>>> inFlight = new ConcurrentHashMap<>();

    @Autowired
    public DefaultRepositoryLanguageService(GitHubIssueProvider gitHubIssueProvider) {
        this.gitHubIssueProvider = gitHubIssueProvider;
    }

    @Override
    public Optional<String> getRepositoryLanguage(String repositoryFullName) {
        if (repositoryFullName == null || repositoryFullName.isBlank()) {
            return Optional.empty();
        }

        String normalizedKey = repositoryFullName.trim().toLowerCase();

        // 1. Fast path: return valid cached entry
        CachedLanguageEntry cached = cache.get(normalizedKey);
        if (cached != null && !cached.isExpired()) {
            return cached.language();
        }

        // 2. De-duplicate concurrent network fetches using inFlight map
        CompletableFuture<Optional<String>> future = inFlight.computeIfAbsent(normalizedKey, key ->
                CompletableFuture.supplyAsync(() -> fetchRepositoryLanguage(repositoryFullName.trim()))
        );

        Optional<String> result;
        try {
            result = future.join();
        } catch (CompletionException ce) {
            log.warn("Asynchronous repository language fetch failed for {}: {}", repositoryFullName, ce.getMessage());
            result = Optional.empty();
        } finally {
            inFlight.remove(normalizedKey);
        }

        // 3. Store result with appropriate TTL (longer for success, shorter for failure/unverified)
        Duration ttl = result.isPresent() ? SUCCESS_TTL : FAILURE_TTL;
        putIntoCache(normalizedKey, new CachedLanguageEntry(result, Instant.now().plus(ttl)));

        return result;
    }

    @Override
    public boolean isLanguageMatching(String repositoryFullName, String requestedLanguage) {
        if (requestedLanguage == null || requestedLanguage.isBlank()) {
            return true;
        }

        Optional<String> repoLangOpt = getRepositoryLanguage(repositoryFullName);
        if (repoLangOpt.isEmpty()) {
            return false;
        }

        return normalizeLanguage(repoLangOpt.get()).equalsIgnoreCase(normalizeLanguage(requestedLanguage));
    }

    @Override
    public boolean isLanguageMismatch(String repositoryFullName, String requestedLanguage) {
        if (requestedLanguage == null || requestedLanguage.isBlank()) {
            return false;
        }

        Optional<String> repoLangOpt = getRepositoryLanguage(repositoryFullName);
        if (repoLangOpt.isEmpty()) {
            // Unverified repository language is not considered a proven mismatch
            return false;
        }

        return !normalizeLanguage(repoLangOpt.get()).equalsIgnoreCase(normalizeLanguage(requestedLanguage));
    }

    @Override
    public void clearCache() {
        cache.clear();
        inFlight.clear();
        log.debug("Repository language cache and in-flight table cleared");
    }

    private void putIntoCache(String key, CachedLanguageEntry entry) {
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            evictStaleEntries();
        }
        cache.put(key, entry);
    }

    private synchronized void evictStaleEntries() {
        Iterator<Map.Entry<String, CachedLanguageEntry>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) {
                it.remove();
            }
        }
        // If still at or over capacity after evicting expired entries, evict up to 10%
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            int toRemove = MAX_CACHE_ENTRIES / 10;
            Iterator<String> keyIt = cache.keySet().iterator();
            while (keyIt.hasNext() && toRemove > 0) {
                keyIt.next();
                keyIt.remove();
                toRemove--;
            }
        }
    }

    private Optional<String> fetchRepositoryLanguage(String repositoryFullName) {
        if (gitHubIssueProvider == null) {
            return Optional.empty();
        }

        String[] parts = repositoryFullName.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            log.warn("Invalid repository coordinates: {}", repositoryFullName);
            return Optional.empty();
        }

        try {
            GitHubRepository repo = gitHubIssueProvider.getRepository(parts[0].trim(), parts[1].trim());
            if (repo != null && repo.getLanguage() != null && !repo.getLanguage().isBlank()) {
                String language = repo.getLanguage().trim();
                log.debug("Verified repository primary language for {}: {}", repositoryFullName, language);
                return Optional.of(language);
            }
        } catch (Exception ex) {
            log.warn("Unable to fetch repository metadata for {} (will treat language as unverified): {}",
                    repositoryFullName, ex.getMessage());
        }

        return Optional.empty();
    }

    private String normalizeLanguage(String lang) {
        if (lang == null) return "";
        String clean = lang.trim().toLowerCase();
        return switch (clean) {
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "rb" -> "ruby";
            case "cs", "c#" -> "csharp";
            case "c++" -> "cpp";
            case "golang" -> "go";
            default -> clean;
        };
    }
}
