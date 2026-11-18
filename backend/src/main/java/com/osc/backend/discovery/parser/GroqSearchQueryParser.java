package com.osc.backend.discovery.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.discovery.dto.AiSearchCriteriaJson;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.exception.AiQueryParsingException;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.validator.SearchFilterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Groq-backed implementation of SearchQueryParser leveraging Spring AI ChatClient.
 * Translates natural language contributor prompts into structured GitHub issue search filters.
 */
@Component
public class GroqSearchQueryParser implements SearchQueryParser {

    private static final Logger log = LoggerFactory.getLogger(GroqSearchQueryParser.class);

    private static final String SYSTEM_PROMPT = """
            You are an expert GitHub search query interpreter for OpenSourceContributor.
            Your task is to translate natural language contributor queries into structured JSON criteria for the GitHub REST API.

            Strict rules:
            1. Output ONLY a valid JSON object matching the schema below. Do not wrap in markdown or include conversational text.
            2. Never invent repository names, issue numbers, URLs, or fake counts.
            3. Only extract a repository if the user explicitly specifies one in owner/repo or project format.
            4. Only extract a programming language if explicitly mentioned or strongly implied by technologies (e.g. Java, Python, TypeScript, Go, Rust, C++).
            5. Extract relevant labels when implied (e.g., "good first issue", "help wanted", "documentation", "bug").
            6. If the user mentions beginner, easy, starter, or newcomer tasks, set difficulty to "beginner" and add "good first issue" to labels.
            7. Set state to "open" unless the user explicitly asks for closed issues.

            JSON schema:
            {
              "keywords": "search keywords or null",
              "language": "programming language or null",
              "repository": "owner/name or null",
              "labels": ["label1", "label2"],
              "state": "open",
              "sort": "updated or comments or created or reactions or null",
              "order": "desc or asc or null",
              "difficulty": "beginner or intermediate or advanced or null"
            }
            """;

    private final ChatClient chatClient;
    private final SearchFilterValidator validator;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final boolean groqEnabled;

    public GroqSearchQueryParser(
            @Autowired(required = false) ChatClient.Builder chatClientBuilder,
            SearchFilterValidator validator,
            ObjectMapper objectMapper,
            @Value("${spring.ai.openai.api-key:disabled}") String apiKey,
            @Value("${app.groq.enabled:true}") boolean groqEnabled
    ) {
        this.chatClient = chatClientBuilder != null ? chatClientBuilder.build() : null;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.groqEnabled = groqEnabled;
    }

    @Override
    public boolean isAvailable() {
        return groqEnabled &&
                apiKey != null &&
                !apiKey.isBlank() &&
                !"disabled".equalsIgnoreCase(apiKey.trim()) &&
                chatClient != null;
    }

    @Override
    public ParsedFilterDto parse(String prompt, int page, int perPage) {
        validator.validatePrompt(prompt);

        if (!isAvailable()) {
            throw new AiServiceUnavailableException("AI discovery service is currently unavailable. Groq API is not configured or disabled.");
        }

        log.debug("Invoking Groq AI for query interpretation: '{}'", prompt);

        String rawResponse;
        try {
            rawResponse = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("Failed to invoke Groq AI: {}", ex.getMessage(), ex);
            throw new AiServiceUnavailableException("Failed to communicate with AI discovery engine. Please try again later.", ex);
        }

        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new AiQueryParsingException("AI discovery engine returned an empty response");
        }

        String cleanedJson = cleanJson(rawResponse);
        AiSearchCriteriaJson criteriaJson;
        try {
            criteriaJson = objectMapper.readValue(cleanedJson, AiSearchCriteriaJson.class);
        } catch (JsonProcessingException ex) {
            log.warn("Malformed JSON received from AI model: '{}'", cleanedJson, ex);
            throw new AiQueryParsingException("Failed to interpret search prompt: AI returned invalid criteria format", ex);
        }

        return validator.validateAndSanitize(criteriaJson, page, perPage);
    }

    private String cleanJson(String raw) {
        String trimmed = raw.trim();
        // Remove markdown backticks if present
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // Extract substring between outermost braces if extra text exists
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }
}
