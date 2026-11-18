package com.osc.backend.discovery.service;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.service.AiUsageService;
import com.osc.backend.common.exception.AiQuotaExceededException;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.filter.UnsuitableIssueFilter;
import com.osc.backend.discovery.parser.SearchQueryParser;
import com.osc.backend.discovery.ranking.DefaultIssueRanker;
import com.osc.backend.discovery.validator.SearchFilterValidator;
import com.osc.backend.github.exception.GitHubRateLimitExceededException;
import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import com.osc.backend.github.model.GitHubLabel;
import com.osc.backend.github.model.GitHubPage;
import com.osc.backend.github.model.GitHubRateLimit;
import com.osc.backend.github.model.GitHubRepository;
import com.osc.backend.github.provider.GitHubIssueProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscoveryService Unit Tests")
class DiscoveryServiceTest {

    @Mock
    private SearchQueryParser searchQueryParser;

    @Mock
    private GitHubIssueProvider gitHubIssueProvider;

    @Mock
    private AiUsageService aiUsageService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private SearchFilterValidator validator;
    private MatchExplanationGenerator explanationGenerator;
    private RepositoryLanguageService repositoryLanguageService;
    private UnsuitableIssueFilter unsuitableIssueFilter;
    private DiscoveryServiceImpl discoveryService;

    private final Long testUserId = 42L;

    @BeforeEach
    void setUp() {
        validator = new SearchFilterValidator();
        repositoryLanguageService = new DefaultRepositoryLanguageService(gitHubIssueProvider);
        explanationGenerator = new MatchExplanationGenerator(repositoryLanguageService);
        unsuitableIssueFilter = new UnsuitableIssueFilter();
        discoveryService = new DiscoveryServiceImpl(
                searchQueryParser,
                validator,
                gitHubIssueProvider,
                explanationGenerator,
                aiUsageService,
                currentUserProvider,
                new DefaultIssueRanker(repositoryLanguageService),
                repositoryLanguageService,
                unsuitableIssueFilter,
                null
        );
    }

    @Test
    @DisplayName("discoverIssues successfully orchestrates complete flow and consumes quota only once")
    void discoverIssues_Success_OrchestratesFlow() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto(
                "Find beginner-friendly Java issues with good first issue labels", 1, 30
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);

        AiQuotaResponseDto quotaDto = new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false);
        when(aiUsageService.consumeQuota(testUserId)).thenReturn(quotaDto);

        ParsedFilterDto parsedFilter = new ParsedFilterDto(
                null, "Java", null, List.of("good first issue"), "open", null, null, "beginner", 1, 30
        );
        when(searchQueryParser.parse(eq(request.getPrompt()), eq(1), eq(30))).thenReturn(parsedFilter);

        GitHubIssue mockIssue = new GitHubIssue();
        mockIssue.setId(901L);
        mockIssue.setNumber(15);
        mockIssue.setTitle("Fix Java documentation typo");
        mockIssue.setRepositoryFullName("spring-projects/spring-boot");
        mockIssue.setState("open");
        mockIssue.setCommentsCount(1);
        mockIssue.setLabels(List.of(new GitHubLabel(1L, "good first issue", "7057ff", "Starter")));

        GitHubRepository repo = new GitHubRepository();
        repo.setLanguage("Java");
        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenReturn(repo);

        GitHubRateLimit rateLimit = new GitHubRateLimit(60, 59, Instant.now().plusSeconds(3600), null);
        GitHubPage<GitHubIssue> gitHubPage = new GitHubPage<>(List.of(mockIssue), 1L, 1, 30, false, false, rateLimit);

        when(gitHubIssueProvider.searchIssues(any(GitHubIssueSearchFilter.class))).thenReturn(gitHubPage);

        // Execute
        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        // Verify response
        assertNotNull(response);
        assertEquals(request.getPrompt(), response.getPrompt());
        assertEquals(1, response.getResults().size());
        assertEquals(19, response.getRemainingDailyQuota());
        assertEquals(1L, response.getTotalCount());
        assertNotNull(response.getRateLimit());

        // Verify match explanation
        String explanation = response.getResults().get(0).getMatchExplanation();
        assertTrue(explanation.contains("Matches requested language: Java"));
        assertTrue(explanation.contains("good first issue"));

        // Verify quota was consumed exactly ONCE for the request
        verify(aiUsageService, times(1)).consumeQuota(testUserId);

        // Verify parser was called exactly ONCE
        verify(searchQueryParser, times(1)).parse(request.getPrompt(), 1, 30);

        // Verify GitHub provider was called with public filter (no OAuth tokens)
        ArgumentCaptor<GitHubIssueSearchFilter> filterCaptor = ArgumentCaptor.forClass(GitHubIssueSearchFilter.class);
        verify(gitHubIssueProvider, times(1)).searchIssues(filterCaptor.capture());
        GitHubIssueSearchFilter passedFilter = filterCaptor.getValue();
        assertEquals("Java", passedFilter.getLanguage());
        assertTrue(passedFilter.getLabels().contains("good first issue"));
    }

    @Test
    @DisplayName("Regression: Filters out job advertisements and daily reading logs from discovery results")
    void discoverIssues_FiltersUnsuitableResults() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find Java Spring Boot issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto parsedFilter = new ParsedFilterDto(
                "Spring Boot", "Java", null, List.of(), "open", null, null, null, 1, 30
        );
        when(searchQueryParser.parse(any(), anyInt(), anyInt())).thenReturn(parsedFilter);

        GitHubIssue validIssue = new GitHubIssue();
        validIssue.setId(101L);
        validIssue.setNumber(1);
        validIssue.setTitle("Fix NullPointerException in Spring Boot REST controller");
        validIssue.setRepositoryFullName("spring-projects/spring-boot");
        validIssue.setState("open");

        GitHubIssue jobAd = new GitHubIssue();
        jobAd.setId(102L);
        jobAd.setNumber(2);
        jobAd.setTitle("Senior Java Developer Needed for Microservices");
        jobAd.setBody("Join our team, competitive salary and benefits");
        jobAd.setRepositoryFullName("hiring-org/jobs");
        jobAd.setState("open");

        GitHubIssue dailyLog = new GitHubIssue();
        dailyLog.setId(103L);
        dailyLog.setNumber(3);
        dailyLog.setTitle("An unrelated daily reading task");
        dailyLog.setBody("Daily study notes for clean architecture");
        dailyLog.setRepositoryFullName("learner/notes");
        dailyLog.setState("open");

        GitHubRepository javaRepo = new GitHubRepository();
        javaRepo.setLanguage("Java");
        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenReturn(javaRepo);

        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(validIssue, jobAd, dailyLog), 3L, 1, 30, false, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        // Job ad and daily reading task must be filtered out
        assertEquals(1, response.getResults().size());
        assertEquals(validIssue.getId(), response.getResults().get(0).getIssue().getId());
    }

    @Test
    @DisplayName("Regression: Excludes repositories whose verified language does NOT match requested language")
    void discoverIssues_ExcludesNonJavaRepositoriesWhenJavaRequested() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find Java backend issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto parsedFilter = new ParsedFilterDto(
                "backend", "Java", null, List.of(), "open", null, null, null, 1, 30
        );
        when(searchQueryParser.parse(any(), anyInt(), anyInt())).thenReturn(parsedFilter);

        GitHubIssue javaIssue = new GitHubIssue();
        javaIssue.setId(201L);
        javaIssue.setTitle("Improve database connection pool in Java");
        javaIssue.setRepositoryFullName("spring-projects/spring-boot");
        javaIssue.setState("open");

        GitHubIssue pythonIssue = new GitHubIssue();
        pythonIssue.setId(202L);
        pythonIssue.setTitle("Optimize backend routing service");
        pythonIssue.setRepositoryFullName("django/django");
        pythonIssue.setState("open");

        GitHubRepository javaRepo = new GitHubRepository();
        javaRepo.setLanguage("Java");
        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenReturn(javaRepo);

        GitHubRepository pythonRepo = new GitHubRepository();
        pythonRepo.setLanguage("Python");
        when(gitHubIssueProvider.getRepository("django", "django")).thenReturn(pythonRepo);

        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(javaIssue, pythonIssue), 2L, 1, 30, false, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        // Python repo issue must be excluded from Java search results
        assertEquals(1, response.getResults().size());
        assertEquals(javaIssue.getId(), response.getResults().get(0).getIssue().getId());
    }

    @Test
    @DisplayName("Regression: Preserves issue in multi-language repo if issue carries an explicit language label")
    void discoverIssues_MultiLanguageRepoWithLanguageLabel_Preserved() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find Java issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto parsedFilter = new ParsedFilterDto(
                null, "Java", null, List.of(), "open", null, null, null, 1, 30
        );
        when(searchQueryParser.parse(any(), anyInt(), anyInt())).thenReturn(parsedFilter);

        GitHubIssue polyglotIssue = new GitHubIssue();
        polyglotIssue.setId(401L);
        polyglotIssue.setTitle("Java API binding for image processing");
        polyglotIssue.setRepositoryFullName("opencv/opencv");
        polyglotIssue.setState("open");
        polyglotIssue.setLabels(List.of(new GitHubLabel(1L, "java", "blue", "Java Bindings")));

        GitHubRepository cppRepo = new GitHubRepository();
        cppRepo.setLanguage("C++");
        when(gitHubIssueProvider.getRepository("opencv", "opencv")).thenReturn(cppRepo);

        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(polyglotIssue), 1L, 1, 30, false, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        // Issue with explicit language label is preserved even if repo primary language is C++
        assertEquals(1, response.getResults().size());
        assertEquals(polyglotIssue.getId(), response.getResults().get(0).getIssue().getId());
    }

    @Test
    @DisplayName("Regression: Caches repository metadata so duplicate repositories are requested only once")
    void discoverIssues_CachesRepositoryMetadataAcrossIssues() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find Java Spring Boot issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto parsedFilter = new ParsedFilterDto(
                "Spring Boot", "Java", null, List.of(), "open", null, null, null, 1, 30
        );
        when(searchQueryParser.parse(any(), anyInt(), anyInt())).thenReturn(parsedFilter);

        GitHubIssue issue1 = new GitHubIssue();
        issue1.setId(301L);
        issue1.setTitle("Spring Boot fix A");
        issue1.setRepositoryFullName("spring-projects/spring-boot");
        issue1.setState("open");

        GitHubIssue issue2 = new GitHubIssue();
        issue2.setId(302L);
        issue2.setTitle("Spring Boot fix B");
        issue2.setRepositoryFullName("spring-projects/spring-boot");
        issue2.setState("open");

        GitHubRepository javaRepo = new GitHubRepository();
        javaRepo.setLanguage("Java");
        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot")).thenReturn(javaRepo);

        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(issue1, issue2), 2L, 1, 30, false, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        assertEquals(2, response.getResults().size());
        // Verify getRepository was called ONLY ONCE for the same repository
        verify(gitHubIssueProvider, times(1)).getRepository("spring-projects", "spring-boot");
    }

    @Test
    @DisplayName("discoverIssues throws UnauthenticatedException when user is not authenticated and never consumes quota or calls Groq")
    void discoverIssues_UnauthenticatedUser_ThrowsUnauthenticatedExceptionAndNeverConsumesQuotaOrCallsGroq() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues", 1, 30);

        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        UnauthenticatedException ex = assertThrows(UnauthenticatedException.class, () ->
                discoveryService.discoverIssues(request)
        );

        assertEquals("Authentication is required to access this resource", ex.getMessage());

        // Strictly verify that AI quota, Groq parser, and GitHub search are NEVER called
        verify(aiUsageService, never()).consumeQuota(any());
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("discoverIssues does NOT consume quota if prompt validation fails")
    void discoverIssues_InvalidPrompt_DoesNotConsumeQuota() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("ab"); // below 3 characters

        assertThrows(InvalidRequestException.class, () ->
                discoveryService.discoverIssues(request)
        );

        // Verify quota is NEVER touched
        verify(aiUsageService, never()).consumeQuota(any());
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("discoverIssues does NOT consume quota if Groq AI service is unavailable")
    void discoverIssues_GroqUnavailable_DoesNotConsumeQuota() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Python issues");

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(false);

        assertThrows(AiServiceUnavailableException.class, () ->
                discoveryService.discoverIssues(request)
        );

        // Verify quota was NOT consumed
        verify(aiUsageService, never()).consumeQuota(any());
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("discoverIssues aborts and throws AiQuotaExceededException when daily quota is exhausted")
    void discoverIssues_QuotaExhausted_ThrowsException() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find issues in react");

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenThrow(new AiQuotaExceededException("Daily AI search quota exhausted"));

        assertThrows(AiQuotaExceededException.class, () ->
                discoveryService.discoverIssues(request)
        );

        // Verify parser and GitHub provider were NEVER invoked after quota exhaustion
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("discoverIssues propagates GitHubRateLimitExceededException cleanly")
    void discoverIssues_GitHubRateLimit_PropagatesCleanly() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Go issues");

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto filter = new ParsedFilterDto(null, "Go", null, List.of(), "open", null, null, null, 1, 30);
        when(searchQueryParser.parse(any(), anyInt(), anyInt())).thenReturn(filter);

        GitHubRateLimit rateLimit = new GitHubRateLimit(60, 0, Instant.now().plusSeconds(60), null);
        when(gitHubIssueProvider.searchIssues(any()))
                .thenThrow(new GitHubRateLimitExceededException("Rate limit reached", rateLimit));

        assertThrows(GitHubRateLimitExceededException.class, () ->
                discoveryService.discoverIssues(request)
        );
    }

    @Test
    @DisplayName("browsePublicIssues successfully executes without AI, quota, or user session")
    void browsePublicIssues_Success_BypassesAiAndQuota() {
        com.osc.backend.discovery.dto.PublicDiscoveryRequestDto request =
                new com.osc.backend.discovery.dto.PublicDiscoveryRequestDto(
                        "virtual threads", "Java", "spring-projects/spring-boot",
                        List.of("good first issue"), "open", "updated", "desc", 1, 30
                );

        GitHubIssue issue = new GitHubIssue(
                101L, 1234, "Support virtual threads", "body", "https://github.com/spring-projects/spring-boot/issues/1234",
                "open", "spring-projects/spring-boot", List.of(new GitHubLabel(1L, "good first issue", "7057ff", "desc")),
                "user", "avatar", 5, Instant.now(), Instant.now(), null
        );

        when(gitHubIssueProvider.searchIssues(any(GitHubIssueSearchFilter.class)))
                .thenReturn(new GitHubPage<>(List.of(issue), 1L, 1, 30, false, false, null));

        when(gitHubIssueProvider.getRepository("spring-projects", "spring-boot"))
                .thenReturn(new GitHubRepository(1L, "spring-projects/spring-boot", "desc", "Java", 100, 10, "open", "https://github.com/spring-projects/spring-boot"));

        DiscoverySearchResponseDto response = discoveryService.browsePublicIssues(request, "192.168.1.10");

        assertNotNull(response);
        assertEquals("GUEST", response.getDiscoveryMode());
        assertNull(response.getPrompt());
        assertNull(response.getRemainingDailyQuota());
        assertEquals(1, response.getResults().size());

        // Guest matchScore MUST be null
        assertNull(response.getResults().get(0).getMatchScore());
        assertTrue(response.getResults().get(0).getMatchExplanation().startsWith("Factual match:"));

        // Strictly verify AI and user session were NEVER invoked
        verify(aiUsageService, never()).consumeQuota(any());
        verify(currentUserProvider, never()).getCurrentUserId();
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("browsePublicIssues throws InvalidRequestException when no search filter provided")
    void browsePublicIssues_NoFilter_ThrowsInvalidRequestException() {
        com.osc.backend.discovery.dto.PublicDiscoveryRequestDto emptyRequest =
                new com.osc.backend.discovery.dto.PublicDiscoveryRequestDto(
                        null, null, null, List.of(), "open", "updated", "desc", 1, 30
                );

        assertThrows(InvalidRequestException.class, () ->
                discoveryService.browsePublicIssues(emptyRequest, "192.168.1.10")
        );

        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("discoverIssues handles totalCount below 1,000 correctly")
    void discoverIssues_TotalCountBelow1000_CalculatesAccessibleCountAndTotalPages() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find Java issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto filter = new ParsedFilterDto(null, "Java", null, List.of(), "open", null, null, null, 1, 30);
        when(searchQueryParser.parse(any(), eq(1), eq(30))).thenReturn(filter);

        GitHubIssue issue = new GitHubIssue();
        issue.setId(101L);
        issue.setTitle("Java issue");
        issue.setRepositoryFullName("owner/repo");
        issue.setState("open");

        // totalCount = 250, perPage = 30 -> 9 total pages
        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(issue), 250L, 1, 30, true, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        assertNotNull(response);
        assertEquals(250L, response.getTotalCount(), "GitHub actual totalCount must be preserved");
        assertEquals(250L, response.getAccessibleCount(), "Accessible count must equal totalCount when below 1,000");
        assertEquals(9, response.getTotalPages(), "Total pages must be ceil(250 / 30) = 9");
        assertEquals(1, response.getPage());
        assertTrue(response.isHasNext());
        assertFalse(response.isHasPrevious());
    }

    @Test
    @DisplayName("discoverIssues handles totalCount above 1,000 by capping accessibleCount at 1,000 and preserving totalCount")
    void discoverIssues_TotalCountAbove1000_CapsAccessibleCountAt1000() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find good Java issues", 1, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto filter = new ParsedFilterDto(null, "Java", null, List.of(), "open", null, null, null, 1, 30);
        when(searchQueryParser.parse(any(), eq(1), eq(30))).thenReturn(filter);

        GitHubIssue issue = new GitHubIssue();
        issue.setId(102L);
        issue.setTitle("Java issue 2");
        issue.setRepositoryFullName("owner/repo");
        issue.setState("open");

        // totalCount = 15915, perPage = 30 -> accessibleCount = 1000, totalPages = 34
        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(issue), 15915L, 1, 30, true, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        assertNotNull(response);
        assertEquals(15915L, response.getTotalCount(), "GitHub actual totalCount of 15,915 must be preserved");
        assertEquals(1000L, response.getAccessibleCount(), "Accessible count must be capped at 1,000");
        assertEquals(34, response.getTotalPages(), "Total pages must be ceil(1000 / 30) = 34 instead of 531");
        assertEquals(1, response.getPage());
        assertTrue(response.isHasNext());
        assertFalse(response.isHasPrevious());
    }

    @Test
    @DisplayName("discoverIssues handles partial final page and disables hasNext on last accessible page")
    void discoverIssues_PartialFinalPage_LastAccessiblePageHasNoNext() {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find issues", 34, 30);

        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto filter = new ParsedFilterDto(null, "Java", null, List.of(), "open", null, null, null, 34, 30);
        when(searchQueryParser.parse(any(), eq(34), eq(30))).thenReturn(filter);

        GitHubIssue issue = new GitHubIssue();
        issue.setId(103L);
        issue.setTitle("Java issue final page");
        issue.setRepositoryFullName("owner/repo");
        issue.setState("open");

        // On page 34 with totalCount = 1000, remaining items = 10 (partial page)
        GitHubPage<GitHubIssue> page = new GitHubPage<>(List.of(issue), 1000L, 34, 30, false, true, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page);

        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);

        assertNotNull(response);
        assertEquals(1000L, response.getTotalCount());
        assertEquals(1000L, response.getAccessibleCount());
        assertEquals(34, response.getTotalPages());
        assertEquals(34, response.getPage());
        assertFalse(response.isHasNext(), "hasNext must be FALSE on the last accessible page");
        assertTrue(response.isHasPrevious(), "hasPrevious must be TRUE on page 34");
    }

    @Test
    @DisplayName("discoverIssues enforces boundary when requesting page 1 vs last accessible page")
    void discoverIssues_Page1AndLastAccessiblePage_NavigationStates() {
        // Test Page 1
        DiscoverySearchRequestDto req1 = new DiscoverySearchRequestDto("Find issues page 1", 1, 30);
        when(currentUserProvider.getCurrentUserId()).thenReturn(testUserId);
        when(searchQueryParser.isAvailable()).thenReturn(true);
        when(aiUsageService.consumeQuota(testUserId))
                .thenReturn(new AiQuotaResponseDto(20, 1, 19, LocalDate.now(), false));

        ParsedFilterDto filter1 = new ParsedFilterDto(null, "Java", null, List.of(), "open", null, null, null, 1, 30);
        when(searchQueryParser.parse(any(), eq(1), eq(30))).thenReturn(filter1);

        GitHubPage<GitHubIssue> page1 = new GitHubPage<>(List.of(new GitHubIssue()), 1000L, 1, 30, true, false, null);
        when(gitHubIssueProvider.searchIssues(any())).thenReturn(page1);

        DiscoverySearchResponseDto resp1 = discoveryService.discoverIssues(req1);
        assertEquals(1, resp1.getPage());
        assertFalse(resp1.isHasPrevious(), "Page 1 hasPrevious must be FALSE");
        assertTrue(resp1.isHasNext(), "Page 1 hasNext must be TRUE when totalPages > 1");
    }

    @Test
    @DisplayName("discoverIssues rejects page requests beyond accessible limit before consuming quota or calling Groq")
    void discoverIssues_PageBeyondAccessibleLimit_ThrowsInvalidRequestException() {
        // Page 35 with perPage 30: (35 - 1) * 30 = 1020 >= 1000
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find issues", 35, 30);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                discoveryService.discoverIssues(request)
        );

        assertTrue(ex.getMessage().contains("exceeds the maximum accessible GitHub Search window"));

        // Strictly verify that quota is NOT consumed and neither Groq nor GitHub were called
        verify(aiUsageService, never()).consumeQuota(any());
        verify(searchQueryParser, never()).parse(any(), anyInt(), anyInt());
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }

    @Test
    @DisplayName("browsePublicIssues rejects page requests beyond accessible limit")
    void browsePublicIssues_PageBeyondAccessibleLimit_ThrowsInvalidRequestException() {
        com.osc.backend.discovery.dto.PublicDiscoveryRequestDto req =
                new com.osc.backend.discovery.dto.PublicDiscoveryRequestDto(
                        "spring", "Java", null, List.of(), "open", "updated", "desc", 35, 30
                );

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                discoveryService.browsePublicIssues(req, "192.168.1.1")
        );

        assertTrue(ex.getMessage().contains("exceeds the maximum accessible GitHub Search window"));
        verify(gitHubIssueProvider, never()).searchIssues(any());
    }
}
