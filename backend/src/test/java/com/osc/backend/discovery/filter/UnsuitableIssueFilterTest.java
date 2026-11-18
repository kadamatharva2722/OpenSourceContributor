package com.osc.backend.discovery.filter;

import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UnsuitableIssueFilter Unit Tests")
class UnsuitableIssueFilterTest {

    private UnsuitableIssueFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UnsuitableIssueFilter();
    }

    @Test
    @DisplayName("Excludes senior developer job advertisements")
    void isEligible_JobAdvertisement_Excluded() {
        GitHubIssue issue = createIssue(
                "Senior Java Developer Needed for Backend Microservices",
                "We are looking for an experienced Java developer with Spring Boot experience to join our team. Full-time position with competitive salary.",
                List.of()
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes hiring postings with recruitment title patterns")
    void isEligible_WeAreHiring_Excluded() {
        GitHubIssue issue = createIssue(
                "We are hiring backend engineers!",
                "Check out our open positions at https://example.com/careers",
                List.of()
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes issues carrying recruitment labels")
    void isEligible_RecruitmentLabel_Excluded() {
        GitHubIssue issue = createIssue(
                "Backend Engineer position",
                "Looking for a backend contributor",
                List.of(new GitHubLabel(1L, "hiring", "blue", "Careers"))
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes daily reading and study check-in tasks")
    void isEligible_DailyReadingTask_Excluded() {
        GitHubIssue issue = createIssue(
                "An unrelated daily reading task",
                "Read chapter 3 of Clean Code and take notes",
                List.of()
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes 100 days of code / day numbered logs")
    void isEligible_DayNumberedLog_Excluded() {
        GitHubIssue issue = createIssue(
                "Day 42 - Reading Spring Boot source code",
                "My daily study notes for today",
                List.of()
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes issues carrying daily log labels")
    void isEligible_DailyLogLabel_Excluded() {
        GitHubIssue issue = createIssue(
                "Study notes for Java concurrency",
                "Read about synchronized and volatile",
                List.of(new GitHubLabel(1L, "daily-reading", "yellow", "Daily notes"))
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes general announcements and service maintenance notices")
    void isEligible_Announcement_Excluded() {
        GitHubIssue issue = createIssue(
                "[Announcement] Planned maintenance window this Saturday",
                "The server will be offline from 2am to 4am UTC",
                List.of()
        );

        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Excludes placeholder and spam issues")
    void isEligible_SpamPlaceholder_Excluded() {
        GitHubIssue issue = createIssue("test", "", List.of());
        assertFalse(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves documentation update issues as eligible")
    void isEligible_DocumentationIssue_Eligible() {
        GitHubIssue issue = createIssue(
                "Update documentation for Spring Boot REST API endpoints",
                "The OpenAPI documentation is missing request body examples for POST /api/v1/auth",
                List.of(new GitHubLabel(1L, "documentation", "0075ca", "Docs"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves bug fix issues as eligible")
    void isEligible_BugFix_Eligible() {
        GitHubIssue issue = createIssue(
                "Fix NullPointerException in AuthController during token refresh",
                "When refresh token is expired, a NullPointerException is thrown instead of 401",
                List.of(new GitHubLabel(1L, "bug", "d73a4a", "Bug"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves unit testing issues as eligible")
    void isEligible_TestingIssue_Eligible() {
        GitHubIssue issue = createIssue(
                "Add unit tests for UserService and UserRepository",
                "Increase branch coverage for edge cases when user record is soft-deleted",
                List.of(new GitHubLabel(1L, "test", "yellow", "Tests"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves feature enhancement issues as eligible")
    void isEligible_FeatureIssue_Eligible() {
        GitHubIssue issue = createIssue(
                "Implement REST endpoint for user preference export",
                "Allow users to download their preferences as JSON via GET /api/v1/preferences/export",
                List.of(new GitHubLabel(1L, "enhancement", "a2eeef", "New feature"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves technical issues mentioning daily as a scheduler concept")
    void isEligible_DailySchedulerIssue_Eligible() {
        GitHubIssue issue = createIssue(
                "Fix memory leak in daily scheduled task runner",
                "The cron task scheduled at midnight does not release database connections properly",
                List.of(new GitHubLabel(1L, "bug", "d73a4a", "Bug"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves technical issues with day/time phrases in title")
    void isEligible_TechnicalIssueWithDayTimePhrase_Eligible() {
        GitHubIssue issue = createIssue(
                "Authentication token expires in 1 day (issue #42)",
                "Tokens should expire in 30 days instead of 1 day",
                List.of(new GitHubLabel(1L, "bug", "d73a4a", "Bug"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves technical issues reading task queues or data")
    void isEligible_TechnicalIssueReadingTaskQueue_Eligible() {
        GitHubIssue issue = createIssue(
                "Error reading task definitions from database during startup",
                "The task reader fails when task payload contains null bytes",
                List.of(new GitHubLabel(1L, "bug", "d73a4a", "Bug"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Preserves technical issues referencing zero-day patches")
    void isEligible_ZeroDayPatchIssue_Eligible() {
        GitHubIssue issue = createIssue(
                "Zero-day vulnerability patch in HTTP client parsing",
                "Backport the security fix from upstream",
                List.of(new GitHubLabel(1L, "security", "red", "Security"))
        );

        assertTrue(filter.isEligible(issue));
    }

    @Test
    @DisplayName("Safely handles null issue without throwing exceptions")
    void isEligible_NullIssue_ReturnsFalse() {
        assertFalse(filter.isEligible(null));
    }

    private GitHubIssue createIssue(String title, String body, List<GitHubLabel> labels) {
        GitHubIssue issue = new GitHubIssue();
        issue.setId(100L);
        issue.setNumber(1);
        issue.setTitle(title);
        issue.setBody(body);
        issue.setRepositoryFullName("org/repo");
        issue.setState("open");
        issue.setLabels(labels);
        return issue;
    }
}
