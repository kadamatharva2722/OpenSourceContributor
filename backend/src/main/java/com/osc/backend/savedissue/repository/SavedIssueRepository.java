package com.osc.backend.savedissue.repository;

import com.osc.backend.savedissue.entity.SavedIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link SavedIssue} entity.
 */
@Repository
public interface SavedIssueRepository extends JpaRepository<SavedIssue, Long> {

    List<SavedIssue> findByUserId(Long userId);

    Optional<SavedIssue> findByIdAndUserId(Long id, Long userId);

    Optional<SavedIssue> findByUserIdAndGithubIssueId(Long userId, Long githubIssueId);

    boolean existsByUserIdAndGithubIssueId(Long userId, Long githubIssueId);

    void deleteByUserIdAndGithubIssueId(Long userId, Long githubIssueId);

    @Modifying
    @Query("DELETE FROM SavedIssue si WHERE si.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
