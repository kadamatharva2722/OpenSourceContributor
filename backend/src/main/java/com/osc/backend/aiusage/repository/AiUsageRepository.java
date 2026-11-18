package com.osc.backend.aiusage.repository;

import com.osc.backend.aiusage.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link AiUsage} entity.
 */
@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    Optional<AiUsage> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);

    @Modifying
    @Query("DELETE FROM AiUsage au WHERE au.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Atomically reserves one AI search quota unit using PostgreSQL ON CONFLICT.
     * If no row exists for today, creates it with request_count = 1 (affected = 1).
     * If row exists and request_count < dailyLimit, increments by 1 (affected = 1).
     * If row exists and request_count >= dailyLimit, WHERE condition fails and update is skipped (affected = 0).
     *
     * @param userId     target user ID
     * @param usageDate  calendar date for the quota window
     * @param dailyLimit configured daily limit
     * @return 1 if quota was reserved, 0 if quota was exhausted
     */
    @Modifying
    @Query(value = """
        INSERT INTO ai_usage (user_id, usage_date, request_count, created_at, updated_at)
        VALUES (:userId, :usageDate, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (user_id, usage_date)
        DO UPDATE SET
            request_count = ai_usage.request_count + 1,
            updated_at = CURRENT_TIMESTAMP
        WHERE ai_usage.request_count < :dailyLimit
        """, nativeQuery = true)
    int reserveQuotaAtomic(
            @Param("userId") Long userId,
            @Param("usageDate") LocalDate usageDate,
            @Param("dailyLimit") int dailyLimit
    );
}
