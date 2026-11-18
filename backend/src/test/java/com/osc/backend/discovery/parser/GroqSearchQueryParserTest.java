package com.osc.backend.discovery.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.exception.AiQueryParsingException;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.validator.SearchFilterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroqSearchQueryParser Unit Tests")
class GroqSearchQueryParserTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private SearchFilterValidator validator;
    private ObjectMapper objectMapper;
    private GroqSearchQueryParser parser;

    @BeforeEach
    void setUp() {
        validator = new SearchFilterValidator();
        objectMapper = new ObjectMapper();

        when(chatClientBuilder.build()).thenReturn(chatClient);
        parser = new GroqSearchQueryParser(chatClientBuilder, validator, objectMapper, "gsk_test_key", true);
    }

    @Test
    @DisplayName("isAvailable returns true when API key is configured and client exists")
    void isAvailable_Configured_ReturnsTrue() {
        assertTrue(parser.isAvailable());
    }

    @Test
    @DisplayName("isAvailable returns false when API key is default 'disabled' or blank")
    void isAvailable_DisabledOrBlankKey_ReturnsFalse() {
        GroqSearchQueryParser disabledParser = new GroqSearchQueryParser(chatClientBuilder, validator, objectMapper, "disabled", true);
        assertFalse(disabledParser.isAvailable());

        GroqSearchQueryParser blankParser = new GroqSearchQueryParser(chatClientBuilder, validator, objectMapper, "   ", true);
        assertFalse(blankParser.isAvailable());

        GroqSearchQueryParser toggleDisabledParser = new GroqSearchQueryParser(chatClientBuilder, validator, objectMapper, "gsk_key", false);
        assertFalse(toggleDisabledParser.isAvailable());
    }

    @Test
    @DisplayName("parse successfully interprets valid AI JSON response")
    void parse_ValidJsonResponse_ReturnsParsedFilter() {
        String jsonOutput = """
                {
                  "keywords": "memory leak",
                  "language": "Java",
                  "repository": "facebook/react",
                  "labels": ["bug", "good first issue"],
                  "state": "open",
                  "sort": "updated",
                  "order": "desc",
                  "difficulty": "beginner"
                }
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(jsonOutput);

        ParsedFilterDto filter = parser.parse("Find beginner Java memory leak bugs in facebook/react", 1, 30);

        assertNotNull(filter);
        assertEquals("memory leak", filter.getKeywords());
        assertEquals("Java", filter.getLanguage());
        assertEquals("facebook/react", filter.getRepository());
        assertTrue(filter.getLabels().contains("bug"));
        assertTrue(filter.getLabels().contains("good first issue"));
        assertEquals("open", filter.getState());
        assertEquals("updated", filter.getSort());
        assertEquals("desc", filter.getOrder());
        assertEquals("beginner", filter.getDifficultyPreference());
    }

    @Test
    @DisplayName("parse strips markdown code fences from AI output")
    void parse_MarkdownCodeFence_StripsAndParses() {
        String jsonWithFence = """
                ```json
                {
                  "language": "Python",
                  "labels": ["good first issue"],
                  "difficulty": "beginner"
                }
                ```
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(jsonWithFence);

        ParsedFilterDto filter = parser.parse("Find beginner Python issues", 1, 30);

        assertNotNull(filter);
        assertEquals("Python", filter.getLanguage());
        assertTrue(filter.getLabels().contains("good first issue"));
        assertEquals("beginner", filter.getDifficultyPreference());
    }

    @Test
    @DisplayName("parse throws AiQueryParsingException when AI returns malformed JSON")
    void parse_MalformedJson_ThrowsAiQueryParsingException() {
        String badJson = "{ invalid json here...";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(badJson);

        assertThrows(AiQueryParsingException.class, () ->
                parser.parse("Find issues", 1, 30)
        );
    }

    @Test
    @DisplayName("parse throws AiQueryParsingException when AI returns empty content")
    void parse_EmptyContent_ThrowsAiQueryParsingException() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("   ");

        assertThrows(AiQueryParsingException.class, () ->
                parser.parse("Find issues", 1, 30)
        );
    }

    @Test
    @DisplayName("parse throws AiServiceUnavailableException when Groq is not configured")
    void parse_WhenNotAvailable_ThrowsAiServiceUnavailableException() {
        GroqSearchQueryParser unavailParser = new GroqSearchQueryParser(null, validator, objectMapper, "disabled", true);

        assertThrows(AiServiceUnavailableException.class, () ->
                unavailParser.parse("Find beginner Java issues", 1, 30)
        );
    }

    @Test
    @DisplayName("parse throws InvalidRequestException when prompt is invalid")
    void parse_InvalidPrompt_ThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () ->
                parser.parse("ab", 1, 30)
        );
    }
}
