package com.osc.backend.aiusage.service;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.entity.AiUsage;
import com.osc.backend.aiusage.repository.AiUsageRepository;
import com.osc.backend.common.exception.AiQuotaExceededException;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiUsageServiceTest {

    @Mock
    private AiUsageRepository aiUsageRepository;

    @Mock
    private UserService userService;

    private AiUsageService aiUsageService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        aiUsageService = new AiUsageService(aiUsageRepository, userService, 20, "UTC");
        sampleUser = new User(1001L, "octocat", "The Octocat", null, null);
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("consumeQuota should reserve one search unit when below daily limit")
    void consumeQuota_WhenBelowDailyLimit_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        // Atomic operation returns 1 (success)
        when(aiUsageRepository.reserveQuotaAtomic(eq(1L), eq(today), eq(20))).thenReturn(1);

        // After reservation, user has used 1 query
        AiUsage usageRecord = new AiUsage(sampleUser, today, 1);
        when(aiUsageRepository.findByUserIdAndUsageDate(1L, today)).thenReturn(Optional.of(usageRecord));

        AiQuotaResponseDto response = aiUsageService.consumeQuota(1L);

        assertNotNull(response);
        assertEquals(20, response.getDailyLimit());
        assertEquals(1, response.getUsedToday());
        assertEquals(19, response.getRemainingToday());
        assertFalse(response.isQuotaExceeded());
        verify(aiUsageRepository).reserveQuotaAtomic(1L, today, 20);
    }

    @Test
    @DisplayName("consumeQuota should throw AiQuotaExceededException when atomic reservation returns 0 (quota exhausted)")
    void consumeQuota_WhenAtomicOperationReturnsZero_ThrowsAiQuotaExceededException() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        // Atomic operation returns 0 (limit reached or exceeded in PostgreSQL)
        when(aiUsageRepository.reserveQuotaAtomic(eq(1L), eq(today), eq(20))).thenReturn(0);

        AiQuotaExceededException ex = assertThrows(
                AiQuotaExceededException.class,
                () -> aiUsageService.consumeQuota(1L)
        );

        assertTrue(ex.getMessage().contains("Daily AI search quota of 20 exceeded"));
        verify(aiUsageRepository).reserveQuotaAtomic(1L, today, 20);
    }

    @Test
    @DisplayName("getQuotaInfo should return accurate remaining and used count")
    void getQuotaInfo_ReturnsAccurateMetrics() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        AiUsage usageRecord = new AiUsage(sampleUser, today, 15);
        when(aiUsageRepository.findByUserIdAndUsageDate(1L, today)).thenReturn(Optional.of(usageRecord));

        AiQuotaResponseDto info = aiUsageService.getQuotaInfo(1L);

        assertEquals(20, info.getDailyLimit());
        assertEquals(15, info.getUsedToday());
        assertEquals(5, info.getRemainingToday());
        assertFalse(info.isQuotaExceeded());
    }

    @Test
    @DisplayName("getQuotaInfo should handle no existing usage record as 0 used and full quota remaining")
    void getQuotaInfo_WhenNoRecordToday_ReturnsFullQuota() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        when(aiUsageRepository.findByUserIdAndUsageDate(1L, today)).thenReturn(Optional.empty());

        AiQuotaResponseDto info = aiUsageService.getQuotaInfo(1L);

        assertEquals(20, info.getDailyLimit());
        assertEquals(0, info.getUsedToday());
        assertEquals(20, info.getRemainingToday());
        assertFalse(info.isQuotaExceeded());
    }

    @Test
    @DisplayName("getRemainingDailyQuota should return correct remaining amount")
    void getRemainingDailyQuota_ReturnsCorrectAmount() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        AiUsage usageRecord = new AiUsage(sampleUser, today, 20);
        when(aiUsageRepository.findByUserIdAndUsageDate(1L, today)).thenReturn(Optional.of(usageRecord));

        int remaining = aiUsageService.getRemainingDailyQuota(1L);

        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Simulate multiple concurrent quota reservations demonstrating atomic thread safety")
    void concurrentQuotaReservations_Simulation() throws InterruptedException {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        LocalDate today = aiUsageService.getCurrentDate();

        // Simulate atomic database counter that caps strictly at limit (20)
        AtomicInteger databaseCounter = new AtomicInteger(18); // 2 slots remaining
        int limit = 20;

        when(aiUsageRepository.reserveQuotaAtomic(eq(1L), eq(today), eq(limit)))
                .thenAnswer(invocation -> {
                    // Simulates: UPDATE ai_usage SET request_count = request_count + 1 WHERE request_count < limit
                    int current;
                    do {
                        current = databaseCounter.get();
                        if (current >= limit) {
                            return 0; // 0 rows affected (exhausted)
                        }
                    } while (!databaseCounter.compareAndSet(current, current + 1));
                    return 1; // 1 row affected (reserved)
                });

        when(aiUsageRepository.findByUserIdAndUsageDate(eq(1L), eq(today)))
                .thenAnswer(invocation -> Optional.of(new AiUsage(sampleUser, today, databaseCounter.get())));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger rejectedReservations = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // ensure simultaneous execution
                    aiUsageService.consumeQuota(1L);
                    successfulReservations.incrementAndGet();
                } catch (AiQuotaExceededException ex) {
                    rejectedReservations.incrementAndGet();
                } catch (Exception ex) {
                    fail("Unexpected exception: " + ex.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // start all threads together
        boolean finished = finishLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        assertTrue(finished, "All concurrent reservation tasks should complete within timeout");
        executor.shutdown();

        // Exactly 2 available slots must succeed; the remaining 8 threads must be rejected!
        assertEquals(2, successfulReservations.get(), "Only the remaining quota slots should be reserved");
        assertEquals(8, rejectedReservations.get(), "Requests exceeding limit must be rejected");
        assertEquals(20, databaseCounter.get(), "Counter must never exceed the daily limit of 20");
    }
}
