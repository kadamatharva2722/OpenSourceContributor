package com.osc.backend.github.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing RFC 5988 Link headers returned by GitHub API for pagination.
 */
public final class GitHubLinkHeaderParser {

    private static final Pattern LINK_PATTERN = Pattern.compile("<([^>]+)>;\\s*rel=\"([^\"]+)\"");
    private static final Pattern PAGE_PARAM_PATTERN = Pattern.compile("[?&]page=(\\d+)");

    private GitHubLinkHeaderParser() {
    }

    /**
     * Parsed pagination metadata from RFC 5988 Link header.
     */
    public static class PaginationInfo {
        private final boolean hasNext;
        private final boolean hasPrev;
        private final Integer nextPage;
        private final Integer prevPage;
        private final Integer lastPage;
        private final Integer firstPage;
        private final Map<String, String> links;

        public PaginationInfo(boolean hasNext, boolean hasPrev, Integer nextPage,
                              Integer prevPage, Integer lastPage, Integer firstPage,
                              Map<String, String> links) {
            this.hasNext = hasNext;
            this.hasPrev = hasPrev;
            this.nextPage = nextPage;
            this.prevPage = prevPage;
            this.lastPage = lastPage;
            this.firstPage = firstPage;
            this.links = links != null ? Collections.unmodifiableMap(links) : Collections.emptyMap();
        }

        public boolean hasNext() {
            return hasNext;
        }

        public boolean hasPrev() {
            return hasPrev;
        }

        public Integer getNextPage() {
            return nextPage;
        }

        public Integer getPrevPage() {
            return prevPage;
        }

        public Integer getLastPage() {
            return lastPage;
        }

        public Integer getFirstPage() {
            return firstPage;
        }

        public String getUrl(String rel) {
            return links.get(rel);
        }
    }

    /**
     * Parses a raw Link header value into structured PaginationInfo.
     *
     * @param linkHeader the Link header string (may be null or empty)
     * @return populated PaginationInfo
     */
    public static PaginationInfo parse(String linkHeader) {
        if (linkHeader == null || linkHeader.trim().isEmpty()) {
            return new PaginationInfo(false, false, null, null, null, null, Collections.emptyMap());
        }

        Map<String, String> links = new HashMap<>();
        Matcher matcher = LINK_PATTERN.matcher(linkHeader);
        while (matcher.find()) {
            String url = matcher.group(1);
            String rel = matcher.group(2);
            links.put(rel, url);
        }

        boolean hasNext = links.containsKey("next");
        boolean hasPrev = links.containsKey("prev");
        Integer nextPage = extractPageFromUrl(links.get("next"));
        Integer prevPage = extractPageFromUrl(links.get("prev"));
        Integer lastPage = extractPageFromUrl(links.get("last"));
        Integer firstPage = extractPageFromUrl(links.get("first"));

        return new PaginationInfo(hasNext, hasPrev, nextPage, prevPage, lastPage, firstPage, links);
    }

    private static Integer extractPageFromUrl(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = PAGE_PARAM_PATTERN.matcher(url);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
