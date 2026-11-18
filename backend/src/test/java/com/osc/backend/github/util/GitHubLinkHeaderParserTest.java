package com.osc.backend.github.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubLinkHeaderParser Unit Tests")
class GitHubLinkHeaderParserTest {

    @Test
    @DisplayName("Parses standard GitHub RFC 5988 Link header")
    void parse_CompleteLinkHeader_ExtractsAllRels() {
        String linkHeader = "<https://api.github.com/search/issues?q=is%3Aissue&page=2&per_page=30>; rel=\"next\", " +
                "<https://api.github.com/search/issues?q=is%3Aissue&page=5&per_page=30>; rel=\"last\", " +
                "<https://api.github.com/search/issues?q=is%3Aissue&page=1&per_page=30>; rel=\"first\"";

        GitHubLinkHeaderParser.PaginationInfo info = GitHubLinkHeaderParser.parse(linkHeader);

        assertTrue(info.hasNext());
        assertFalse(info.hasPrev());
        assertEquals(2, info.getNextPage());
        assertEquals(5, info.getLastPage());
        assertEquals(1, info.getFirstPage());
        assertNull(info.getPrevPage());
        assertNotNull(info.getUrl("next"));
    }

    @Test
    @DisplayName("Parses middle page with next and prev")
    void parse_MiddlePage_ExtractsNextAndPrev() {
        String linkHeader = "<https://api.github.com/search/issues?page=3>; rel=\"next\", " +
                "<https://api.github.com/search/issues?page=1>; rel=\"prev\", " +
                "<https://api.github.com/search/issues?page=10>; rel=\"last\"";

        GitHubLinkHeaderParser.PaginationInfo info = GitHubLinkHeaderParser.parse(linkHeader);

        assertTrue(info.hasNext());
        assertTrue(info.hasPrev());
        assertEquals(3, info.getNextPage());
        assertEquals(1, info.getPrevPage());
        assertEquals(10, info.getLastPage());
    }

    @Test
    @DisplayName("Handles empty and null headers safely")
    void parse_EmptyOrNull_ReturnsSafeDefaults() {
        GitHubLinkHeaderParser.PaginationInfo infoNull = GitHubLinkHeaderParser.parse(null);
        assertFalse(infoNull.hasNext());
        assertFalse(infoNull.hasPrev());
        assertNull(infoNull.getNextPage());

        GitHubLinkHeaderParser.PaginationInfo infoEmpty = GitHubLinkHeaderParser.parse("   ");
        assertFalse(infoEmpty.hasNext());
        assertFalse(infoEmpty.hasPrev());
    }
}
