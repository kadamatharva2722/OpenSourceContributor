package com.osc.backend.contact.repository;

import com.osc.backend.contact.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    Optional<ContactMessage> findByReferenceCode(String referenceCode);

    long countByStatus(String status);

    @Query("SELECT c FROM ContactMessage c WHERE " +
           "(:status IS NULL OR :status = '' OR c.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.referenceCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ContactMessage> searchMessages(@Param("status") String status, @Param("search") String search, Pageable pageable);
}
