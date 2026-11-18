package com.osc.backend.github.provider;

import com.osc.backend.github.client.GitHubClientProperties;
import com.osc.backend.github.dto.upstream.GitHubIssueUpstreamDto;
import com.osc.backend.github.dto.upstream.GitHubLabelUpstreamDto;
import com.osc.backend.github.dto.upstream.GitHubRepositoryUpstreamDto;
import com.osc.backend.github.dto.upstream.GitHubSearchIssuesResponseUpstreamDto;
import com.osc.backend.github.exception.GitHubApiException;
import com.osc.backend.github.exception.GitHubApiUnavailableException;
import com.osc.backend.github.exception.GitHubInvalidResponseException;
import com.osc.backend.github.exception.GitHubRateLimitExceededException;
import com.osc.backend.github.exception.GitHubResourceNotFoundException;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import com.osc.backend.github.model.GitHubLabel;
import com.osc.backend.github.model.GitHubPage;
import com.osc.backend.github.model.GitHubRateLimit;
import com.osc.backend.github.model.GitHubRepository;
import com.osc.backend.github.util.GitHubLinkHeaderParser;
import com.osc.backend.github.util.GitHubRateLimitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Public REST API implementation of {@link GitHubIssueProvider}.
 *
 * <p>CRITICAL SECURITY GUARANTEES:
 * <ul>
 *   <li>Communicates exclusively with GitHub's public REST endpoints.</li>
 *   <li>NEVER retrieves, stores, or sends OAuth tokens or user credentials.</li>
 *   <li>Does not attach any Authorization header.</li>
 * </ul>
 */
@Service
public class PublicGitHubProvider implements GitHubIssueProvider {

    private static final Logger log = LoggerFactory.getLogger(PublicGitHubProvider.class);

    private final RestClient restClient;
    private final GitHubClientProperties properties;

    public PublicGitHubProvider(@Qualifier("gitHubRestClient") RestClient restClient,
                                GitHubClientProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public GitHubPage<GitHubIssue> searchIssues(GitHubIssueSearchFilter filter) {
        GitHubIssueSearchFilter actualFilter = filter != null ? filter : GitHubIssueSearchFilter.builder().build();

        // Enforce GitHub Search API accessible window (1,000 maximum results)
        if (actualFilter.getPage() > 1 && (long) (actualFilter.getPage() - 1) * actualFilter.getPerPage() >= GitHubPage.GITHUB_MAX_SEARCH_RESULTS) {
            int maxPage = (int) Math.ceil((double) GitHubPage.GITHUB_MAX_SEARCH_RESULTS / actualFilter.getPerPage());
            throw new IllegalArgumentException(
                    "Page request (" + actualFilter.getPage() + ") exceeds the maximum accessible GitHub Search window of "
                            + GitHubPage.GITHUB_MAX_SEARCH_RESULTS + " results. Maximum accessible page for page size "
                            + actualFilter.getPerPage() + " is " + maxPage + "."
            );
        }

        String queryString = actualFilter.toQueryString();

        log.debug("Searching GitHub issues with query: [{}], page: {}, perPage: {}",
                queryString, actualFilter.getPage(), actualFilter.getPerPage());

        return executeWithRetry(() -> {
            ResponseEntity<GitHubSearchIssuesResponseUpstreamDto> response = restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/search/issues")
                                .queryParam("q", queryString)
                                .queryParam("page", actualFilter.getPage())
                                .queryParam("per_page", actualFilter.getPerPage());
                        if (actualFilter.getSort() != null && !actualFilter.getSort().trim().isEmpty()) {
                            b.queryParam("sort", actualFilter.getSort().trim());
                        }
                        if (actualFilter.getOrder() != null && !actualFilter.getOrder().trim().isEmpty()) {
                            b.queryParam("order", actualFilter.getOrder().trim());
                        }
                        return b.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> handleClientError(req, resp, "search/issues"))
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> handleServerError(resp))
                    .toEntity(GitHubSearchIssuesResponseUpstreamDto.class);

            HttpHeaders headers = response.getHeaders();
            GitHubRateLimit rateLimit = GitHubRateLimitParser.parse(headers);
            if (rateLimit != null && rateLimit.getRemaining() < 10) {
                log.warn("GitHub API rate limit remaining is low: {} remaining (resets at {})",
                        rateLimit.getRemaining(), rateLimit.getResetTime());
            }

            GitHubSearchIssuesResponseUpstreamDto body = response.getBody();
            if (body == null) {
                return GitHubPage.empty(actualFilter.getPage(), actualFilter.getPerPage(), rateLimit);
            }

            // Exclude pull requests (GitHub search API returns PRs unless filtered)
            List<GitHubIssue> domainIssues = body.getItems() != null
                    ? body.getItems().stream()
                    .filter(item -> !item.isPullRequest())
                    .map(this::toDomainIssue)
                    .toList()
                    : Collections.emptyList();

            long totalCount = body.getTotalCount() != null ? body.getTotalCount() : 0L;
            long accessibleCount = Math.min(Math.max(0L, totalCount), GitHubPage.GITHUB_MAX_SEARCH_RESULTS);
            int totalPages = accessibleCount > 0 ? (int) Math.ceil((double) accessibleCount / actualFilter.getPerPage()) : 0;

            // Parse RFC 5988 Link header for pagination
            String linkHeader = headers.getFirst(HttpHeaders.LINK);
            GitHubLinkHeaderParser.PaginationInfo linkInfo = GitHubLinkHeaderParser.parse(linkHeader);

            boolean hasPrevious = actualFilter.getPage() > 1;
            boolean hasNext = actualFilter.getPage() < totalPages
                    && (linkInfo.hasNext() || ((long) actualFilter.getPage() * actualFilter.getPerPage() < accessibleCount));

            return new GitHubPage<>(
                    domainIssues,
                    totalCount,
                    actualFilter.getPage(),
                    actualFilter.getPerPage(),
                    hasNext,
                    hasPrevious,
                    rateLimit
            );
        });
    }

    @Override
    public GitHubRepository getRepository(String owner, String repo) {
        if (owner == null || owner.trim().isEmpty() || repo == null || repo.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository owner and name must not be empty");
        }

        String repoFullName = owner.trim() + "/" + repo.trim();
        log.debug("Retrieving GitHub repository: {}", repoFullName);

        return executeWithRetry(() -> {
            ResponseEntity<GitHubRepositoryUpstreamDto> response = restClient.get()
                    .uri("/repos/{owner}/{repo}", owner.trim(), repo.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> handleClientError(req, resp, repoFullName))
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> handleServerError(resp))
                    .toEntity(GitHubRepositoryUpstreamDto.class);

            GitHubRepositoryUpstreamDto body = response.getBody();
            if (body == null) {
                throw new GitHubInvalidResponseException("Empty response body received for repository: " + repoFullName);
            }

            return toDomainRepository(body);
        });
    }

    @Override
    public GitHubIssue getIssue(String owner, String repo, int issueNumber) {
        if (owner == null || owner.trim().isEmpty() || repo == null || repo.trim().isEmpty() || issueNumber <= 0) {
            throw new IllegalArgumentException("Invalid repository coordinates or issue number");
        }

        String issueIdentifier = owner.trim() + "/" + repo.trim() + "#" + issueNumber;
        log.debug("Retrieving GitHub issue: {}", issueIdentifier);

        return executeWithRetry(() -> {
            ResponseEntity<GitHubIssueUpstreamDto> response = restClient.get()
                    .uri("/repos/{owner}/{repo}/issues/{issueNumber}", owner.trim(), repo.trim(), issueNumber)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> handleClientError(req, resp, issueIdentifier))
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> handleServerError(resp))
                    .toEntity(GitHubIssueUpstreamDto.class);

            GitHubIssueUpstreamDto body = response.getBody();
            if (body == null) {
                throw new GitHubInvalidResponseException("Empty response body received for issue: " + issueIdentifier);
            }

            if (body.isPullRequest()) {
                throw new GitHubResourceNotFoundException(
                        issueIdentifier,
                        "Resource at " + issueIdentifier + " is a pull request, not an issue"
                );
            }

            return toDomainIssue(body);
        });
    }

    /**
     * Executes an operation with bounded retries for transient 5xx and network failures only.
     * Never retries 4xx or rate limit exceptions.
     */
    private <T> T executeWithRetry(Supplier<T> action) {
        int maxAttempts = Math.max(1, properties.getMaxRetries());
        long backoffMs = Math.max(0, properties.getRetryBackoffMs());

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (GitHubRateLimitExceededException | GitHubResourceNotFoundException ex) {
                // Rate limits and 404s are non-transient client/quota states; never retry
                throw ex;
            } catch (GitHubApiUnavailableException ex) {
                lastException = ex;
                if (attempt == maxAttempts) {
                    log.error("GitHub API 5xx failure persisted after {} attempts: {}", maxAttempts, ex.getMessage());
                    throw ex;
                }
                log.warn("GitHub API transient 5xx error (attempt {} of {}). Retrying...", attempt, maxAttempts);
                sleepBackoff(backoffMs, attempt);
            } catch (ResourceAccessException ex) {
                lastException = ex;
                if (attempt == maxAttempts) {
                    log.error("GitHub API connection failure persisted after {} attempts: {}", maxAttempts, ex.getMessage());
                    throw new GitHubApiUnavailableException("GitHub API connection failed after " + maxAttempts + " attempts", ex);
                }
                log.warn("GitHub API network timeout/failure (attempt {} of {}). Retrying...", attempt, maxAttempts);
                sleepBackoff(backoffMs, attempt);
            } catch (GitHubApiException ex) {
                // Non-retriable GitHub error
                throw ex;
            } catch (RestClientException ex) {
                lastException = ex;
                if (attempt == maxAttempts) {
                    throw new GitHubApiException("GitHub API communication error: " + ex.getMessage(), ex);
                }
                log.warn("GitHub API request error (attempt {} of {}). Retrying...", attempt, maxAttempts);
                sleepBackoff(backoffMs, attempt);
            } catch (Exception ex) {
                throw new GitHubApiException("Unexpected error calling GitHub API: " + ex.getMessage(), ex);
            }
        }

        throw new GitHubApiUnavailableException("GitHub API unavailable after " + maxAttempts + " attempts", lastException);
    }

    private void sleepBackoff(long backoffMs, int attempt) {
        long delay = backoffMs * attempt;
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitHubApiException("Interrupted during GitHub API retry backoff", e);
        }
    }

    private void handleClientError(HttpRequest req, ClientHttpResponse resp, String resourceIdentifier) throws IOException {
        HttpHeaders headers = resp.getHeaders();
        GitHubRateLimit rateLimit = GitHubRateLimitParser.parse(headers);
        int status = resp.getStatusCode().value();

        if (status == 429 || (status == 403 && rateLimit != null && rateLimit.isExhausted())) {
            log.warn("GitHub API rate limit exceeded (status: {}). RateLimit: {}", status, rateLimit);
            throw new GitHubRateLimitExceededException(
                    "GitHub API rate limit exceeded. Reset at: " + (rateLimit != null ? rateLimit.getResetTime() : "unknown"),
                    rateLimit
            );
        }

        if (status == 404) {
            String path = resourceIdentifier != null ? resourceIdentifier : req.getURI().getPath();
            log.warn("GitHub API resource not found (status 404): {}", path);
            throw new GitHubResourceNotFoundException(path, "GitHub resource not found: " + path);
        }

        log.warn("GitHub API client error: {} on {}", status, req.getURI());
        throw new GitHubApiException("GitHub API client error: " + status, status);
    }

    private void handleServerError(ClientHttpResponse resp) throws IOException {
        int status = resp.getStatusCode().value();
        log.warn("GitHub API returned server error: {}", status);
        throw new GitHubApiUnavailableException("GitHub API server error: " + status, status, null);
    }

    private GitHubIssue toDomainIssue(GitHubIssueUpstreamDto upstream) {
        if (upstream == null) {
            return null;
        }

        List<GitHubLabel> labels = upstream.getLabels() != null
                ? upstream.getLabels().stream().map(this::toDomainLabel).toList()
                : Collections.emptyList();

        String authorUsername = upstream.getUser() != null ? upstream.getUser().getLogin() : null;
        String authorAvatarUrl = upstream.getUser() != null ? upstream.getUser().getAvatarUrl() : null;

        return new GitHubIssue(
                upstream.getId(),
                upstream.getNumber(),
                upstream.getTitle(),
                upstream.getBody(),
                upstream.getHtmlUrl(),
                upstream.getState(),
                upstream.extractRepositoryFullName(),
                labels,
                authorUsername,
                authorAvatarUrl,
                upstream.getComments(),
                upstream.getCreatedAt(),
                upstream.getUpdatedAt(),
                upstream.getClosedAt()
        );
    }

    private GitHubLabel toDomainLabel(GitHubLabelUpstreamDto upstream) {
        if (upstream == null) {
            return null;
        }
        return new GitHubLabel(
                upstream.getId(),
                upstream.getName(),
                upstream.getColor(),
                upstream.getDescription()
        );
    }

    private GitHubRepository toDomainRepository(GitHubRepositoryUpstreamDto upstream) {
        if (upstream == null) {
            return null;
        }

        String licenseName = upstream.getLicense() != null ? upstream.getLicense().getName() : null;

        return new GitHubRepository(
                upstream.getId(),
                upstream.getName(),
                upstream.getFullName(),
                upstream.getDescription(),
                upstream.getHtmlUrl(),
                upstream.getLanguage(),
                upstream.getStargazersCount(),
                upstream.getForksCount(),
                upstream.getOpenIssuesCount(),
                upstream.getTopics(),
                licenseName,
                upstream.getCreatedAt(),
                upstream.getUpdatedAt(),
                upstream.getPushedAt()
        );
    }
}
