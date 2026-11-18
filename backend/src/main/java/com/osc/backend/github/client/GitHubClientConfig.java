package com.osc.backend.github.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Dedicated Spring configuration for GitHub REST API HTTP client.
 *
 * <p>CRITICAL SECURITY REQUIREMENT:
 * This client is strictly unauthenticated and communicates ONLY with GitHub's public REST API.
 * NEVER attach OAuth tokens, user credentials, or Authorization headers to this client.
 */
@Configuration
public class GitHubClientConfig {

    public static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    public static final String GITHUB_API_VERSION = "2022-11-28";
    public static final String GITHUB_MEDIA_TYPE = "application/vnd.github+json";

    @Bean("gitHubRestClient")
    public RestClient gitHubRestClient(GitHubClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeout()));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, GITHUB_MEDIA_TYPE)
                .defaultHeader(GITHUB_API_VERSION_HEADER, GITHUB_API_VERSION)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .build();
    }
}
