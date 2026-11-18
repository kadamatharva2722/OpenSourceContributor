package com.osc.backend.report.controller;

import com.osc.backend.common.security.ClientIpResolver;
import com.osc.backend.common.security.PublicSubmissionRateLimiter;
import com.osc.backend.report.dto.IssueReportRequestDto;
import com.osc.backend.report.dto.IssueReportResponseDto;
import com.osc.backend.report.service.IssueReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/issues")
public class IssueReportController {

    private final IssueReportService issueReportService;
    private final PublicSubmissionRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public IssueReportController(
            IssueReportService issueReportService,
            PublicSubmissionRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver
    ) {
        this.issueReportService = issueReportService;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public ResponseEntity<IssueReportResponseDto> submitIssueReport(
            @Valid @RequestBody IssueReportRequestDto request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIpResolver.resolvePublicSubmissionClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp);

        IssueReportResponseDto response = issueReportService.createReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
