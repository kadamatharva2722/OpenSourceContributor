package com.osc.backend.report.service;

import com.osc.backend.report.dto.IssueReportRequestDto;
import com.osc.backend.report.dto.IssueReportResponseDto;
import com.osc.backend.report.entity.IssueReport;
import com.osc.backend.report.repository.IssueReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueReportService {

    private static final Logger log = LoggerFactory.getLogger(IssueReportService.class);

    private final IssueReportRepository issueReportRepository;

    public IssueReportService(IssueReportRepository issueReportRepository) {
        this.issueReportRepository = issueReportRepository;
    }

    @Transactional
    public IssueReportResponseDto createReport(IssueReportRequestDto request) {
        IssueReport report = new IssueReport(
                request.getRepositoryName().trim(),
                request.getIssueUrl().trim(),
                request.getIssueNumber(),
                request.getReason().trim(),
                request.getDetails() != null ? request.getDetails().trim() : null,
                request.getReportedByUsername() != null ? request.getReportedByUsername().trim() : "anonymous"
        );

        IssueReport saved = issueReportRepository.save(report);
        log.info("Issue report created for {} by {}", saved.getRepositoryName(), saved.getReportedByUsername());
        return IssueReportResponseDto.fromEntity(saved);
    }
}
