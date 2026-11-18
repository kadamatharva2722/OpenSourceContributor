package com.osc.backend.request.repository;

import com.osc.backend.request.entity.RepositoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryRequestRepository extends JpaRepository<RepositoryRequest, Long> {

    long countByStatus(String status);

    @Query("SELECT r FROM RepositoryRequest r WHERE " +
           "(:status IS NULL OR :status = '' OR r.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(r.repositoryName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.repositoryUrl) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.requestedByUsername) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RepositoryRequest> searchRequests(@Param("status") String status, @Param("search") String search, Pageable pageable);
}
