package com.osc.backend.github.provider;

import com.osc.backend.github.client.GitHubClientProperties;
import com.osc.backend.github.exception.GitHubApiUnavailableException;
import com.osc.backend.github.exception.GitHubRateLimitExceededException;
import com.osc.backend.github.exception.GitHubResourceNotFoundException;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import com.osc.backend.github.model.GitHubPage;
import com.osc.backend.github.model.GitHubRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("PublicGitHubProvider Unit Tests")
class PublicGitHubProviderTest {

    private MockRestServiceServer mockServer;
    private PublicGitHubProvider provider;
    private GitHubClientProperties properties;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.baseUrl("https://api.github.com").build();

        properties = new GitHubClientProperties();
        properties.setBaseUrl("https://api.github.com");
        properties.setMaxRetries(2);
        properties.setRetryBackoffMs(0); // instant retries for test execution

        provider = new PublicGitHubProvider(restClient, properties);
    }

    @Test
    @DisplayName("searchIssues successfully filters out Pull Requests from results")
    void searchIssues_FiltersPullRequests() {
        String jsonResponse = """
                {
                  "total_count": 2,
                  "incomplete_results": false,
                  "items": [
                    {
                      "id": 101,
                      "number": 1,
                      "title": "Legitimate Issue",
                      "body": "Issue description",
                      "html_url": "https://github.com/octocat/Hello-World/issues/1",
                      "state": "open",
                      "repository_url": "https://api.github.com/repos/octocat/Hello-World",
                      "user": {
                        "login": "octocat",
                        "avatar_url": "https://avatars.githubusercontent.com/u/583231"
                      },
                      "labels": [
                        { "id": 1, "name": "bug", "color": "f29513", "description": "Bug report" }
                      ],
                      "comments": 3,
                      "pull_request": null,
                      "created_at": "2026-01-01T00:00:00Z",
                      "updated_at": "2026-01-02T00:00:00Z"
                    },
                    {
                      "id": 102,
                      "number": 2,
                      "title": "Pull Request disguised as issue",
                      "body": "PR description",
                      "html_url": "https://github.com/octocat/Hello-World/pull/2",
                      "state": "open",
                      "repository_url": "https://api.github.com/repos/octocat/Hello-World",
                      "user": {
                        "login": "contributor",
                        "avatar_url": "https://avatars.githubusercontent.com/u/12345"
                      },
                      "labels": [],
                      "comments": 0,
                      "pull_request": {
                        "url": "https://api.github.com/repos/octocat/Hello-World/pulls/2"
                      },
                      "created_at": "2026-01-01T00:00:00Z",
                      "updated_at": "2026-01-02T00:00:00Z"
                    }
                  ]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "60");
        headers.set("X-RateLimit-Remaining", "58");
        headers.set("X-RateLimit-Reset", "1700000000");

        mockServer.expect(requestTo(containsString("/search/issues")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON).headers(headers));

        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .query("bug")
                .build();

        GitHubPage<GitHubIssue> page = provider.searchIssues(filter);

        mockServer.verify();
        assertNotNull(page);
        assertEquals(2L, page.getTotalCount());
        // Verifies PR was filtered out, leaving exactly 1 issue
        assertEquals(1, page.getItems().size());

        GitHubIssue issue = page.getItems().get(0);
        assertEquals(101L, issue.getId());
        assertEquals("Legitimate Issue", issue.getTitle());
        assertEquals("octocat/Hello-World", issue.getRepositoryFullName());
        assertEquals("octocat", issue.getAuthorUsername());
        assertEquals(1, issue.getLabels().size());
        assertEquals("bug", issue.getLabels().get(0).getName());

        assertNotNull(page.getRateLimit());
        assertEquals(58, page.getRateLimit().getRemaining());
    }

    @Test
    @DisplayName("searchIssues throws GitHubRateLimitExceededException when remaining is 0 on 403")
    void searchIssues_RateLimited_ThrowsException() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "60");
        headers.set("X-RateLimit-Remaining", "0");
        headers.set("X-RateLimit-Reset", "1700000000");

        mockServer.expect(requestTo(containsString("/search/issues")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).headers(headers).body("{\"message\": \"API rate limit exceeded\"}"));

        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder().build();

        GitHubRateLimitExceededException ex = assertThrows(GitHubRateLimitExceededException.class, () ->
                provider.searchIssues(filter)
        );

        mockServer.verify();
        assertNotNull(ex.getRateLimit());
        assertEquals(0, ex.getRateLimit().getRemaining());
    }

    @Test
    @DisplayName("getRepository returns domain repository on 200 OK")
    void getRepository_Success() {
        String json = """
                {
                  "id": 1296269,
                  "name": "Hello-World",
                  "full_name": "octocat/Hello-World",
                  "description": "My first repository on GitHub!",
                  "html_url": "https://github.com/octocat/Hello-World",
                  "language": "Java",
                  "stargazers_count": 2100,
                  "forks_count": 1500,
                  "open_issues_count": 35,
                  "topics": ["java", "starter"],
                  "license": {
                    "key": "mit",
                    "name": "MIT License",
                    "spdx_id": "MIT"
                  },
                  "created_at": "2026-01-01T00:00:00Z",
                  "updated_at": "2026-01-02T00:00:00Z",
                  "pushed_at": "2026-01-03T00:00:00Z"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubRepository repo = provider.getRepository("octocat", "Hello-World");

        mockServer.verify();
        assertNotNull(repo);
        assertEquals(1296269L, repo.getId());
        assertEquals("Hello-World", repo.getName());
        assertEquals("octocat/Hello-World", repo.getFullName());
        assertEquals("Java", repo.getLanguage());
        assertEquals(2100, repo.getStargazersCount());
        assertEquals("MIT License", repo.getLicenseName());
        assertEquals(2, repo.getTopics().size());
    }

    @Test
    @DisplayName("getRepository throws GitHubResourceNotFoundException on 404")
    void getRepository_NotFound_ThrowsException() {
        mockServer.expect(requestTo("https://api.github.com/repos/nonexistent/repo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(GitHubResourceNotFoundException.class, () ->
                provider.getRepository("nonexistent", "repo")
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("Retries on 500 server error and succeeds on subsequent attempt")
    void executeWithRetry_ServerTransientError_RetriesAndSucceeds() {
        String json = """
                {
                  "id": 1,
                  "name": "Hello-World",
                  "full_name": "octocat/Hello-World",
                  "language": "Java"
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andRespond(withServerError());

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GitHubRepository repo = provider.getRepository("octocat", "Hello-World");

        mockServer.verify();
        assertNotNull(repo);
        assertEquals("octocat/Hello-World", repo.getFullName());
    }

    @Test
    @DisplayName("Throws GitHubApiUnavailableException when 500 retries are exhausted")
    void executeWithRetry_RetriesExhausted_ThrowsException() {
        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andRespond(withServerError());

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World"))
                .andRespond(withServerError());

        assertThrows(GitHubApiUnavailableException.class, () ->
                provider.getRepository("octocat", "Hello-World")
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("getIssue rejects pull requests with GitHubResourceNotFoundException")
    void getIssue_WhenPullRequest_RejectsWithNotFound() {
        String json = """
                {
                  "id": 999,
                  "number": 42,
                  "title": "PR Title",
                  "state": "open",
                  "pull_request": {
                    "url": "https://api.github.com/repos/octocat/Hello-World/pulls/42"
                  }
                }
                """;

        mockServer.expect(requestTo("https://api.github.com/repos/octocat/Hello-World/issues/42"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThrows(GitHubResourceNotFoundException.class, () ->
                provider.getIssue("octocat", "Hello-World", 42)
        );

        mockServer.verify();
    }

    @Test
    @DisplayName("searchIssues enforces accessible count cap at 1,000 for totalCount > 1,000")
    void searchIssues_TotalCountAbove1000_CapsAccessibleCount() {
        String json = """
                {
                  "total_count": 50000,
                  "incomplete_results": false,
                  "items": [
                    {
                      "id": 1,
                      "number": 10,
                      "title": "Bug report",
                      "html_url": "https://github.com/octocat/Hello-World/issues/10",
                      "state": "open",
                      "repository_url": "https://api.github.com/repos/octocat/Hello-World",
                      "user": { "login": "octocat" },
                      "labels": [],
                      "comments": 0,
                      "created_at": "2026-01-01T00:00:00Z",
                      "updated_at": "2026-01-02T00:00:00Z"
                    }
                  ]
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "60");
        headers.set("X-RateLimit-Remaining", "59");
        headers.set("X-RateLimit-Reset", "1700000000");

        mockServer.expect(requestTo(containsString("/search/issues")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON).headers(headers));

        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .query("bug")
                .page(1)
                .perPage(30)
                .build();

        GitHubPage<GitHubIssue> result = provider.searchIssues(filter);

        assertNotNull(result);
        assertEquals(50000L, result.getTotalCount(), "Actual GitHub total_count preserved");
        assertEquals(1000L, result.getAccessibleCount(), "Accessible count capped at 1,000");
        assertEquals(34, result.getTotalPages(), "Total pages ceil(1000 / 30) = 34");
        assertTrue(result.isHasNext(), "hasNext should be true on page 1");
        assertFalse(result.isHasPrevious(), "hasPrevious should be false on page 1");
    }

    @Test
    @DisplayName("searchIssues rejects page requests exceeding accessible window with IllegalArgumentException")
    void searchIssues_PageExceedingAccessibleLimit_ThrowsIllegalArgumentException() {
        GitHubIssueSearchFilter filter = GitHubIssueSearchFilter.builder()
                .query("bug")
                .page(35)
                .perPage(30)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                provider.searchIssues(filter)
        );

        assertTrue(ex.getMessage().contains("exceeds the maximum accessible GitHub Search window"));
    }
}
