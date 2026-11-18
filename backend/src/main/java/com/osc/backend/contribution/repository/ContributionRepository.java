package com.osc.backend.contribution.repository;

import com.osc.backend.contribution.entity.Contribution;
import com.osc.backend.contribution.entity.ContributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Contribution} entity.
 */
@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    List<Contribution> findByUserId(Long userId);

    List<Contribution> findByUserIdAndStatus(Long userId, ContributionStatus status);

    Optional<Contribution> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM Contribution c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
