package com.osc.backend.discovery.service;

import com.osc.backend.discovery.dto.IssueMatchDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.ranking.ScoredIssue;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@DisplayName("MatchExplanationGenerator Unit Tests")
class MatchExplanationGeneratorTest {

    private MatchExplanationGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MatchExplanationGenerator();
    }

    @Test
    @DisplayName("Generates factual match explanation clearly distinguishing verified GitHub data from inferred criteria")
    void generateExplanation_WithMatchingCriteria_BuildsFactualExplanation() {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(1001L);
        issue.setNumber(42);
        issue.setTitle("NullPointerException in auth module");
        issue.setBody("Fix the null pointer exception when token is empty");
        issue.setRepositoryFullName("octocat/Hello-World");
        issue.setState("open");
        issue.setCommentsCount(3);
        issue.setLabels(List.of(
                new GitHubLabel(1L, "good first issue", "7057ff", "Good for newcomers"),
                new GitHubLabel(2L, "bug", "d73a4a", "Something isn't working")
        ));

        ParsedFilterDto filter = new ParsedFilterDto(
                "auth", "Java", "octocat/Hello-World", List.of("good first issue"),
                "open", null, null, "beginner", 1, 30
        );

        IssueMatchDto matchDto = generator.generateExplanation(issue, filter);

        assertNotNull(matchDto);
        assertEquals(issue, matchDto.getIssue());

        String explanation = matchDto.getMatchExplanation();
        assertNotNull(explanation);

        // Verify distinction: Verified GitHub Metadata vs Inferred Query Relevance
        assertTrue(explanation.contains("Verified GitHub Metadata:"));
        assertTrue(explanation.contains("Inferred Query Relevance:"));

        // Verify factual assertions are present
        assertTrue(explanation.contains("good first issue"));
        assertTrue(explanation.contains("octocat/Hello-World"));
        assertTrue(explanation.contains("Contains requested search terms in title"));
        assertTrue(explanation.contains("open with 3 comment(s)"));
        assertTrue(explanation.contains("Targeted via requested language filter 'Java'"));

        // Verify NO invented probabilities or unverifiable claims
        assertFalse(explanation.contains("probability"));
        assertFalse(explanation.contains("acceptance"));
        assertFalse(explanation.contains("maintainer responsiveness"));
        assertFalse(explanation.contains("success rate"));

        // Verify matched criteria list
        List<String> criteria = matchDto.getMatchedCriteria();
        assertTrue(criteria.stream().anyMatch(c -> c.contains("Java")));
        assertTrue(criteria.stream().anyMatch(c -> c.contains("good first issue")));
        assertTrue(criteria.stream().anyMatch(c -> c.contains("octocat/Hello-World")));
    }

    @Test
    @DisplayName("Clarifies difficulty as a user preference when non-beginner difficulty is requested")
    void generateExplanation_DifficultyPreference_ClarifiedAsUserPreference() {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(1002L);
        issue.setTitle("Refactor database indexing");
        issue.setRepositoryFullName("facebook/react");
        issue.setState("open");
        issue.setCommentsCount(0);

        ParsedFilterDto filter = new ParsedFilterDto(
                null, null, null, null, "open", null, null, "intermediate", 1, 30
        );

        IssueMatchDto matchDto = generator.generateExplanation(issue, filter);

        assertTrue(matchDto.getMatchExplanation().contains("note: difficulty is a user preference and not a verified GitHub metric"));
    }

    @Test
    @DisplayName("Integrates ScoredIssue scores and factor breakdown into IssueMatchDto")
    void generateExplanation_WithScoredIssue_PopulatesScoreAndFactors() {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(2001L);
        issue.setNumber(88);
        issue.setTitle("Bug in parser");
        issue.setState("open");

        ParsedFilterDto filter = new ParsedFilterDto();

        ScoredIssue scored = new ScoredIssue(
                issue, 85.5, 25.0, 30.0, 20.0, 0.0, 0.0, 10.0, 0.5,
                List.of("Exact repository match: facebook/react", "Matched requested label: bug")
        );

        IssueMatchDto matchDto = generator.generateExplanation(scored, filter);

        assertNotNull(matchDto);
        assertEquals(85.5, matchDto.getMatchScore());
        assertTrue(matchDto.getMatchedCriteria().contains("Exact repository match: facebook/react"));
        assertTrue(matchDto.getMatchedCriteria().contains("Matched requested label: bug"));
    }

    @Test
    @DisplayName("Regression: Verified repository primary language is clearly stated as verified fact")
    void generateExplanation_VerifiedRepositoryLanguage_StatesVerifiedFact() {
        RepositoryLanguageService langService = Mockito.mock(RepositoryLanguageService.class);
        when(langService.getRepositoryLanguage("spring-projects/spring-boot")).thenReturn(Optional.of("Java"));
        when(langService.isLanguageMatching("spring-projects/spring-boot", "Java")).thenReturn(true);

        MatchExplanationGenerator gen = new MatchExplanationGenerator(langService);

        GitHubIssue issue = new GitHubIssue();
        issue.setId(4001L);
        issue.setTitle("Documentation typo in quickstart");
        issue.setRepositoryFullName("spring-projects/spring-boot");
        issue.setState("open");

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");

        IssueMatchDto matchDto = gen.generateExplanation(issue, filter);

        assertTrue(matchDto.getMatchExplanation().contains("Repository primary language is verified as Java"));
        assertTrue(matchDto.getMatchedCriteria().contains("Verified Repository Primary Language: Java"));
    }

    @Test
    @DisplayName("Regression: Unverified repository language does not claim verified fact")
    void generateExplanation_UnverifiedLanguage_StatesQueryRelevanceOnly() {
        RepositoryLanguageService langService = Mockito.mock(RepositoryLanguageService.class);
        when(langService.getRepositoryLanguage("unknown/repo")).thenReturn(Optional.empty());

        MatchExplanationGenerator gen = new MatchExplanationGenerator(langService);

        GitHubIssue issue = new GitHubIssue();
        issue.setId(4002L);
        issue.setTitle("Some bug report");
        issue.setRepositoryFullName("unknown/repo");
        issue.setState("open");

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setLanguage("Java");

        IssueMatchDto matchDto = gen.generateExplanation(issue, filter);

        assertFalse(matchDto.getMatchExplanation().contains("Repository primary language is verified as"));
        assertTrue(matchDto.getMatchExplanation().contains("Targeted via requested language filter 'Java'"));
    }

    @Test
    @DisplayName("Regression: Explains matches for specific frameworks like Spring Boot and REST APIs")
    void generateExplanation_TechnicalConcepts_ExplainsSpringBootAndRest() {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(4003L);
        issue.setTitle("Build Spring Boot REST API for authentication");
        issue.setBody("Develop the Spring Boot controller with REST conventions");
        issue.setRepositoryFullName("org/repo");
        issue.setState("open");

        ParsedFilterDto filter = new ParsedFilterDto();
        filter.setKeywords("Spring Boot REST APIs");

        IssueMatchDto matchDto = generator.generateExplanation(issue, filter);

        assertTrue(matchDto.getMatchExplanation().contains("Matches requested framework: Spring Boot"));
        assertTrue(matchDto.getMatchExplanation().contains("Matches requested technical concept: REST APIs"));
        assertTrue(matchDto.getMatchedCriteria().contains("Technical Match: Spring Boot"));
        assertTrue(matchDto.getMatchedCriteria().contains("Technical Match: REST APIs"));
    }

    @Test
    @DisplayName("Regression test: Never produces forbidden speculative assertions or acceptance claims")
    void generateExplanation_Regression_NoForbiddenClaims() {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(3001L);
        issue.setTitle("Documentation typo in quickstart");
        issue.setBody("Please fix the typo in getting started guide");
        issue.setRepositoryFullName("apache/spark");
        issue.setState("open");
        issue.setCommentsCount(1);
        issue.setLabels(List.of(
                new GitHubLabel(1L, "good first issue", "7057ff", "Starter"),
                new GitHubLabel(2L, "documentation", "0075ca", "Docs")
        ));

        ParsedFilterDto filter = new ParsedFilterDto(
                "typo", "Scala", "apache/spark", List.of("good first issue"),
                "open", null, null, "beginner", 1, 30
        );

        IssueMatchDto matchDto = generator.generateExplanation(issue, filter);
        String explanation = matchDto.getMatchExplanation();

        assertNotNull(explanation);
        assertFalse(explanation.contains("probability"), "Explanation must never claim probability");
        assertFalse(explanation.contains("acceptance"), "Explanation must never mention acceptance");
        assertFalse(explanation.contains("maintainer responsiveness"), "Explanation must never claim maintainer responsiveness");
        assertFalse(explanation.contains("success rate"), "Explanation must never claim success rate");
        assertTrue(explanation.contains("Note: Community labels indicate the intended audience set by maintainers, not an objective guarantee of task simplicity"));
    }

    @Test
    @DisplayName("Returns null when issue is null")
    void generateExplanation_NullIssue_ReturnsNull() {
        assertNull(generator.generateExplanation((GitHubIssue) null, new ParsedFilterDto()));
        assertNull(generator.generateExplanation((ScoredIssue) null, new ParsedFilterDto()));
    }
}
