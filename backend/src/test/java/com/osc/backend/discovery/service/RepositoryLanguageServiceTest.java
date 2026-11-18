package com.osc.backend.discovery.service;

import com.osc.backend.github.exception.GitHubResourceNotFoundException;
import com.osc.backend.github.model.GitHubRepository;
import com.osc.backend.github.provider.GitHubIssueProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryLanguageService Unit Tests")
class RepositoryLanguageServiceTest {

    @Mock
    private GitHubIssueProvider gitHubIssueProvider;

    private DefaultRepositoryLanguageService languageService;

    @BeforeEach
    void setUp() {
        languageService = new DefaultRepositoryLanguageService(gitHubIssueProvider);
    }

    @Test
    @DisplayName("Successfully resolves and caches repository primary language")
    void getRepositoryLanguage_Success_CachesResult() {
        GitHubRepository repo = new GitHubRepository();
        repo.setLanguage("Java");
        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenReturn(repo);

        Optional<String> lang1 = languageService.getRepositoryLanguage("spring-projects/spring-boot");
        assertTrue(lang1.isPresent());
        assertEquals("Java", lang1.get());

        // Second call must be served from cache without second provider call
        Optional<String> lang2 = languageService.getRepositoryLanguage("spring-projects/spring-boot");
        assertTrue(lang2.isPresent());
        assertEquals("Java", lang2.get());

        verify(gitHubIssueProvider, times(1)).getRepository("spring-projects", "spring-boot");
    }

    @Test
    @DisplayName("De-duplicates concurrent network requests for the same repository")
    void getRepositoryLanguage_ConcurrentRequests_Deduplicated() throws Exception {
        GitHubRepository repo = new GitHubRepository();
        repo.setLanguage("Java");

        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenAnswer(invocation -> {
            Thread.sleep(50); // Simulate network latency
            return repo;
        });

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Optional<String>> f1 = executor.submit(() -> {
            latch.await();
            return languageService.getRepositoryLanguage("spring-projects/spring-boot");
        });
        Future<Optional<String>> f2 = executor.submit(() -> {
            latch.await();
            return languageService.getRepositoryLanguage("spring-projects/spring-boot");
        });

        latch.countDown();

        Optional<String> res1 = f1.get(2, TimeUnit.SECONDS);
        Optional<String> res2 = f2.get(2, TimeUnit.SECONDS);

        executor.shutdown();

        assertTrue(res1.isPresent());
        assertTrue(res2.isPresent());
        assertEquals("Java", res1.get());
        assertEquals("Java", res2.get());

        // Verify provider was invoked exactly ONCE despite concurrent callers
        verify(gitHubIssueProvider, times(1)).getRepository("spring-projects", "spring-boot");
    }

    @Test
    @DisplayName("Gracefully returns empty on 404 or network failure without throwing exceptions")
    void getRepositoryLanguage_NotFound_ReturnsEmptyGracefully() {
        when(gitHubIssueProvider.getRepository("unknown", "repo"))
                .thenThrow(new GitHubResourceNotFoundException("unknown/repo", "Not found"));

        Optional<String> lang = assertDoesNotThrow(() ->
                languageService.getRepositoryLanguage("unknown/repo")
        );

        assertTrue(lang.isEmpty());
    }

    @Test
    @DisplayName("Correctly checks language matching and aliases")
    void isLanguageMatching_Aliases_MatchCorrectly() {
        GitHubRepository repoJs = new GitHubRepository();
        repoJs.setLanguage("JavaScript");
        when(gitHubIssueProvider.getRepository("facebook", "react")).thenReturn(repoJs);

        assertTrue(languageService.isLanguageMatching("facebook/react", "JavaScript"));
        assertTrue(languageService.isLanguageMatching("facebook/react", "js"));
        assertFalse(languageService.isLanguageMatching("facebook/react", "Java"));
    }

    @Test
    @DisplayName("Correctly identifies language mismatch for non-matching languages")
    void isLanguageMismatch_IdentifiesMismatch() {
        GitHubRepository repoPy = new GitHubRepository();
        repoPy.setLanguage("Python");
        when(gitHubIssueProvider.getRepository("django", "django")).thenReturn(repoPy);

        // Requested Java, verified Python -> mismatch is true
        assertTrue(languageService.isLanguageMismatch("django/django", "Java"));
        // Requested Python, verified Python -> mismatch is false
        assertFalse(languageService.isLanguageMismatch("django/django", "Python"));
    }

    @Test
    @DisplayName("Unverified repository language is not considered a proven mismatch")
    void isLanguageMismatch_Unverified_NotTreatedAsMismatch() {
        when(gitHubIssueProvider.getRepository("unknown", "repo"))
                .thenThrow(new RuntimeException("Rate limit reached"));

        // Language is unverified; cannot prove mismatch
        assertFalse(languageService.isLanguageMismatch("unknown/repo", "Java"));
    }

    @Test
    @DisplayName("Safely handles null and invalid repository coordinates")
    void getRepositoryLanguage_InvalidCoordinates_ReturnsEmpty() {
        assertTrue(languageService.getRepositoryLanguage(null).isEmpty());
        assertTrue(languageService.getRepositoryLanguage("").isEmpty());
        assertTrue(languageService.getRepositoryLanguage("invalid-no-slash").isEmpty());
    }
}
