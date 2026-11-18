package com.osc.backend.discovery.parser;

import com.osc.backend.discovery.dto.ParsedFilterDto;

/**
 * Contract for interpreting natural language search queries into structured GitHub search filters.
 */
public interface SearchQueryParser {

    /**
     * Interprets a user's natural language query into validated structured criteria.
     *
     * @param prompt  natural language query from contributor
     * @param page    requested page number
     * @param perPage requested items per page
     * @return validated and sanitized ParsedFilterDto
     */
    ParsedFilterDto parse(String prompt, int page, int perPage);

    /**
     * Indicates whether the AI parsing backend is configured and ready.
     *
     * @return true if Groq/LLM integration is available
     */
    boolean isAvailable();
}
