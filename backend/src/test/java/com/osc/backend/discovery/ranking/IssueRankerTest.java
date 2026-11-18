package com.osc.backend.discovery.ranking;

import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.service.RepositoryLanguageService;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("IssueRanker Unit Tests")
class IssueRankerTest {

    private IssueRanker ranker;

    @BeforeEach
    void setUp() {
        ranker = new DefaultIssueRanker();
    }

    @Test
    @DisplayName("Exact language match ranks higher than non-matching language")
    void rankIssues_ExactLanguageMatch_ScoresHigher() {
        GitHubIssue issueJava = createIssue(1L, 101, "Fix memory leak", "Body text", "owner/java-app", "open",
                List.of(new GitHubLabel(1L, "java", "blue", "Java")), Instant.now());

        GitHubIssue issueOther = createIssue(2L, 102, "Fix memory leak", "Body text", "owner/python-app", "open",
                List.of(new GitHubLabel(2L, "python", "yellow", "Python")), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");
        filter.setKeywords("memory leak");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(issueOther, issueJava), filter);

        assertEquals(2, ranked.size());
        assertEquals(issueJava.getId(), ranked.get(0).getIssue().getId());
        assertTrue(ranked.get(0).getTotalScore() > ranked.get(1).getTotalScore());
        assertTrue(ranked.get(0).getLanguageScore() > ranked.get(1).getLanguageScore());
    }

    @Test
    @DisplayName("Exact repository match ranks higher than another repository")
    void rankIssues_ExactRepositoryMatch_ScoresHigher() {
        GitHubIssue issueTargetRepo = createIssue(1L, 10, "Improve docs", "Details", "facebook/react", "open",
                List.of(), Instant.now());

        GitHubIssue issueOtherRepo = createIssue(2L, 20, "Improve docs", "Details", "google/guava", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setRepository("facebook/react");
        filter.setKeywords("docs");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(issueOtherRepo, issueTargetRepo), filter);

        assertEquals(2, ranked.size());
        assertEquals("facebook/react", ranked.get(0).getIssue().getRepositoryFullName());
        assertTrue(ranked.get(0).getRepositoryScore() > ranked.get(1).getRepositoryScore());
    }

    @Test
    @DisplayName("Matching requested labels increases score and rank")
    void rankIssues_LabelMatch_ScoresHigher() {
        GitHubIssue issueWithLabels = createIssue(1L, 1, "Feature request", "Details", "org/repo", "open",
                List.of(new GitHubLabel(1L, "bug", "red", "Bug"), new GitHubLabel(2L, "help wanted", "green", "Help")),
                Instant.now());

        GitHubIssue issueWithoutLabels = createIssue(2L, 2, "Feature request", "Details", "org/repo", "open",
                List.of(new GitHubLabel(3L, "question", "grey", "Question")), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLabels(List.of("bug", "help wanted"));

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(issueWithoutLabels, issueWithLabels), filter);

        assertEquals(issueWithLabels.getId(), ranked.get(0).getIssue().getId());
        assertTrue(ranked.get(0).getLabelScore() > ranked.get(1).getLabelScore());
    }

    @Test
    @DisplayName("Keyword match in title scores higher than in body alone")
    void rankIssues_KeywordMatch_TitleWeightedOverBody() {
        GitHubIssue titleMatch = createIssue(1L, 10, "Authentication crash on startup", "Generic description", "org/repo", "open",
                List.of(), Instant.now());

        GitHubIssue bodyMatch = createIssue(2L, 20, "Minor UI fix", "This relates to authentication crash on startup", "org/repo", "open",
                List.of(), Instant.now());

        GitHubIssue noMatch = createIssue(3L, 30, "Update README", "Fix documentation typo", "org/repo", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("authentication crash");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(noMatch, bodyMatch, titleMatch), filter);

        assertEquals(titleMatch.getId(), ranked.get(0).getIssue().getId());
        assertEquals(bodyMatch.getId(), ranked.get(1).getIssue().getId());
        assertEquals(noMatch.getId(), ranked.get(2).getIssue().getId());
        assertTrue(ranked.get(0).getKeywordScore() > ranked.get(1).getKeywordScore());
        assertTrue(ranked.get(1).getKeywordScore() > ranked.get(2).getKeywordScore());
    }

    @Test
    @DisplayName("Beginner preference prioritizes issues with recognized starter labels")
    void rankIssues_BeginnerPreference_PrioritizesGoodFirstIssue() {
        GitHubIssue beginnerIssue1 = createIssue(1L, 100, "Refactor button", "Simple UI task", "org/repo", "open",
                List.of(new GitHubLabel(1L, "good first issue", "7057ff", "Starter")), Instant.now());

        GitHubIssue beginnerIssue2 = createIssue(2L, 200, "Update icon", "Starter task", "org/repo", "open",
                List.of(new GitHubLabel(2L, "first-timers-only", "008800", "Newcomer")), Instant.now());

        GitHubIssue expertIssue = createIssue(3L, 300, "Rewrite memory allocator", "Complex task", "org/repo", "open",
                List.of(new GitHubLabel(3L, "architecture", "000000", "Hard")), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setDifficultyPreference("beginner");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(expertIssue, beginnerIssue1, beginnerIssue2), filter);

        assertEquals(3, ranked.size());
        assertTrue(ranked.get(0).getBeginnerScore() > 0);
        assertTrue(ranked.get(1).getBeginnerScore() > 0);
        assertEquals(0.0, ranked.get(2).getBeginnerScore());
        assertEquals(expertIssue.getId(), ranked.get(2).getIssue().getId());
    }

    @Test
    @DisplayName("Safely handles issues with incomplete or null metadata without throwing exceptions")
    void rankIssues_IncompleteMetadata_HandledSafely() {
        GitHubIssue incomplete = new GitHubIssue();
        incomplete.setId(999L);

        GitHubIssue complete = createIssue(1L, 10, "Valid title", "Valid body", "org/repo", "open",
                List.of(new GitHubLabel(1L, "bug", "red", "Bug")), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Valid");
        filter.setLabels(List.of("bug"));

        List<ScoredIssue> ranked = assertDoesNotThrow(() ->
                ranker.rankIssues(List.of(incomplete, complete), filter)
        );

        assertEquals(2, ranked.size());
        assertEquals(complete.getId(), ranked.get(0).getIssue().getId());
        assertEquals(incomplete.getId(), ranked.get(1).getIssue().getId());
    }

    @Test
    @DisplayName("Deterministic tie-breaking: identical scores produce stable ordering")
    void rankIssues_EqualScores_StableDeterministicOrdering() {
        Instant sameTime = Instant.parse("2026-01-01T00:00:00Z");

        GitHubIssue issueA = createIssue(100L, 5, "Shared title", "Shared body", "org/repo", "open", List.of(), sameTime);
        GitHubIssue issueB = createIssue(200L, 10, "Shared title", "Shared body", "org/repo", "open", List.of(), sameTime);

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Shared");

        List<ScoredIssue> run1 = ranker.rankIssues(List.of(issueA, issueB), filter);
        List<ScoredIssue> run2 = ranker.rankIssues(List.of(issueB, issueA), filter);

        assertEquals(run1.get(0).getIssue().getId(), run2.get(0).getIssue().getId());
        assertEquals(run1.get(1).getIssue().getId(), run2.get(1).getIssue().getId());
        assertEquals(200L, run1.get(0).getIssue().getId());
        assertEquals(100L, run1.get(1).getIssue().getId());
    }

    @Test
    @DisplayName("Secondary factor: Freshness does not override primary relevance criteria")
    void rankIssues_FreshnessSecondary_DoesNotOverrideRelevance() {
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 86400);
        Instant today = Instant.now();

        GitHubIssue relevantOlder = createIssue(1L, 10, "Java bug", "Body", "org/repo", "open",
                List.of(new GitHubLabel(1L, "good first issue", "blue", "Starter")), thirtyDaysAgo);

        GitHubIssue irrelevantNewer = createIssue(2L, 20, "Unrelated", "Body", "org/repo", "open",
                List.of(), today);

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLabels(List.of("good first issue"));
        filter.setDifficultyPreference("beginner");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(irrelevantNewer, relevantOlder), filter);

        assertEquals(relevantOlder.getId(), ranked.get(0).getIssue().getId());
        assertTrue(ranked.get(0).getTotalScore() > ranked.get(1).getTotalScore());
    }

    @Test
    @DisplayName("Pagination compatibility: Preserves total item count without dropping items")
    void rankIssues_PaginationCompatibility_PreservesAllItems() {
        List<GitHubIssue> pageItems = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            pageItems.add(createIssue((long) i, i, "Issue " + i, "Body " + i, "org/repo", "open", List.of(), Instant.now()));
        }

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Issue");
        filter.setPage(1);
        filter.setPerPage(30);

        List<ScoredIssue> ranked = ranker.rankIssues(pageItems, filter);

        assertEquals(30, ranked.size());
    }

    @Test
    @DisplayName("Regression: Verified Java repository metadata awards maximum language bonus")
    void rankIssues_VerifiedJavaRepo_AwardsMaxLanguageScore() {
        RepositoryLanguageService langService = Mockito.mock(RepositoryLanguageService.class);
        when(langService.getRepositoryLanguage("spring-projects/spring-boot")).thenReturn(Optional.of("Java"));
        when(langService.isLanguageMatching("spring-projects/spring-boot", "Java")).thenReturn(true);

        IssueRanker rankerWithLang = new DefaultIssueRanker(langService);

        GitHubIssue javaIssue = createIssue(1L, 1, "Fix typo in doc", "Body", "spring-projects/spring-boot", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");

        ScoredIssue scored = rankerWithLang.scoreIssue(javaIssue, filter);

        assertEquals(DefaultIssueRanker.MAX_LANGUAGE_SCORE, scored.getLanguageScore());
        assertTrue(scored.getMatchedFactors().stream().anyMatch(f -> f.contains("Verified GitHub repository primary language: Java")));
    }

    @Test
    @DisplayName("Regression: Verified non-Java repository receives heavy language mismatch penalty")
    void rankIssues_VerifiedNonJavaRepo_ReceivesHeavyPenalty() {
        RepositoryLanguageService langService = Mockito.mock(RepositoryLanguageService.class);
        when(langService.getRepositoryLanguage("python-org/django")).thenReturn(Optional.of("Python"));
        when(langService.isLanguageMatching("python-org/django", "Java")).thenReturn(false);

        IssueRanker rankerWithLang = new DefaultIssueRanker(langService);

        GitHubIssue pythonIssue = createIssue(2L, 2, "Backend service error", "Body", "python-org/django", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");

        ScoredIssue scored = rankerWithLang.scoreIssue(pythonIssue, filter);

        assertEquals(-50.0, scored.getLanguageScore());
        assertTrue(scored.getMatchedFactors().stream().anyMatch(f -> f.contains("Repository primary language mismatch")));
    }

    @Test
    @DisplayName("Regression: Missing repository language receives zero verified bonus and is not falsely verified")
    void rankIssues_MissingRepoLanguage_ReceivesZeroVerifiedBonus() {
        RepositoryLanguageService langService = Mockito.mock(RepositoryLanguageService.class);
        when(langService.getRepositoryLanguage("unknown/repo")).thenReturn(Optional.empty());

        IssueRanker rankerWithLang = new DefaultIssueRanker(langService);

        GitHubIssue unverifiedIssue = createIssue(3L, 3, "Some issue", "Body", "unknown/repo", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");

        ScoredIssue scored = rankerWithLang.scoreIssue(unverifiedIssue, filter);

        assertEquals(0.0, scored.getLanguageScore());
        assertFalse(scored.getMatchedFactors().stream().anyMatch(f -> f.contains("Verified GitHub repository")));
        assertTrue(scored.getMatchedFactors().stream().anyMatch(f -> f.contains("repository primary language unverified")));
    }

    @Test
    @DisplayName("Regression: Spring Boot and REST API technical concepts weighted over generic keywords")
    void rankIssues_SpringBootAndRestApi_WeightedOverGenericKeywords() {
        GitHubIssue specificTechIssue = createIssue(1L, 10, "Spring Boot REST API endpoint for user authentication",
                "Implement secure endpoint using Spring Boot controllers and REST conventions",
                "org/repo", "open", List.of(), Instant.now());

        GitHubIssue genericKeywordsIssue = createIssue(2L, 20, "Backend development task for application service",
                "General development work for backend service system",
                "org/repo", "open", List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Spring Boot REST APIs backend development");

        List<ScoredIssue> ranked = ranker.rankIssues(List.of(genericKeywordsIssue, specificTechIssue), filter);

        assertEquals(specificTechIssue.getId(), ranked.get(0).getIssue().getId());
        assertTrue(ranked.get(0).getKeywordScore() > ranked.get(1).getKeywordScore());
        // Specific technology score should be significantly higher than dampened generic keywords
        assertTrue(ranked.get(0).getKeywordScore() >= 25.0);
        assertTrue(ranked.get(1).getKeywordScore() <= 10.0);
    }

    @Test
    @DisplayName("Regression: React frontend-only tasks receive conflict penalty when backend work requested")
    void rankIssues_FrontendOnlyTask_PenalizedForBackendSearch() {
        GitHubIssue reactIssue = createIssue(1L, 1, "React UI component with Tailwind CSS button styling",
                "Style the modal component using Tailwind CSS in React", "facebook/react", "open",
                List.of(new GitHubLabel(1L, "frontend", "blue", "UI")), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Java Spring Boot REST backend development");

        ScoredIssue scored = ranker.scoreIssue(reactIssue, filter);

        assertTrue(scored.getMatchedFactors().stream().anyMatch(f -> f.contains("Frontend-focused task penalized for backend search (-30.0 pts)")));
        // The -30 pt penalty should pull down total score
        assertTrue(scored.getTotalScore() < 0.0);
    }

    @Test
    @DisplayName("Regression: Generic keywords alone are capped and cannot dominate score")
    void rankIssues_GenericKeywords_StrictlyCapped() {
        GitHubIssue genericOnly = createIssue(1L, 1, "Backend development for service application tool",
                "General backend code system development work", "org/repo", "open",
                List.of(), Instant.now());

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("backend development service application");

        ScoredIssue scored = ranker.scoreIssue(genericOnly, filter);

        // Generic terms in title and body must be strictly capped
        assertTrue(scored.getKeywordScore() <= 6.0);
    }

    private GitHubIssue createIssue(Long id, int number, String title, String body, String repo,
                                    String state, List<GitHubLabel> labels, Instant updatedAt) {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(id);
        issue.setNumber(number);
        issue.setTitle(title);
        issue.setBody(body);
        issue.setRepositoryFullName(repo);
        issue.setState(state);
        issue.setLabels(labels);
        issue.setUpdatedAt(updatedAt);
        issue.setCreatedAt(updatedAt != null ? updatedAt.minusSeconds(86400) : null);
        issue.setCommentsCount(0);
        return issue;
    }
}
