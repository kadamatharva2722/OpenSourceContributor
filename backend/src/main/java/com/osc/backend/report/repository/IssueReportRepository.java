package com.osc.backend.report.repository;

import com.osc.backend.report.entity.IssueReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {

    long countByStatus(String status);

    @Query("SELECT r FROM IssueReport r WHERE " +
           "(:status IS NULL OR :status = '' OR r.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(r.repositoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.reason) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.reportedByUsername) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<IssueReport> searchReports(@Param("status") String status, @Param("search") String search, Pageable pageable);
}
