package com.osc.backend.discovery.service;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.service.AiUsageService;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.security.ClientIpRateLimiter;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.IssueMatchDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.dto.PublicDiscoveryRequestDto;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.filter.UnsuitableIssueFilter;
import com.osc.backend.discovery.parser.SearchQueryParser;
import com.osc.backend.discovery.ranking.DefaultIssueRanker;
import com.osc.backend.discovery.ranking.IssueRanker;
import com.osc.backend.discovery.ranking.ScoredIssue;
import com.osc.backend.discovery.validator.SearchFilterValidator;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import com.osc.backend.github.model.GitHubLabel;
import com.osc.backend.github.model.GitHubPage;
import com.osc.backend.github.provider.GitHubIssueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates GitHub issue discovery for both authenticated AI contributors and public guests.
 */
@Service
public class DiscoveryServiceImpl implements DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryServiceImpl.class);

    private final SearchQueryParser searchQueryParser;
    private final SearchFilterValidator validator;
    private final GitHubIssueProvider gitHubIssueProvider;
    private final MatchExplanationGenerator explanationGenerator;
    private final AiUsageService aiUsageService;
    private final CurrentUserProvider currentUserProvider;
    private final IssueRanker issueRanker;
    private final RepositoryLanguageService repositoryLanguageService;
    private final UnsuitableIssueFilter unsuitableIssueFilter;
    private final ClientIpRateLimiter clientIpRateLimiter;

    /**
     * Primary constructor explicitly selected by Spring via {@code @Autowired}.
     */
    @Autowired
    public DiscoveryServiceImpl(
            SearchQueryParser searchQueryParser,
            SearchFilterValidator validator,
            GitHubIssueProvider gitHubIssueProvider,
            MatchExplanationGenerator explanationGenerator,
            AiUsageService aiUsageService,
            CurrentUserProvider currentUserProvider,
            IssueRanker issueRanker,
            RepositoryLanguageService repositoryLanguageService,
            UnsuitableIssueFilter unsuitableIssueFilter,
            @Autowired(required = false) ClientIpRateLimiter clientIpRateLimiter
    ) {
        this.searchQueryParser = searchQueryParser;
        this.validator = validator;
        this.gitHubIssueProvider = gitHubIssueProvider;
        this.explanationGenerator = explanationGenerator;
        this.aiUsageService = aiUsageService;
        this.currentUserProvider = currentUserProvider;
        this.repositoryLanguageService = repositoryLanguageService != null
                ? repositoryLanguageService
                : new DefaultRepositoryLanguageService(gitHubIssueProvider);
        this.issueRanker = issueRanker != null
                ? issueRanker
                : new DefaultIssueRanker(this.repositoryLanguageService);
        this.unsuitableIssueFilter = unsuitableIssueFilter != null
                ? unsuitableIssueFilter
                : new UnsuitableIssueFilter();
        this.clientIpRateLimiter = clientIpRateLimiter != null
                ? clientIpRateLimiter
                : new ClientIpRateLimiter();
    }

    /**
     * Convenience constructor for tests.
     */
    public DiscoveryServiceImpl(
            SearchQueryParser searchQueryParser,
            SearchFilterValidator validator,
            GitHubIssueProvider gitHubIssueProvider,
            MatchExplanationGenerator explanationGenerator,
            AiUsageService aiUsageService,
            CurrentUserProvider currentUserProvider,
            IssueRanker issueRanker
    ) {
        this(
                searchQueryParser,
                validator,
                gitHubIssueProvider,
                explanationGenerator,
                aiUsageService,
                currentUserProvider,
                issueRanker,
                new DefaultRepositoryLanguageService(gitHubIssueProvider),
                new UnsuitableIssueFilter(),
                new ClientIpRateLimiter()
        );
    }

    @Override
    public DiscoverySearchResponseDto discoverIssues(DiscoverySearchRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Discovery search request must not be null");
        }

        // 1. Fast fail-precheck: validate user prompt syntax, length boundaries, and pagination limits
        validator.validatePrompt(request.getPrompt());
        validator.validatePagination(request.getPage(), request.getPerPage());

        // 2. Resolve authenticated user identity safely from server session
        Long userId = currentUserProvider.getCurrentUserId();

        // 3. Verify AI service availability before attempting quota reservation
        if (!searchQueryParser.isAvailable()) {
            throw new AiServiceUnavailableException("AI discovery engine is currently unavailable. Groq API key is unconfigured or disabled.");
        }

        // 4. Atomically reserve quota (throws AiQuotaExceededException if daily limit exceeded)
        AiQuotaResponseDto quotaInfo = aiUsageService.consumeQuota(userId);
        log.info("Reserved AI discovery quota for user {}. Remaining today: {}", userId, quotaInfo.getRemainingToday());

        // 5. Interpret natural language prompt into structured search filters (exactly 1 LLM call)
        ParsedFilterDto parsedFilter = searchQueryParser.parse(
                request.getPrompt(),
                request.getPage(),
                request.getPerPage()
        );

        // 6. Convert to validated GitHub REST API search filter
        GitHubIssueSearchFilter gitHubFilter = validator.toGitHubFilter(parsedFilter);

        // 7. Discover candidate issues from GitHub Public REST API
        GitHubPage<GitHubIssue> gitHubPage = gitHubIssueProvider.searchIssues(gitHubFilter);
        List<GitHubIssue> rawIssues = (gitHubPage != null && gitHubPage.getItems() != null)
                ? gitHubPage.getItems()
                : Collections.emptyList();

        // 8. Filter out unsuitable issues (job ads, study logs, announcements)
        // and exclude repositories whose verified primary language does not match the requested language
        List<GitHubIssue> eligibleIssues = rawIssues.stream()
                .filter(unsuitableIssueFilter::isEligible)
                .filter(issue -> {
                    if (parsedFilter != null && parsedFilter.getLanguage() != null && !parsedFilter.getLanguage().isBlank()) {
                        if (repositoryLanguageService != null) {
                            if (hasLabelMatchingLanguage(issue, parsedFilter.getLanguage())) {
                                return true;
                            }
                            return !repositoryLanguageService.isLanguageMismatch(issue.getRepositoryFullName(), parsedFilter.getLanguage());
                        }
                    }
                    return true;
                })
                .toList();

        // 9. Deterministically rank eligible issues by multi-factor relevance
        List<ScoredIssue> scoredIssues = issueRanker.rankIssues(eligibleIssues, parsedFilter);

        // 10. Generate factual match explanations distinguishing verified facts from inferred criteria
        List<IssueMatchDto> issueMatches = (scoredIssues != null && !scoredIssues.isEmpty())
                ? scoredIssues.stream()
                .map(scored -> explanationGenerator.generateExplanation(scored, parsedFilter))
                .toList()
                : Collections.emptyList();

        // 11. Assemble response payload with calculated accessibleCount and totalPages
        long totalCount = gitHubPage != null ? gitHubPage.getTotalCount() : 0L;
        long accessibleCount = Math.min(Math.max(0L, totalCount), 1000L);
        int perPage = gitHubPage != null ? gitHubPage.getPerPage() : request.getPerPage();
        int totalPages = accessibleCount > 0 ? (int) Math.ceil((double) accessibleCount / perPage) : 0;
        int page = gitHubPage != null ? gitHubPage.getPage() : request.getPage();
        boolean hasPrevious = page > 1;
        boolean hasNext = page < totalPages && (gitHubPage != null && gitHubPage.isHasNext());

        return new DiscoverySearchResponseDto(
                request.getPrompt(),
                parsedFilter,
                issueMatches,
                totalCount,
                accessibleCount,
                totalPages,
                page,
                perPage,
                hasNext,
                hasPrevious,
                quotaInfo.getRemainingToday(),
                gitHubPage != null ? gitHubPage.getRateLimit() : null,
                "AUTHENTICATED_AI"
        );
    }

    @Override
    public DiscoverySearchResponseDto browsePublicIssues(PublicDiscoveryRequestDto request, String clientIp) {
        // 1. Enforce bounded client IP rate limiting
        clientIpRateLimiter.checkRateLimit(clientIp);

        // 2. Validate and sanitize query parameters; requires at least one meaningful filter
        ParsedFilterDto parsedFilter = validator.validateAndSanitizePublic(request);

        // 3. Convert to validated GitHub search filter
        GitHubIssueSearchFilter gitHubFilter = validator.toGitHubFilter(parsedFilter);

        // 4. Query public GitHub REST API without OAuth credentials
        GitHubPage<GitHubIssue> gitHubPage = gitHubIssueProvider.searchIssues(gitHubFilter);
        List<GitHubIssue> rawIssues = (gitHubPage != null && gitHubPage.getItems() != null)
                ? gitHubPage.getItems()
                : Collections.emptyList();

        // 5. Filter out unsuitable issues (job ads, study logs, announcements)
        // and verify repository primary language (never assume GitHub issue search query is authoritative)
        List<GitHubIssue> eligibleIssues = rawIssues.stream()
                .filter(unsuitableIssueFilter::isEligible)
                .filter(issue -> {
                    if (parsedFilter.getLanguage() != null && !parsedFilter.getLanguage().isBlank()) {
                        if (repositoryLanguageService != null) {
                            if (hasLabelMatchingLanguage(issue, parsedFilter.getLanguage())) {
                                return true;
                            }
                            return !repositoryLanguageService.isLanguageMismatch(issue.getRepositoryFullName(), parsedFilter.getLanguage());
                        }
                    }
                    return true;
                })
                .toList();

        // 6. Assemble factual matches with strictly null matchScore (no fabricated AI scores)
        List<IssueMatchDto> factualMatches = eligibleIssues.stream()
                .map(issue -> buildFactualGuestMatch(issue, parsedFilter))
                .toList();

        // 7. Assemble public response payload (remainingDailyQuota is null; discoveryMode is GUEST)
        long totalCount = gitHubPage != null ? gitHubPage.getTotalCount() : 0L;
        long accessibleCount = Math.min(Math.max(0L, totalCount), 1000L);
        int perPage = gitHubPage != null ? gitHubPage.getPerPage() : parsedFilter.getPerPage();
        int totalPages = accessibleCount > 0 ? (int) Math.ceil((double) accessibleCount / perPage) : 0;
        int page = gitHubPage != null ? gitHubPage.getPage() : parsedFilter.getPage();
        boolean hasPrevious = page > 1;
        boolean hasNext = page < totalPages && (gitHubPage != null && gitHubPage.isHasNext());

        return new DiscoverySearchResponseDto(
                null,
                parsedFilter,
                factualMatches,
                totalCount,
                accessibleCount,
                totalPages,
                page,
                perPage,
                hasNext,
                hasPrevious,
                null, // null remaining daily quota for guests
                gitHubPage != null ? gitHubPage.getRateLimit() : null,
                "GUEST"
        );
    }

    private IssueMatchDto buildFactualGuestMatch(GitHubIssue issue, ParsedFilterDto filter) {
        List<String> criteria = new ArrayList<>();

        if (filter.getRepository() != null && !filter.getRepository().isBlank()) {
            criteria.add("Repository: " + issue.getRepositoryFullName());
        }

        if (filter.getLanguage() != null && !filter.getLanguage().isBlank()) {
            if (repositoryLanguageService != null) {
                Optional<String> repoLang = repositoryLanguageService.getRepositoryLanguage(issue.getRepositoryFullName());
                if (repoLang.isPresent()) {
                    criteria.add("Language: " + filter.getLanguage() + " (verified repository primary language: " + repoLang.get() + ")");
                } else if (hasLabelMatchingLanguage(issue, filter.getLanguage())) {
                    criteria.add("Language: " + filter.getLanguage() + " (matched issue label)");
                } else {
                    criteria.add("Language: " + filter.getLanguage() + " (unverified repository language)");
                }
            } else {
                criteria.add("Language: " + filter.getLanguage());
            }
        }

        if (filter.getLabels() != null && !filter.getLabels().isEmpty()) {
            for (String reqLabel : filter.getLabels()) {
                if (issue.getLabels() != null) {
                    for (GitHubLabel actualLabel : issue.getLabels()) {
                        if (actualLabel.getName() != null && actualLabel.getName().equalsIgnoreCase(reqLabel)) {
                            criteria.add("Label: " + actualLabel.getName());
                            break;
                        }
                    }
                }
            }
        }

        if (filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            criteria.add("Keyword match: " + filter.getKeywords());
        }

        String explanation = criteria.isEmpty()
                ? "Matched public issue filter criteria."
                : "Factual match: " + String.join("; ", criteria);

        // Guest matchScore is strictly null (never fabricate AI scores)
        return new IssueMatchDto(issue, explanation, criteria, null);
    }

    private boolean hasLabelMatchingLanguage(GitHubIssue issue, String language) {
        if (issue == null || issue.getLabels() == null || language == null) {
            return false;
        }
        String clean = language.trim().toLowerCase();
        for (GitHubLabel label : issue.getLabels()) {
            if (label != null && label.getName() != null) {
                String labelName = label.getName().trim().toLowerCase();
                if (labelName.equals(clean) || labelName.startsWith(clean + "/") || labelName.endsWith("/" + clean)) {
                    return true;
                }
            }
        }
        return false;
    }
}
