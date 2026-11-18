package com.osc.backend.discovery.ranking;

import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.service.RepositoryLanguageService;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Production-quality issue ranker implementing multi-factor relevance scoring.
 *
 * <p>Scoring Weights Breakdown:
 * <ul>
 *   <li>Exact Repository Match: up to 30.0 pts</li>
 *   <li>Requested Labels Match: up to 40.0 pts (20 pts per exact label)</li>
 *   <li>Beginner-Friendly Preference: up to 30.0 pts (25 pts for primary starter label)</li>
 *   <li>Verified Repository Language Match: up to 35.0 pts (penalty -50.0 pts on proven mismatch)</li>
 *   <li>Keywords & Technical Concepts (Title & Body): up to 35.0 pts (specific technologies weighted over dampened generic terms)</li>
 *   <li>State Match (Open): 10.0 pts</li>
 *   <li>Freshness (Secondary Factor): up to 5.0 pts (strictly bounded decay)</li>
 *   <li>Domain Conflict Penalty: -30.0 pts (penalizes frontend-only tasks when backend work was requested)</li>
 * </ul>
 *
 * <p>Guarantees 100% deterministic ordering using multi-tier tie-breakers (score -> updated -> ID -> number).
 */
@Component
public class DefaultIssueRanker implements IssueRanker {

    public static final double MAX_REPO_SCORE = 30.0;
    public static final double MAX_LABEL_SCORE = 40.0;
    public static final double MAX_BEGINNER_SCORE = 30.0;
    public static final double MAX_LANGUAGE_SCORE = 35.0;
    public static final double MAX_KEYWORD_SCORE = 35.0;
    public static final double MAX_STATE_SCORE = 10.0;
    public static final double MAX_FRESHNESS_SCORE = 5.0;
    public static final double FRONTEND_CONFLICT_PENALTY = -30.0;

    private static final Set<String> RECOGNIZED_BEGINNER_LABELS = Set.of(
            "good first issue",
            "good-first-issue",
            "first-timers-only",
            "first timer only",
            "beginner",
            "beginner-friendly",
            "easy",
            "starter",
            "up-for-grabs",
            "newbie",
            "help wanted",
            "help-wanted"
    );

    // High-value specific framework and technology phrases (multi-word)
    private static final Set<String> SPECIFIC_TECH_PHRASES = Set.of(
            "spring boot",
            "spring-boot",
            "rest api",
            "rest apis",
            "restful api",
            "restful apis",
            "react native",
            "next js",
            "vue js"
    );

    // High-value specific framework and technology single terms
    private static final Set<String> SPECIFIC_TECH_TERMS = Set.of(
            "spring",
            "rest",
            "restful",
            "hibernate",
            "jpa",
            "maven",
            "gradle",
            "kafka",
            "redis",
            "docker",
            "kubernetes",
            "graphql",
            "postgres",
            "postgresql",
            "mysql",
            "mongodb",
            "microservices",
            "oauth",
            "jwt"
    );

    // Generic keywords that must be dampened so they do not dominate scores
    private static final Set<String> GENERIC_KEYWORDS = Set.of(
            "backend",
            "back-end",
            "api",
            "apis",
            "development",
            "dev",
            "app",
            "application",
            "service",
            "services",
            "system",
            "code",
            "tool",
            "tools",
            "work",
            "task",
            "issue"
    );

    // Frontend-specific terms for domain mismatch detection
    private static final Set<String> FRONTEND_TERMS = Set.of(
            "react",
            "vue",
            "angular",
            "svelte",
            "tailwind",
            "css",
            "scss",
            "html",
            "ui",
            "ux",
            "frontend",
            "front-end",
            "styling",
            "component",
            "components",
            "button",
            "dropdown",
            "modal",
            "dom",
            "jsx",
            "tsx"
    );

    private final RepositoryLanguageService repositoryLanguageService;

    public DefaultIssueRanker() {
        this(null);
    }

    @Autowired
    public DefaultIssueRanker(@Autowired(required = false) RepositoryLanguageService repositoryLanguageService) {
        this.repositoryLanguageService = repositoryLanguageService;
    }

    @Override
    public List<ScoredIssue> rankIssues(List<GitHubIssue> issues, ParsedFilterDto filter) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScoredIssue> scoredList = new ArrayList<>(issues.size());
        for (GitHubIssue issue : issues) {
            if (issue != null) {
                scoredList.add(scoreIssue(issue, filter));
            }
        }

        // Deterministic sorting via ScoredIssue.compareTo
        Collections.sort(scoredList);
        return scoredList;
    }

    @Override
    public ScoredIssue scoreIssue(GitHubIssue issue, ParsedFilterDto filter) {
        if (issue == null) {
            return new ScoredIssue(null, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Collections.emptyList());
        }

        List<String> matchedFactors = new ArrayList<>();

        // 1. Repository Match (up to 30.0 pts)
        double repoScore = scoreRepository(issue, filter, matchedFactors);

        // 2. Labels Match (up to 40.0 pts)
        double labelScore = scoreLabels(issue, filter, matchedFactors);

        // 3. Beginner Preference Match (up to 30.0 pts)
        double beginnerScore = scoreBeginnerPreference(issue, filter, matchedFactors);

        // 4. Verified Language Match (up to 35.0 pts; -50.0 pts for verified mismatch)
        double langScore = scoreLanguage(issue, filter, matchedFactors);

        // 5. Keyword & Technical Concept Match in Title & Body (up to 35.0 pts, generic dampened)
        double keywordScore = scoreKeywords(issue, filter, matchedFactors);

        // 6. State Match (10.0 pts)
        double stateScore = scoreState(issue, filter, matchedFactors);

        // 7. Freshness as Secondary Factor (up to 5.0 pts)
        double freshnessScore = scoreFreshness(issue, matchedFactors);

        // 8. Domain Conflict Penalty (penalize frontend-only tasks when backend requested)
        double domainPenalty = scoreDomainConflict(issue, filter, matchedFactors);

        double totalScore = repoScore + labelScore + beginnerScore + langScore + keywordScore + stateScore + freshnessScore + domainPenalty;

        return new ScoredIssue(
                issue,
                totalScore,
                langScore,
                repoScore,
                labelScore,
                keywordScore,
                beginnerScore,
                stateScore,
                freshnessScore,
                matchedFactors
        );
    }

    private double scoreRepository(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (filter == null || filter.getRepository() == null || filter.getRepository().isBlank()) {
            return 0.0;
        }

        String targetRepo = filter.getRepository().trim().toLowerCase();
        String actualRepo = issue.getRepositoryFullName() != null ? issue.getRepositoryFullName().trim().toLowerCase() : null;

        if (actualRepo == null) {
            return 0.0;
        }

        if (actualRepo.equals(targetRepo)) {
            factors.add("Exact repository match: " + issue.getRepositoryFullName());
            return MAX_REPO_SCORE;
        }

        if (actualRepo.endsWith("/" + targetRepo) || targetRepo.endsWith("/" + actualRepo)) {
            factors.add("Partial repository match: " + issue.getRepositoryFullName());
            return 20.0;
        }

        return 0.0;
    }

    private double scoreLabels(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (filter == null || filter.getLabels() == null || filter.getLabels().isEmpty()) {
            return 0.0;
        }
        if (issue.getLabels() == null || issue.getLabels().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        List<String> matchedLabels = new ArrayList<>();

        for (String reqLabel : filter.getLabels()) {
            if (reqLabel == null || reqLabel.isBlank()) continue;
            String reqNorm = normalize(reqLabel);

            for (GitHubLabel actual : issue.getLabels()) {
                if (actual.getName() == null) continue;
                String actNorm = normalize(actual.getName());

                if (actual.getName().equalsIgnoreCase(reqLabel.trim())) {
                    score += 20.0;
                    matchedLabels.add(actual.getName());
                    break;
                } else if (actNorm.equals(reqNorm)) {
                    score += 15.0;
                    matchedLabels.add(actual.getName());
                    break;
                }
            }
        }

        if (!matchedLabels.isEmpty()) {
            factors.add("Matched requested label(s): " + String.join(", ", matchedLabels));
        }

        return Math.min(score, MAX_LABEL_SCORE);
    }

    private double scoreBeginnerPreference(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (issue.getLabels() == null || issue.getLabels().isEmpty()) {
            return 0.0;
        }

        List<String> beginnerLabels = new ArrayList<>();
        for (GitHubLabel label : issue.getLabels()) {
            if (label.getName() == null) continue;
            String norm = normalize(label.getName());
            for (String recognized : RECOGNIZED_BEGINNER_LABELS) {
                if (norm.equals(normalize(recognized)) || norm.contains(normalize(recognized))) {
                    if (!beginnerLabels.contains(label.getName())) {
                        beginnerLabels.add(label.getName());
                    }
                    break;
                }
            }
        }

        if (beginnerLabels.isEmpty()) {
            return 0.0;
        }

        boolean requestedBeginner = filter != null && (
                "beginner".equalsIgnoreCase(filter.getDifficultyPreference()) ||
                        (filter.getLabels() != null && filter.getLabels().stream().anyMatch(this::isBeginnerLabelString))
        );

        if (requestedBeginner) {
            double score = 25.0 + (beginnerLabels.size() > 1 ? 5.0 : 0.0);
            factors.add("Carries community beginner label(s): " + String.join(", ", beginnerLabels) +
                    " (matches beginner preference)");
            return Math.min(score, MAX_BEGINNER_SCORE);
        } else {
            factors.add("Carries community starter label: " + beginnerLabels.get(0));
            return 5.0;
        }
    }

    private double scoreLanguage(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (filter == null || filter.getLanguage() == null || filter.getLanguage().isBlank()) {
            return 0.0;
        }

        String reqLang = filter.getLanguage().trim();

        // Check verified repository metadata if RepositoryLanguageService is provided
        if (repositoryLanguageService != null && issue.getRepositoryFullName() != null) {
            Optional<String> repoLangOpt = repositoryLanguageService.getRepositoryLanguage(issue.getRepositoryFullName());
            if (repoLangOpt.isPresent()) {
                String repoLang = repoLangOpt.get();
                if (repositoryLanguageService.isLanguageMatching(issue.getRepositoryFullName(), reqLang)) {
                    factors.add("Verified GitHub repository primary language: " + repoLang);
                    return MAX_LANGUAGE_SCORE;
                } else {
                    factors.add("Repository primary language mismatch (verified as " + repoLang +
                            ", requested " + reqLang + ") [-50.0 pts]");
                    return -50.0;
                }
            } else {
                // Repository primary language is unverified on GitHub
                if (hasLabelMatchingLanguage(issue, reqLang)) {
                    factors.add("Issue label mentions " + reqLang + " (repository primary language unverified)");
                    return 10.0;
                }
                if (issue.getTitle() != null && issue.getTitle().toLowerCase().contains(reqLang.toLowerCase())) {
                    factors.add("Language mentioned in issue title (repository primary language unverified)");
                    return 5.0;
                }
                factors.add("Targeted via requested language filter (repository primary language unverified)");
                return 0.0;
            }
        }

        // Fallback scoring when repository language service is unavailable
        if (hasLabelMatchingLanguage(issue, reqLang)) {
            factors.add("Language confirmed by issue label");
            return 25.0;
        }
        if (issue.getRepositoryFullName() != null && issue.getRepositoryFullName().toLowerCase().contains(reqLang.toLowerCase())) {
            factors.add("Language identified in repository name: " + issue.getRepositoryFullName());
            return 20.0;
        }
        if (issue.getTitle() != null && issue.getTitle().toLowerCase().contains(reqLang.toLowerCase())) {
            factors.add("Language mentioned in issue title");
            return 15.0;
        }

        factors.add("Targeted via requested language query: " + reqLang);
        return 10.0;
    }

    private double scoreKeywords(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (filter == null || filter.getKeywords() == null || filter.getKeywords().isBlank()) {
            return 0.0;
        }

        String keywordsRaw = filter.getKeywords().trim().toLowerCase();
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase() : "";
        String body = issue.getBody() != null ? issue.getBody().toLowerCase() : "";

        double score = 0.0;

        // 1. Exact full keyword phrase match in title
        if (!title.isEmpty() && title.contains(keywordsRaw)) {
            factors.add("Exact keywords phrase match in title");
            return 25.0;
        }

        // 2. High-value specific framework & technology matching (e.g. "Spring Boot", "REST APIs")
        List<String> matchedTech = new ArrayList<>();
        double techScore = 0.0;

        for (String phrase : SPECIFIC_TECH_PHRASES) {
            if (keywordsRaw.contains(phrase)) {
                if (title.contains(phrase)) {
                    techScore += 18.0;
                    matchedTech.add(phrase + " (in title)");
                } else if (body.contains(phrase)) {
                    techScore += 6.0;
                    matchedTech.add(phrase + " (in description)");
                }
            }
        }

        for (String term : SPECIFIC_TECH_TERMS) {
            if (keywordsRaw.contains(term) && !matchedTech.stream().anyMatch(m -> m.contains(term))) {
                if (containsWord(title, term)) {
                    techScore += 12.0;
                    matchedTech.add(term + " (in title)");
                } else if (containsWord(body, term)) {
                    techScore += 4.0;
                    matchedTech.add(term + " (in description)");
                }
            }
        }

        if (!matchedTech.isEmpty()) {
            factors.add("Matches requested technical concept(s): " + String.join(", ", matchedTech));
            score += Math.min(techScore, 30.0);
        }

        // 3. Generic keyword scoring with strict dampening (backend, api, development)
        double genericTitleScore = 0.0;
        double genericBodyScore = 0.0;
        int genericCount = 0;

        String[] terms = keywordsRaw.split("\\s+");
        for (String term : terms) {
            if (term.length() <= 2) continue;

            if (GENERIC_KEYWORDS.contains(term)) {
                if (containsWord(title, term)) {
                    genericTitleScore += 2.0;
                    genericCount++;
                } else if (containsWord(body, term)) {
                    genericBodyScore += 1.0;
                }
            } else if (!SPECIFIC_TECH_TERMS.contains(term)) {
                // Non-generic custom keyword
                if (containsWord(title, term)) {
                    score += 5.0;
                } else if (containsWord(body, term)) {
                    score += 2.0;
                }
            }
        }

        // Generic terms are capped at 4.0 pts in title and 2.0 pts in body to prevent dominating scores
        double genericScore = Math.min(genericTitleScore, 4.0) + Math.min(genericBodyScore, 2.0);
        score += genericScore;

        if (genericCount > 0 && matchedTech.isEmpty()) {
            factors.add("Matched general technical keywords (dampened relevance: " + genericCount + " term(s))");
        }

        return Math.min(score, MAX_KEYWORD_SCORE);
    }

    private double scoreDomainConflict(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        if (filter == null || filter.getKeywords() == null || filter.getKeywords().isBlank()) {
            return 0.0;
        }

        String keywords = filter.getKeywords().toLowerCase();
        boolean backendRequested = isBackendRequested(keywords);
        boolean frontendRequested = isFrontendRequested(keywords);

        // If backend was requested but frontend was NOT requested
        if (backendRequested && !frontendRequested) {
            if (isFrontendFocusedIssue(issue)) {
                factors.add("Frontend-focused task penalized for backend search (-30.0 pts)");
                return FRONTEND_CONFLICT_PENALTY;
            }
        }

        return 0.0;
    }

    private boolean isBackendRequested(String keywords) {
        return keywords.contains("backend") || keywords.contains("back-end") ||
                keywords.contains("spring") || keywords.contains("database") ||
                keywords.contains("server") || keywords.contains("controller") ||
                keywords.contains("jpa") || keywords.contains("sql") ||
                keywords.contains("rest");
    }

    private boolean isFrontendRequested(String keywords) {
        return keywords.contains("frontend") || keywords.contains("front-end") ||
                keywords.contains("react") || keywords.contains("vue") ||
                keywords.contains("angular") || keywords.contains("ui") ||
                keywords.contains("css") || keywords.contains("tailwind") ||
                keywords.contains("fullstack");
    }

    private boolean isFrontendFocusedIssue(GitHubIssue issue) {
        // Check labels for frontend tags
        if (issue.getLabels() != null) {
            for (GitHubLabel label : issue.getLabels()) {
                if (label.getName() != null) {
                    String norm = label.getName().toLowerCase();
                    if (norm.equals("frontend") || norm.equals("front-end") ||
                            norm.equals("ui") || norm.equals("css") ||
                            norm.equals("react") || norm.equals("styling")) {
                        return true;
                    }
                }
            }
        }

        // Check title for frontend terms without backend counter-balance
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase() : "";
        boolean hasFrontendTerm = FRONTEND_TERMS.stream().anyMatch(term -> containsWord(title, term));
        boolean hasBackendTerm = isBackendRequested(title);

        return hasFrontendTerm && !hasBackendTerm;
    }

    private double scoreState(GitHubIssue issue, ParsedFilterDto filter, List<String> factors) {
        String reqState = (filter != null && filter.getState() != null) ? filter.getState().trim().toLowerCase() : "open";
        String actualState = issue.getState() != null ? issue.getState().trim().toLowerCase() : "open";

        if (actualState.equals(reqState)) {
            factors.add("State matches: " + actualState);
            return MAX_STATE_SCORE;
        }
        return 0.0;
    }

    private double scoreFreshness(GitHubIssue issue, List<String> factors) {
        Instant timestamp = issue.getUpdatedAt() != null ? issue.getUpdatedAt() : issue.getCreatedAt();
        if (timestamp == null) {
            return 0.0;
        }

        long daysOld = Duration.between(timestamp, Instant.now()).toDays();
        if (daysOld < 0) {
            daysOld = 0;
        }

        double score;
        if (daysOld <= 7) {
            score = 5.0;
            factors.add("Freshly updated (" + daysOld + "d ago)");
        } else if (daysOld <= 30) {
            score = 4.0;
            factors.add("Recently updated (" + daysOld + "d ago)");
        } else if (daysOld <= 90) {
            score = 3.0;
        } else if (daysOld <= 180) {
            score = 2.0;
        } else if (daysOld <= 365) {
            score = 1.0;
        } else {
            score = 0.0;
        }

        return Math.min(score, MAX_FRESHNESS_SCORE);
    }

    private boolean hasLabelMatchingLanguage(GitHubIssue issue, String language) {
        if (issue.getLabels() == null || language == null) return false;
        String lowerLang = language.toLowerCase();
        for (GitHubLabel label : issue.getLabels()) {
            if (label.getName() != null && label.getName().toLowerCase().contains(lowerLang)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String source, String word) {
        if (source == null || word == null || source.isEmpty() || word.isEmpty()) {
            return false;
        }
        return source.matches("(?i).*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*");
    }

    private boolean isBeginnerLabelString(String label) {
        if (label == null) return false;
        String norm = normalize(label);
        for (String recognized : RECOGNIZED_BEGINNER_LABELS) {
            if (norm.equals(normalize(recognized))) return true;
        }
        return false;
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase().replaceAll("[\\-_\\s]+", " ");
    }
}
