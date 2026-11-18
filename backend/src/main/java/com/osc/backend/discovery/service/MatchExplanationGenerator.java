package com.osc.backend.discovery.service;

import com.osc.backend.discovery.dto.IssueMatchDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.ranking.ScoredIssue;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates transparent, factual explanations of why a GitHub issue matches the user's search criteria.
 *
 * <p>Core Principles:
 * <ul>
 *   <li>Strictly relies on verified GitHub issue and repository metadata.</li>
 *   <li>Distinguishes repository detected primary language from unverified search queries.</li>
 *   <li>Clarifies that repository primary language does not guarantee the issue language in polyglot repositories.</li>
 *   <li>Clearly distinguishes verified GitHub labels from inferred suitability.</li>
 *   <li>Never claims an issue is objectively easy or predicts maintainer responsiveness.</li>
 *   <li>Explicitly clarifies that beginner labels reflect maintainer intent rather than verified task difficulty.</li>
 *   <li>Does not claim the ranking score is an objective measure of contribution success.</li>
 * </ul>
 */
@Component
public class MatchExplanationGenerator {

    private final RepositoryLanguageService repositoryLanguageService;

    public MatchExplanationGenerator() {
        this(null);
    }

    @Autowired
    public MatchExplanationGenerator(@Autowired(required = false) RepositoryLanguageService repositoryLanguageService) {
        this.repositoryLanguageService = repositoryLanguageService;
    }

    /**
     * Generates an IssueMatchDto for a ScoredIssue, integrating computed score factors.
     *
     * @param scored scored issue with breakdown
     * @param filter validated search filter criteria
     * @return IssueMatchDto with verified explanation and score
     */
    public IssueMatchDto generateExplanation(ScoredIssue scored, ParsedFilterDto filter) {
        if (scored == null || scored.getIssue() == null) {
            return null;
        }

        IssueMatchDto matchDto = generateExplanation(scored.getIssue(), filter);
        matchDto.setMatchScore(scored.getTotalScore());

        // Augment matched criteria with scored factors if not already present
        if (scored.getMatchedFactors() != null) {
            for (String factor : scored.getMatchedFactors()) {
                if (factor != null && !factor.isBlank() && !matchDto.getMatchedCriteria().contains(factor)) {
                    matchDto.getMatchedCriteria().add(factor);
                }
            }
        }

        return matchDto;
    }

    /**
     * Generates an IssueMatchDto containing factual explanations and matched criteria list.
     *
     * @param issue  actual GitHub issue returned from provider
     * @param filter validated search filter criteria
     * @return IssueMatchDto with verified explanation
     */
    public IssueMatchDto generateExplanation(GitHubIssue issue, ParsedFilterDto filter) {
        if (issue == null) {
            return null;
        }

        List<String> matchedCriteria = new ArrayList<>();
        List<String> verifiedFacts = new ArrayList<>();
        List<String> inferredRelevance = new ArrayList<>();

        // 1. Language verification: check repository primary language metadata
        if (filter != null && filter.getLanguage() != null && !filter.getLanguage().isBlank()) {
            String reqLang = filter.getLanguage().trim();
            explainLanguageMatch(issue, reqLang, matchedCriteria, verifiedFacts, inferredRelevance);
        }

        // 2. Specific technical concepts & frameworks (e.g. Spring Boot, REST APIs)
        explainTechnicalConcepts(issue, filter, matchedCriteria, verifiedFacts);

        // 3. Label matching (verified against actual GitHub issue labels)
        List<String> matchedLabels = findMatchingLabels(issue, filter != null ? filter.getLabels() : null);
        if (!matchedLabels.isEmpty()) {
            for (String label : matchedLabels) {
                matchedCriteria.add("Verified Label: " + label);
            }
            verifiedFacts.add("Carries requested label(s): " + String.join(", ", matchedLabels));
        }

        // 4. Repository matching (verified against actual repository full name)
        if (filter != null && filter.getRepository() != null && !filter.getRepository().isBlank()) {
            if (issue.getRepositoryFullName() != null &&
                    issue.getRepositoryFullName().equalsIgnoreCase(filter.getRepository().trim())) {
                matchedCriteria.add("Verified Repository: " + issue.getRepositoryFullName());
                verifiedFacts.add("Located in target repository: " + issue.getRepositoryFullName());
            }
        }

        // 5. Keyword matching in title or body
        if (filter != null && filter.getKeywords() != null && !filter.getKeywords().isBlank()) {
            String keywords = filter.getKeywords().trim();
            boolean inTitle = issue.getTitle() != null && containsIgnoreCase(issue.getTitle(), keywords);
            boolean inBody = issue.getBody() != null && containsIgnoreCase(issue.getBody(), keywords);

            if (inTitle && inBody) {
                matchedCriteria.add("Keyword Match: '" + keywords + "' (title and description)");
                verifiedFacts.add("Contains requested search terms in title and description");
            } else if (inTitle) {
                matchedCriteria.add("Keyword Match: '" + keywords + "' (title)");
                verifiedFacts.add("Contains requested search terms in title");
            } else if (inBody) {
                matchedCriteria.add("Keyword Match: '" + keywords + "' (description)");
                verifiedFacts.add("Contains requested search terms in description");
            }
        }

        // 6. Beginner preference & starter labels (clearly distinguishes verified label from inferred difficulty)
        List<String> starterLabels = findStarterLabels(issue);
        boolean hasStarterLabels = !starterLabels.isEmpty();
        boolean requestedBeginner = filter != null && "beginner".equalsIgnoreCase(filter.getDifficultyPreference());

        if (hasStarterLabels) {
            for (String starterLabel : starterLabels) {
                matchedCriteria.add("Verified Community Label: " + starterLabel);
            }
            if (requestedBeginner) {
                matchedCriteria.add("Inferred Preference: beginner difficulty");
                inferredRelevance.add("Matches beginner preference via maintainer-assigned label(s) '" +
                        String.join(", ", starterLabels) + "'. Note: Community labels indicate the intended audience set by maintainers, not an objective guarantee of task simplicity");
            } else {
                verifiedFacts.add("Carries community starter label(s): " + String.join(", ", starterLabels));
            }
        } else if (requestedBeginner) {
            matchedCriteria.add("User Preference: beginner (no starter label present on issue)");
            inferredRelevance.add("User requested beginner difficulty, but this issue does not carry a recognized beginner-friendly label");
        } else if (filter != null && filter.getDifficultyPreference() != null && !filter.getDifficultyPreference().isBlank()) {
            String diff = filter.getDifficultyPreference().trim();
            matchedCriteria.add("User Preference: " + diff);
            inferredRelevance.add("Matched against user difficulty preference '" + diff + "' (note: difficulty is a user preference and not a verified GitHub metric)");
        }

        // 7. Factual status, activity, and freshness
        String state = issue.getState() != null ? issue.getState() : "open";
        int comments = issue.getCommentsCount() != null ? issue.getCommentsCount() : 0;
        verifiedFacts.add("Issue is currently " + state + " with " + comments + " comment(s)");

        if (issue.getUpdatedAt() != null) {
            long daysOld = Duration.between(issue.getUpdatedAt(), Instant.now()).toDays();
            if (daysOld >= 0 && daysOld <= 30) {
                matchedCriteria.add("Freshness: updated " + daysOld + "d ago");
            }
        }

        // Assemble structured explanation
        StringBuilder sb = new StringBuilder();
        if (!verifiedFacts.isEmpty()) {
            sb.append("Verified GitHub Metadata: ").append(String.join(". ", verifiedFacts)).append(".");
        }
        if (!inferredRelevance.isEmpty()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("Inferred Query Relevance: ").append(String.join(". ", inferredRelevance)).append(".");
        }

        return new IssueMatchDto(issue, sb.toString(), matchedCriteria);
    }

    private void explainLanguageMatch(GitHubIssue issue, String reqLang,
                                      List<String> matchedCriteria,
                                      List<String> verifiedFacts,
                                      List<String> inferredRelevance) {
        if (repositoryLanguageService != null && issue.getRepositoryFullName() != null) {
            Optional<String> repoLangOpt = repositoryLanguageService.getRepositoryLanguage(issue.getRepositoryFullName());
            if (repoLangOpt.isPresent()) {
                String repoLang = repoLangOpt.get();
                if (repositoryLanguageService.isLanguageMatching(issue.getRepositoryFullName(), reqLang)) {
                    matchedCriteria.add("Verified Repository Primary Language: " + repoLang);
                    verifiedFacts.add("Matches requested language: " + reqLang);
                    verifiedFacts.add("Repository primary language is verified as " + repoLang +
                            " (note: individual issue language may vary in multi-language projects)");
                    return;
                } else {
                    matchedCriteria.add("Repository Primary Language: " + repoLang);
                    inferredRelevance.add("Repository primary language is " + repoLang + " (different from requested " + reqLang + ")");
                    return;
                }
            }
        }

        // If repository language could not be verified from repository metadata
        matchedCriteria.add("Language Filter: " + reqLang);
        inferredRelevance.add("Targeted via requested language filter '" + reqLang + "'; repository primary language is unverified on GitHub");

        if (hasLabelContaining(issue, reqLang)) {
            verifiedFacts.add("Carries issue label mentioning " + reqLang);
        }
    }

    private void explainTechnicalConcepts(GitHubIssue issue, ParsedFilterDto filter,
                                          List<String> matchedCriteria,
                                          List<String> verifiedFacts) {
        if (filter == null || filter.getKeywords() == null || filter.getKeywords().isBlank()) {
            return;
        }

        String keywords = filter.getKeywords().toLowerCase();
        String title = issue.getTitle() != null ? issue.getTitle().toLowerCase() : "";
        String body = issue.getBody() != null ? issue.getBody().toLowerCase() : "";

        if (keywords.contains("spring boot") || keywords.contains("spring-boot")) {
            if (title.contains("spring boot") || title.contains("spring-boot") ||
                    body.contains("spring boot") || body.contains("spring-boot")) {
                matchedCriteria.add("Technical Match: Spring Boot");
                verifiedFacts.add("Matches requested framework: Spring Boot");
            }
        }

        if (keywords.contains("rest") || keywords.contains("rest api") || keywords.contains("restful")) {
            if (title.contains("rest") || body.contains("rest")) {
                matchedCriteria.add("Technical Match: REST APIs");
                verifiedFacts.add("Matches requested technical concept: REST APIs");
            }
        }
    }

    private List<String> findMatchingLabels(GitHubIssue issue, List<String> requestedLabels) {
        List<String> matched = new ArrayList<>();
        if (issue.getLabels() == null || requestedLabels == null || requestedLabels.isEmpty()) {
            return matched;
        }

        for (String req : requestedLabels) {
            for (GitHubLabel actual : issue.getLabels()) {
                if (actual.getName() != null && actual.getName().equalsIgnoreCase(req)) {
                    if (!matched.contains(actual.getName())) {
                        matched.add(actual.getName());
                    }
                }
            }
        }
        return matched;
    }

    private List<String> findStarterLabels(GitHubIssue issue) {
        List<String> found = new ArrayList<>();
        if (issue.getLabels() == null) {
            return found;
        }
        for (GitHubLabel label : issue.getLabels()) {
            if (label.getName() != null) {
                String name = label.getName().toLowerCase().replace("-", " ").trim();
                if (name.contains("good first issue") ||
                        name.contains("first timers only") ||
                        name.contains("beginner") ||
                        name.contains("starter") ||
                        name.contains("up for grabs")) {
                    if (!found.contains(label.getName())) {
                        found.add(label.getName());
                    }
                }
            }
        }
        return found;
    }

    private boolean hasLabelContaining(GitHubIssue issue, String term) {
        if (issue.getLabels() == null || term == null) return false;
        String lower = term.toLowerCase();
        for (GitHubLabel label : issue.getLabels()) {
            if (label.getName() != null && label.getName().toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(String text, String search) {
        if (text == null || search == null) {
            return false;
        }
        return text.toLowerCase().contains(search.toLowerCase());
    }
}
