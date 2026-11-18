package com.osc.backend.userpreference.repository;

import com.osc.backend.userpreference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link UserPreference} entity.
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
