package com.osc.backend.discovery.filter;

import com.osc.backend.github.model.GitHubIssue;
import com.osc.backend.github.model.GitHubLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filters out issues unsuitable for open-source contributor discovery.
 *
 * <p>Identifies and excludes:
 * <ul>
 *   <li>Job advertisements and recruitment postings.</li>
 *   <li>Daily reading logs, study check-in journals, and personal reading tasks.</li>
 *   <li>General service announcements, downtime notices, and non-actionable placeholders.</li>
 * </ul>
 *
 * <p>Safely preserves legitimate technical contribution issues:
 * <ul>
 *   <li>Bug reports and bug fixes.</li>
 *   <li>Documentation updates and guides.</li>
 *   <li>Unit testing and test coverage improvements.</li>
 *   <li>Feature requests and enhancements.</li>
 *   <li>Technical tasks with time references (e.g. "token expires in 1 day", "reading task queue").</li>
 * </ul>
 */
@Component
public class UnsuitableIssueFilter {

    private static final Logger log = LoggerFactory.getLogger(UnsuitableIssueFilter.class);

    private static final Set<String> RECRUITMENT_LABELS = Set.of(
            "job",
            "jobs",
            "hiring",
            "recruitment",
            "careers",
            "vacancy",
            "vacancies",
            "job-posting",
            "job-opening"
    );

    private static final Set<String> DAILY_LOG_LABELS = Set.of(
            "daily-reading",
            "daily-task",
            "study-log",
            "reading-notes",
            "daily-checkin",
            "checkin",
            "reading-log",
            "learning-log"
    );

    // Job and recruitment title patterns (targeted at position openings and hiring announcements)
    private static final Pattern JOB_AD_TITLE_PATTERN = Pattern.compile(
            "(?i).*\\b(we\\s+are\\s+hiring|we're\\s+hiring|join\\s+our\\s+team|job\\s+opening|" +
                    "job\\s+opportunity|job\\s+posting|career\\s+opportunity|hiring\\s+now|" +
                    "looking\\s+for\\s+(a\\s+)?(developer|engineer|architect|contractor)|" +
                    "wanted\\s*:\\s*(senior|junior|lead|principal|backend|frontend|java|python|react)|" +
                    "(senior|junior|lead|principal|staff|full[-\\s]?stack|backend|frontend|java|python|react|golang|node)" +
                    "\\s+(developer|engineer|architect|programmer|consultant)\\s+(job|opening|position|role|vacancy|wanted|needed|advertisement)).*",
            Pattern.CASE_INSENSITIVE
    );

    // Day-numbered study logs anchored to the beginning or standard tag formats: e.g. "Day 1:", "[Day 5] -", "100 Days of Code"
    private static final Pattern DAY_NUMBERED_STUDY_LOG_PATTERN = Pattern.compile(
            "(?i)^(\\[?\\s*day\\s*\\d+[\\s\\:\\-\\]]|100\\s*days\\s*of\\s*code).*",
            Pattern.CASE_INSENSITIVE
    );

    // Daily reading logs and study check-in titles (unrelated to codebase development)
    private static final Pattern STUDY_OR_READING_LOG_PATTERN = Pattern.compile(
            "(?i).*\\b(daily\\s+(reading|study|check-?in|journal|learning)|(reading|study)\\s+(log|journal|notes|plan)|reading\\s+task\\s*[:\\-]|leetcode\\s*(daily|practice))\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    // General announcement patterns
    private static final Pattern ANNOUNCEMENT_TITLE_PATTERN = Pattern.compile(
            "(?i)^(\\[(announcement|notice|psa|maintenance|downtime)\\]|" +
                    "(announcement|notice|maintenance|downtime)\\s*:).*",
            Pattern.CASE_INSENSITIVE
    );

    // Body phrases strongly indicating recruitment postings
    private static final Pattern JOB_AD_BODY_PATTERN = Pattern.compile(
            "(?i).*\\b(submit\\s+your\\s+resume|send\\s+your\\s+resume|send\\s+your\\s+cv|" +
                    "apply\\s+at\\s+https?://|apply\\s+via\\s+https?://|job\\s+description\\s*:|" +
                    "compensation\\s*:\\s*\\$|salary\\s*:\\s*\\$).*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Determines whether a GitHub issue is an actionable and eligible open-source contribution task.
     *
     * @param issue the GitHub issue to check
     * @return true if eligible; false if identified as a job ad, study log, or non-actionable post
     */
    public boolean isEligible(GitHubIssue issue) {
        if (issue == null) {
            return false;
        }

        String title = issue.getTitle() != null ? issue.getTitle().trim() : "";
        String body = issue.getBody() != null ? issue.getBody().trim() : "";

        // 1. Check recruitment / job advertisement labels
        if (hasAnyLabel(issue, RECRUITMENT_LABELS)) {
            log.debug("Issue #{} excluded: carries recruitment label", issue.getNumber());
            return false;
        }

        // 2. Check daily reading log / study journal labels
        if (hasAnyLabel(issue, DAILY_LOG_LABELS)) {
            log.debug("Issue #{} excluded: carries daily log label", issue.getNumber());
            return false;
        }

        // 3. Check job advertisement title patterns
        if (!title.isEmpty() && JOB_AD_TITLE_PATTERN.matcher(title).matches()) {
            log.debug("Issue #{} excluded: title matches job advertisement pattern: '{}'", issue.getNumber(), title);
            return false;
        }

        // 4. Check day-numbered study journals (anchored to beginning)
        if (!title.isEmpty() && DAY_NUMBERED_STUDY_LOG_PATTERN.matcher(title).matches()) {
            log.debug("Issue #{} excluded: title matches day-numbered study journal pattern: '{}'", issue.getNumber(), title);
            return false;
        }

        // 5. Check daily reading log / study check-in titles
        if (!title.isEmpty() && STUDY_OR_READING_LOG_PATTERN.matcher(title).matches()) {
            log.debug("Issue #{} excluded: title matches study/reading log pattern: '{}'", issue.getNumber(), title);
            return false;
        }

        // 6. Check announcement patterns
        if (!title.isEmpty() && ANNOUNCEMENT_TITLE_PATTERN.matcher(title).matches()) {
            log.debug("Issue #{} excluded: title matches announcement pattern: '{}'", issue.getNumber(), title);
            return false;
        }

        // 7. Check body for recruitment call-to-actions when title suggests a position
        if (!body.isEmpty() && JOB_AD_BODY_PATTERN.matcher(body).matches()) {
            log.debug("Issue #{} excluded: body contains recruitment application instructions", issue.getNumber());
            return false;
        }

        // 8. Check for non-actionable placeholder/test spam
        if (isPlaceholderOrSpam(title, body)) {
            log.debug("Issue #{} excluded: placeholder or spam issue", issue.getNumber());
            return false;
        }

        return true;
    }

    private boolean hasAnyLabel(GitHubIssue issue, Set<String> targetLabels) {
        List<GitHubLabel> labels = issue.getLabels();
        if (labels == null || labels.isEmpty()) {
            return false;
        }

        for (GitHubLabel label : labels) {
            if (label != null && label.getName() != null) {
                String clean = label.getName().trim().toLowerCase().replaceAll("[\\s_]+", "-");
                if (targetLabels.contains(clean)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlaceholderOrSpam(String title, String body) {
        String lower = title.toLowerCase().trim();
        return (lower.equals("test") || lower.equals("testing") || lower.equals("asdf") ||
                lower.equals("hello world") || lower.equals("test issue")) &&
                (body == null || body.length() < 10);
    }
}
