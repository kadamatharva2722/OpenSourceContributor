package com.osc.backend.github.provider;

import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubIssueSearchFilter;
import com.osc.backend.github.model.GitHubPage;
import com.osc.backend.github.model.GitHubRepository;

/**
 * Contract for discovering public GitHub issues and retrieving repository details.
 * Completely decoupled from HTTP implementation, authentication, and external protocols.
 */
public interface GitHubIssueProvider {

    /**
     * Searches open GitHub issues using the provided filter criteria.
     * Excludes Pull Requests from the returned results.
     *
     * @param filter Search criteria, labels, repository, and pagination
     * @return Paginated page of domain issues with rate limit metadata
     */
    GitHubPage<GitHubIssue> searchIssues(GitHubIssueSearchFilter filter);

    /**
     * Retrieves details for a specific repository.
     *
     * @param owner Repository owner/organization (e.g. "facebook")
     * @param repo  Repository name (e.g. "react")
     * @return Domain repository details
     */
    GitHubRepository getRepository(String owner, String repo);

    /**
     * Retrieves a single issue by repository and issue number.
     *
     * @param owner       Repository owner
     * @param repo        Repository name
     * @param issueNumber Issue number
     * @return Domain issue details
     */
    GitHubIssue getIssue(String owner, String repo, int issueNumber);
}
