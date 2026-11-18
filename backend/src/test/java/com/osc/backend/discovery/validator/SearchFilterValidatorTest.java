package com.osc.backend.discovery.validator;

import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.discovery.dto.AiSearchCriteriaJson;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchFilterValidator Unit Tests")
class SearchFilterValidatorTest {

    private SearchFilterValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SearchFilterValidator();
    }

    @Test
    @DisplayName("validatePrompt accepts valid prompt within boundaries")
    void validatePrompt_ValidLength_Passes() {
        assertDoesNotThrow(() -> validator.validatePrompt("Find good first Java issues"));
    }

    @Test
    @DisplayName("validatePrompt rejects blank or null prompt")
    void validatePrompt_NullOrBlank_ThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> validator.validatePrompt(null));
        assertThrows(InvalidRequestException.class, () -> validator.validatePrompt("   "));
    }

    @Test
    @DisplayName("validatePrompt rejects prompts that are too short or too long")
    void validatePrompt_OutOfBounds_ThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> validator.validatePrompt("ab"));
        String tooLong = "a".repeat(501);
        assertThrows(InvalidRequestException.class, () -> validator.validatePrompt(tooLong));
    }

    @Test
    @DisplayName("validateAndSanitize enforces pagination boundaries")
    void validateAndSanitize_PaginationBounds_Enforced() {
        AiSearchCriteriaJson criteria = new AiSearchCriteriaJson();
        ParsedFilterDto result = validator.validateAndSanitize(criteria, -5, 200);

        assertEquals(1, result.getPage());
        assertEquals(100, result.getPerPage());
    }

    @Test
    @DisplayName("validateAndSanitize sanitizes keywords and prevents query injection")
    void validateAndSanitize_KeywordInjection_Sanitized() {
        AiSearchCriteriaJson criteria = new AiSearchCriteriaJson();
        criteria.setKeywords("is:issue\nrepo:evil/hack:test\"quote\"");

        ParsedFilterDto result = validator.validateAndSanitize(criteria, 1, 30);

        assertNotNull(result.getKeywords());
        assertFalse(result.getKeywords().contains(":"));
        assertFalse(result.getKeywords().contains("\n"));
        assertFalse(result.getKeywords().contains("\""));
    }

    @Test
    @DisplayName("validateAndSanitize accepts valid programming language and rejects dangerous characters")
    void validateAndSanitize_LanguageValidation() {
        AiSearchCriteriaJson validJava = new AiSearchCriteriaJson();
        validJava.setLanguage("Java");
        assertEquals("Java", validator.validateAndSanitize(validJava, 1, 30).getLanguage());

        AiSearchCriteriaJson validCpp = new AiSearchCriteriaJson();
        validCpp.setLanguage("C++");
        assertEquals("C++", validator.validateAndSanitize(validCpp, 1, 30).getLanguage());

        AiSearchCriteriaJson invalidLang = new AiSearchCriteriaJson();
        invalidLang.setLanguage("Java:is:open");
        assertNull(validator.validateAndSanitize(invalidLang, 1, 30).getLanguage());
    }

    @Test
    @DisplayName("validateAndSanitize validates repository in owner/repo format")
    void validateAndSanitize_RepositoryValidation() {
        AiSearchCriteriaJson validRepo = new AiSearchCriteriaJson();
        validRepo.setRepository("facebook/react");
        assertEquals("facebook/react", validator.validateAndSanitize(validRepo, 1, 30).getRepository());

        AiSearchCriteriaJson invalidRepo = new AiSearchCriteriaJson();
        invalidRepo.setRepository("invalid-repo-without-slash");
        assertNull(validator.validateAndSanitize(invalidRepo, 1, 30).getRepository());
    }

    @Test
    @DisplayName("validateAndSanitize sanitizes labels and caps at maximum 5")
    void validateAndSanitize_LabelsCappedAndCleaned() {
        AiSearchCriteriaJson criteria = new AiSearchCriteriaJson();
        criteria.setLabels(List.of("good first issue", "help:wanted", "bug", "enhancement", "docs", "extra-label-6"));

        ParsedFilterDto result = validator.validateAndSanitize(criteria, 1, 30);

        assertTrue(result.getLabels().size() <= 5);
        assertFalse(result.getLabels().contains("help:wanted"));
        assertTrue(result.getLabels().contains("helpwanted"));
    }

    @Test
    @DisplayName("validateAndSanitize beginner difficulty adds 'good first issue' label")
    void validateAndSanitize_BeginnerDifficulty_AddsGoodFirstIssueLabel() {
        AiSearchCriteriaJson criteria = new AiSearchCriteriaJson();
        criteria.setDifficulty("beginner");

        ParsedFilterDto result = validator.validateAndSanitize(criteria, 1, 30);

        assertEquals("beginner", result.getDifficultyPreference());
        assertTrue(result.getLabels().contains("good first issue"));
    }

    @Test
    @DisplayName("toGitHubFilter converts ParsedFilterDto to GitHubIssueSearchFilter correctly")
    void toGitHubFilter_Conversion_Accurate() {
        ParsedFilterDto dto = new ParsedFilterDto(
                "memory leak", "Java", "facebook/react", List.of("bug"),
                "open", "updated", "desc", "beginner", 2, 50
        );

        GitHubIssueSearchFilter filter = validator.toGitHubFilter(dto);

        assertEquals(2, filter.getPage());
        assertEquals(50, filter.getPerPage());
        assertEquals("open", filter.getState());
        assertEquals("Java", filter.getLanguage());
        assertEquals("facebook/react", filter.getRepository());
        assertEquals("updated", filter.getSort());
        assertEquals("desc", filter.getOrder());
        assertTrue(filter.getLabels().contains("bug"));
        assertEquals("memory leak", filter.getQuery());
    }

    @Test
    @DisplayName("validatePagination accepts valid page numbers within the 1,000 result limit")
    void validatePagination_ValidPages_Passes() {
        assertDoesNotThrow(() -> validator.validatePagination(1, 30));
        assertDoesNotThrow(() -> validator.validatePagination(34, 30)); // 33 * 30 = 990 < 1000
        assertDoesNotThrow(() -> validator.validatePagination(10, 100)); // 9 * 100 = 900 < 1000
        assertDoesNotThrow(() -> validator.validatePagination(1, 100));
    }

    @Test
    @DisplayName("validatePagination rejects page numbers that exceed the accessible 1,000 search result window")
    void validatePagination_ExceedsGitHubLimit_ThrowsInvalidRequestException() {
        // For perPage = 30, max accessible page is 34. Page 35 offset is 34 * 30 = 1020 >= 1000.
        InvalidRequestException ex30 = assertThrows(InvalidRequestException.class, () ->
                validator.validatePagination(35, 30)
        );
        assertTrue(ex30.getMessage().contains("exceeds the maximum accessible GitHub Search window"));

        // For perPage = 100, max accessible page is 10. Page 11 offset is 10 * 100 = 1000 >= 1000.
        InvalidRequestException ex100 = assertThrows(InvalidRequestException.class, () ->
                validator.validatePagination(11, 100)
        );
        assertTrue(ex100.getMessage().contains("exceeds the maximum accessible GitHub Search window"));
    }

    @Test
    @DisplayName("validatePagination rejects invalid non-positive page numbers or invalid sizes")
    void validatePagination_InvalidPageOrSize_ThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> validator.validatePagination(0, 30));
        assertThrows(InvalidRequestException.class, () -> validator.validatePagination(-2, 30));
        assertThrows(InvalidRequestException.class, () -> validator.validatePagination(1, 0));
        assertThrows(InvalidRequestException.class, () -> validator.validatePagination(1, 101));
    }

    @Test
    @DisplayName("validateAndSanitizePublic rejects page requests exceeding accessible window")
    void validateAndSanitizePublic_ExceedsAccessibleWindow_ThrowsInvalidRequestException() {
        com.osc.backend.discovery.dto.PublicDiscoveryRequestDto req =
                new com.osc.backend.discovery.dto.PublicDiscoveryRequestDto(
                        "test", "Java", null, List.of(), "open", "updated", "desc", 35, 30
                );

        assertThrows(InvalidRequestException.class, () ->
                validator.validateAndSanitizePublic(req)
        );
    }
}
