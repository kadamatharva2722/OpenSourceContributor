package com.osc.backend.aiusage.service;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.entity.AiUsage;
import com.osc.backend.aiusage.repository.AiUsageRepository;
import com.osc.backend.common.exception.AiQuotaExceededException;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Service managing daily AI discovery request quota per registered contributor.
 * Enforces the policy of 20 searches per user per day atomically using PostgreSQL ON CONFLICT.
 */
@Service
@Transactional(readOnly = true)
public class AiUsageService {

    private final AiUsageRepository aiUsageRepository;
    private final UserService userService;
    private final int dailyLimit;
    private final ZoneId zoneId;

    public AiUsageService(
            AiUsageRepository aiUsageRepository,
            UserService userService,
            @Value("${app.ai.daily-limit.registered:20}") int dailyLimit,
            @Value("${app.ai.quota-timezone:UTC}") String quotaTimezone
    ) {
        this.aiUsageRepository = aiUsageRepository;
        this.userService = userService;
        this.dailyLimit = dailyLimit;
        this.zoneId = ZoneId.of(quotaTimezone);
    }

    /**
     * Retrieves current quota consumption metrics for today.
     */
    public AiQuotaResponseDto getQuotaInfo(Long userId) {
        userService.getUserEntity(userId); // Verifies user exists
        LocalDate today = getCurrentDate();

        int usedToday = aiUsageRepository.findByUserIdAndUsageDate(userId, today)
                .map(AiUsage::getRequestCount)
                .orElse(0);

        int remaining = Math.max(0, dailyLimit - usedToday);
        boolean exhausted = remaining == 0;

        return new AiQuotaResponseDto(dailyLimit, usedToday, remaining, today, exhausted);
    }

    /**
     * Returns the total count of AI queries executed today by the user.
     */
    public int getTodayUsage(Long userId) {
        userService.getUserEntity(userId); // Verifies user exists
        LocalDate today = getCurrentDate();
        return aiUsageRepository.findByUserIdAndUsageDate(userId, today)
                .map(AiUsage::getRequestCount)
                .orElse(0);
    }

    /**
     * Returns the remaining AI searches available to the user today.
     */
    public int getRemainingDailyQuota(Long userId) {
        int usedToday = getTodayUsage(userId);
        return Math.max(0, dailyLimit - usedToday);
    }

    /**
     * Atomically reserves exactly one AI search quota request.
     * Must be called strictly before performing an external LLM invocation (e.g. Groq).
     *
     * @throws AiQuotaExceededException if user has reached or exceeded their daily quota.
     */
    @Transactional
    public AiQuotaResponseDto consumeQuota(Long userId) {
        if (userId == null) {
            throw new InvalidRequestException("User ID cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists
        LocalDate today = getCurrentDate();

        if (dailyLimit <= 0) {
            throw new AiQuotaExceededException(
                    "Daily AI search quota exhausted for date: " + today + ". Limit: " + dailyLimit
            );
        }

        int affectedRows = aiUsageRepository.reserveQuotaAtomic(userId, today, dailyLimit);

        if (affectedRows == 0) {
            throw new AiQuotaExceededException(
                    "Daily AI search quota of " + dailyLimit + " exceeded for user " + userId + " on " + today
            );
        }

        return getQuotaInfo(userId);
    }

    /**
     * Current calendar date according to the configured quota timezone (defaults to UTC).
     */
    public LocalDate getCurrentDate() {
        return LocalDate.now(zoneId);
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }
}
