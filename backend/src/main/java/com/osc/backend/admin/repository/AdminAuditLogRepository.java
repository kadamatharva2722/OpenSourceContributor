package com.osc.backend.admin.repository;

import com.osc.backend.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT a FROM AdminAuditLog a WHERE " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.adminUsername) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.targetType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.targetId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.details) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminAuditLog> searchLogs(@Param("action") String action, @Param("search") String search, Pageable pageable);
}
