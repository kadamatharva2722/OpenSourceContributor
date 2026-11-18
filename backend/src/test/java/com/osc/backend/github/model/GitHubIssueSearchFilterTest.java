package com.osc.backend.github.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubIssueSearchFilter Unit Tests")
class GitHubIssueSearchFilterTest {

    @Test
    @DisplayName("Default filter sets page=1, perPage=30, state=open and includes is:issue")
    void defaultFilter_ContainsStandardDefaults() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder().build();

        assertEquals(1, filter.getPage());
        assertEquals(30, filter.getPerPage());
        assertEquals("open", filter.getState());
        assertEquals("is:issue is:open", filter.toQueryString());
    }

    @Test
    @DisplayName("Enforces perPage upper limit of 100")
    void setPerPage_ExceedingMax_CappedAt100() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .perPage(250)
                .build();

        assertEquals(100, filter.getPerPage());
    }

    @Test
    @DisplayName("Enforces minimum page of 1")
    void setPage_NegativeOrZero_DefaultsToOne() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .page(-5)
                .build();

        assertEquals(1, filter.getPage());
    }

    @Test
    @DisplayName("Builds complete query with keywords, repository, language, and labels")
    void toQueryString_WithFullCriteria_ConstructsCorrectQuery() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .query("memory leak")
                .repository("facebook/react")
                .language("TypeScript")
                .labels(List.of("bug", "good first issue"))
                .state("open")
                .build();

        String queryString = filter.toQueryString();

        assertTrue(queryString.startsWith("is:issue is:open"));
        assertTrue(queryString.contains("repo:facebook/react"));
        assertTrue(queryString.contains("language:TypeScript"));
        assertTrue(queryString.contains("label:bug"));
        assertTrue(queryString.contains("label:\"good first issue\""));
        assertTrue(queryString.endsWith("memory leak"));
    }

    @Test
    @DisplayName("Quotes multi-word language properly")
    void toQueryString_MultiWordLanguage_QuotesLanguage() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .language("Common Lisp")
                .build();

        assertTrue(filter.toQueryString().contains("language:\"Common Lisp\""));
    }
}
