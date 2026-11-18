package com.osc.backend.discovery.validator;

import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.discovery.dto.AiSearchCriteriaJson;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.dto.PublicDiscoveryRequestDto;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes AI-generated search criteria, user prompts, and public guest discovery parameters.
 * Strictly prevents query injection, restricts pagination, and enforces GitHub search boundaries.
 */
@Component
public class SearchFilterValidator {

    public static final int MIN_PROMPT_LENGTH = 3;
    public static final int MAX_PROMPT_LENGTH = 500;
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 30;
    public static final int MAX_PER_PAGE = 100;
    public static final int MAX_LABELS = 5;
    public static final int GITHUB_MAX_SEARCH_RESULTS = 1000;

    private static final Pattern REPO_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+/[a-zA-Z0-9_.-]+$");
    private static final Pattern SAFE_LANGUAGE_PATTERN = Pattern.compile("^[a-zA-Z0-9#+.-]{1,30}$");
    private static final Pattern SAFE_LABEL_PATTERN = Pattern.compile("^[a-zA-Z0-9_ .\\-]{1,50}$");
    private static final Pattern INJECTION_CHARS_PATTERN = Pattern.compile("[\n\r\t:]");

    private static final Set<String> ALLOWED_SORTS = Set.of("comments", "created", "updated", "reactions");
    private static final Set<String> ALLOWED_ORDERS = Set.of("asc", "desc");
    private static final Set<String> ALLOWED_STATES = Set.of("open", "closed");
    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("beginner", "intermediate", "advanced");

    /**
     * Validates user search prompt for AI discovery.
     *
     * @param prompt user prompt string
     * @throws InvalidRequestException if blank or outside length boundaries
     */
    public void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new InvalidRequestException("Search prompt must not be blank");
        }
        String trimmed = prompt.trim();
        if (trimmed.length() < MIN_PROMPT_LENGTH || trimmed.length() > MAX_PROMPT_LENGTH) {
            throw new InvalidRequestException(
                    "Search prompt must be between " + MIN_PROMPT_LENGTH + " and " + MAX_PROMPT_LENGTH + " characters"
            );
        }
    }

    /**
     * Validates that requested pagination parameters do not exceed the GitHub Search API accessible window.
     *
     * @param page page number (1-based)
     * @param perPage page size
     * @throws InvalidRequestException if page is out of bounds or exceeds the 1,000 result limit
     */
    public void validatePagination(Integer page, Integer perPage) {
        int validPage = page != null ? page : DEFAULT_PAGE;
        int validPerPage = perPage != null ? perPage : DEFAULT_PER_PAGE;

        if (validPage < 1) {
            throw new InvalidRequestException("Page number must be at least 1");
        }
        if (validPerPage < 1 || validPerPage > MAX_PER_PAGE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_PER_PAGE);
        }

        if ((long) (validPage - 1) * validPerPage >= GITHUB_MAX_SEARCH_RESULTS) {
            int maxPage = (int) Math.ceil((double) GITHUB_MAX_SEARCH_RESULTS / validPerPage);
            throw new InvalidRequestException(
                    "Page request (" + validPage + ") exceeds the maximum accessible GitHub Search window of "
                            + GITHUB_MAX_SEARCH_RESULTS + " results. Maximum accessible page for page size "
                            + validPerPage + " is " + maxPage + "."
            );
        }
    }

    /**
     * Validates and sanitizes raw criteria extracted by AI into a safe ParsedFilterDto.
     */
    public ParsedFilterDto validateAndSanitize(AiSearchCriteriaJson raw, int page, int perPage) {
        int validPage = Math.max(page, DEFAULT_PAGE);
        int validPerPage = perPage < 1 ? DEFAULT_PER_PAGE : Math.min(perPage, MAX_PER_PAGE);
        validatePagination(validPage, validPerPage);

        if (raw == null) {
            return new ParsedFilterDto(null, null, null, List.of(), "open", null, null, null, validPage, validPerPage);
        }

        // 1. Sanitize Keywords
        String safeKeywords = sanitizeKeywords(raw.getKeywords());

        // 2. Validate and Sanitize Language
        String safeLanguage = sanitizeLanguage(raw.getLanguage());

        // 3. Validate Repository (owner/name)
        String safeRepo = sanitizeRepository(raw.getRepository());

        // 4. Validate and Sanitize Labels
        List<String> safeLabels = sanitizeLabels(raw.getLabels());

        // 5. Validate State
        String safeState = "open";
        if (raw.getState() != null && ALLOWED_STATES.contains(raw.getState().trim().toLowerCase())) {
            safeState = raw.getState().trim().toLowerCase();
        }

        // 6. Validate Sort & Order
        String safeSort = null;
        if (raw.getSort() != null && ALLOWED_SORTS.contains(raw.getSort().trim().toLowerCase())) {
            safeSort = raw.getSort().trim().toLowerCase();
        }

        String safeOrder = null;
        if (raw.getOrder() != null && ALLOWED_ORDERS.contains(raw.getOrder().trim().toLowerCase())) {
            safeOrder = raw.getOrder().trim().toLowerCase();
        }

        // 7. Validate Difficulty
        String safeDifficulty = null;
        if (raw.getDifficulty() != null && ALLOWED_DIFFICULTIES.contains(raw.getDifficulty().trim().toLowerCase())) {
            safeDifficulty = raw.getDifficulty().trim().toLowerCase();
        }

        // If user expressed beginner difficulty preference, ensure "good first issue" is in labels if not already
        if ("beginner".equals(safeDifficulty) && !containsIgnoreCase(safeLabels, "good first issue") && safeLabels.size() < MAX_LABELS) {
            safeLabels.add("good first issue");
        }

        return new ParsedFilterDto(
                safeKeywords,
                safeLanguage,
                safeRepo,
                safeLabels,
                safeState,
                safeSort,
                safeOrder,
                safeDifficulty,
                validPage,
                validPerPage
        );
    }

    /**
     * Validates and sanitizes public unauthenticated discovery query parameters.
     * Enforces that at least one meaningful filter is provided.
     */
    public ParsedFilterDto validateAndSanitizePublic(PublicDiscoveryRequestDto raw) {
        if (raw == null || !raw.hasAtLeastOneFilter()) {
            throw new InvalidRequestException("At least one search filter (query, language, repository, or labels) must be provided");
        }

        int validPage = Math.max(raw.getPage() != null ? raw.getPage() : DEFAULT_PAGE, DEFAULT_PAGE);
        int rawPerPage = raw.getPerPage() != null ? raw.getPerPage() : DEFAULT_PER_PAGE;
        int validPerPage = rawPerPage < 1 ? DEFAULT_PER_PAGE : Math.min(rawPerPage, MAX_PER_PAGE);
        validatePagination(validPage, validPerPage);

        String safeKeywords = sanitizeKeywords(raw.getQuery());
        String safeLanguage = sanitizeLanguage(raw.getLanguage());
        String safeRepo = sanitizeRepository(raw.getRepository());
        List<String> safeLabels = sanitizeLabels(raw.getLabels());

        // Ensure at least one filter remains valid after sanitization
        if (safeKeywords == null && safeLanguage == null && safeRepo == null && safeLabels.isEmpty()) {
            throw new InvalidRequestException("No valid search filter criteria remaining after sanitization");
        }

        String safeState = "open";
        if (raw.getState() != null && ALLOWED_STATES.contains(raw.getState().trim().toLowerCase())) {
            safeState = raw.getState().trim().toLowerCase();
        }

        String safeSort = "updated";
        if (raw.getSort() != null && ALLOWED_SORTS.contains(raw.getSort().trim().toLowerCase())) {
            safeSort = raw.getSort().trim().toLowerCase();
        }

        String safeOrder = "desc";
        if (raw.getOrder() != null && ALLOWED_ORDERS.contains(raw.getOrder().trim().toLowerCase())) {
            safeOrder = raw.getOrder().trim().toLowerCase();
        }

        return new ParsedFilterDto(
                safeKeywords,
                safeLanguage,
                safeRepo,
                safeLabels,
                safeState,
                safeSort,
                safeOrder,
                null, // No AI difficulty inference in guest mode
                validPage,
                validPerPage
        );
    }

    /**
     * Converts a validated ParsedFilterDto to a GitHubIssueSearchFilter for provider invocation.
     */
    public GitHubIssueSearchFilter toGitHubFilter(ParsedFilterDto parsed) {
        GitHubIssueSearchFilter.Builder builder = GitHubIssueSearchFilter.builder()
                .page(parsed.getPage())
                .perPage(parsed.getPerPage())
                .state(parsed.getState());

        if (parsed.getKeywords() != null) {
            builder.query(parsed.getKeywords());
        }
        if (parsed.getLanguage() != null) {
            builder.language(parsed.getLanguage());
        }
        if (parsed.getRepository() != null) {
            builder.repository(parsed.getRepository());
        }
        if (parsed.getLabels() != null && !parsed.getLabels().isEmpty()) {
            builder.labels(parsed.getLabels());
        }
        if (parsed.getSort() != null) {
            builder.sort(parsed.getSort());
        }
        if (parsed.getOrder() != null) {
            builder.order(parsed.getOrder());
        }

        return builder.build();
    }

    public String sanitizeKeywords(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        // Strip control characters, quotes, and colons that could inject GitHub qualifiers
        String cleaned = INJECTION_CHARS_PATTERN.matcher(raw).replaceAll(" ")
                .replace("\"", "")
                .replace("'", "")
                .trim();
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    public String sanitizeLanguage(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if (SAFE_LANGUAGE_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        return null;
    }

    public String sanitizeRepository(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String trimmed = raw.trim();
        if (REPO_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }
        return null;
    }

    public List<String> sanitizeLabels(List<String> rawLabels) {
        List<String> cleaned = new ArrayList<>();
        if (rawLabels == null) {
            return cleaned;
        }
        for (String label : rawLabels) {
            if (label != null && !label.trim().isEmpty()) {
                String trimmed = label.trim();
                // Strip colons, quotes
                trimmed = trimmed.replace(":", "").replace("\"", "").replace("'", "").trim();
                if (SAFE_LABEL_PATTERN.matcher(trimmed).matches()) {
                    if (!containsIgnoreCase(cleaned, trimmed) && cleaned.size() < MAX_LABELS) {
                        cleaned.add(trimmed);
                    }
                }
            }
        }
        return cleaned;
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        return list.stream().anyMatch(item -> item.equalsIgnoreCase(value));
    }
}
