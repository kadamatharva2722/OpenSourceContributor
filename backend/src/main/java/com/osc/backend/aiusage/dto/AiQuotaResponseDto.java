package com.osc.backend.aiusage.dto;

import java.time.LocalDate;

/**
 * Data transfer object representing a user's daily AI search quota details.
 */
public class AiQuotaResponseDto {

    private int dailyLimit;
    private int usedToday;
    private int remainingToday;
    private LocalDate usageDate;
    private boolean quotaExceeded;

    public AiQuotaResponseDto() {
    }

    public AiQuotaResponseDto(int dailyLimit, int usedToday, int remainingToday, LocalDate usageDate, boolean quotaExceeded) {
        this.dailyLimit = dailyLimit;
        this.usedToday = usedToday;
        this.remainingToday = remainingToday;
        this.usageDate = usageDate;
        this.quotaExceeded = quotaExceeded;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getUsedToday() {
        return usedToday;
    }

    public void setUsedToday(int usedToday) {
        this.usedToday = usedToday;
    }

    public int getRemainingToday() {
        return remainingToday;
    }

    public void setRemainingToday(int remainingToday) {
        this.remainingToday = remainingToday;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public boolean isQuotaExceeded() {
        return quotaExceeded;
    }

    public void setQuotaExceeded(boolean quotaExceeded) {
        this.quotaExceeded = quotaExceeded;
    }
}
