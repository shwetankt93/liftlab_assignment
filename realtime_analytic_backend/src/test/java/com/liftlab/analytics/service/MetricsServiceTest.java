package com.liftlab.analytics.service;

import com.liftlab.analytics.metrics.MetricsCollector;
import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.model.MetricsResponse;
import com.liftlab.analytics.repository.MetricsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetricsService
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MetricsCollector metricsCollector;

    @Mock
    private MetricsStorageService storageService;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(metricsCollector, storageService);
    }

    @Test
    void testGetCurrentMetricsCallsCleanupFirst() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(5)
                .topPages(Collections.emptyList())
                .activeSessionsByUser(Collections.emptyMap())
                .timestamp(java.time.Instant.now())
                .build();

        when(storageService.cleanupActiveUsers(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupPageViews(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupUserSessions(anyLong())).thenReturn(Mono.empty());
        when(metricsCollector.calculateAllMetrics(any(MetricContext.class)))
                .thenReturn(Mono.just(mockResponse));

        // When
        StepVerifier.create(metricsService.getCurrentMetrics())
                .expectNextMatches(response -> {
                    assertThat(response.getActiveUsersCount()).isEqualTo(5);
                    return true;
                })
                .verifyComplete();

        // Then - verify cleanup was called before metrics calculation
        verify(storageService, times(1)).cleanupActiveUsers(anyLong());
        verify(storageService, times(1)).cleanupPageViews(anyLong());
        verify(storageService, times(1)).cleanupUserSessions(anyLong());
        // Verify metrics collector was called after cleanup
        verify(metricsCollector, times(1)).calculateAllMetrics(any(MetricContext.class));
    }

    @Test
    void testGetCurrentMetricsWaitsForCleanupBeforeCalculating() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(10)
                .topPages(Collections.emptyList())
                .activeSessionsByUser(Collections.emptyMap())
                .timestamp(java.time.Instant.now())
                .build();

        // Simulate slow cleanup - using then() to ensure sequential execution
        when(storageService.cleanupActiveUsers(anyLong()))
                .thenReturn(Mono.delay(Duration.ofMillis(50)).then());
        when(storageService.cleanupPageViews(anyLong()))
                .thenReturn(Mono.delay(Duration.ofMillis(50)).then());
        when(storageService.cleanupUserSessions(anyLong()))
                .thenReturn(Mono.delay(Duration.ofMillis(50)).then());
        when(metricsCollector.calculateAllMetrics(any(MetricContext.class)))
                .thenReturn(Mono.just(mockResponse));

        // When
        StepVerifier.create(metricsService.getCurrentMetrics())
                .expectNext(mockResponse)
                .verifyComplete();

        // Then - verify cleanup was called and metrics calculation happened
        verify(storageService, times(1)).cleanupActiveUsers(anyLong());
        verify(storageService, times(1)).cleanupPageViews(anyLong());
        verify(storageService, times(1)).cleanupUserSessions(anyLong());
        verify(metricsCollector, times(1)).calculateAllMetrics(any(MetricContext.class));
    }

    @Test
    void testGetCurrentMetricsHandlesCleanupErrors() {
        // Given
        when(storageService.cleanupActiveUsers(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Cleanup failed")));
        when(storageService.cleanupPageViews(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupUserSessions(anyLong())).thenReturn(Mono.empty());

        // When/Then
        StepVerifier.create(metricsService.getCurrentMetrics())
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testGetCurrentMetricsHandlesMetricsCalculationErrors() {
        // Given
        when(storageService.cleanupActiveUsers(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupPageViews(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupUserSessions(anyLong())).thenReturn(Mono.empty());
        when(metricsCollector.calculateAllMetrics(any(MetricContext.class)))
                .thenReturn(Mono.error(new RuntimeException("Calculation failed")));

        // When/Then
        StepVerifier.create(metricsService.getCurrentMetrics())
                .expectError(RuntimeException.class)
                .verify();

        // Verify cleanup was called even if metrics calculation fails
        verify(storageService, times(1)).cleanupActiveUsers(anyLong());
        verify(storageService, times(1)).cleanupPageViews(anyLong());
        verify(storageService, times(1)).cleanupUserSessions(anyLong());
    }

    @Test
    void testGetCurrentMetricsUsesCorrectTimeWindows() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(0)
                .topPages(Collections.emptyList())
                .activeSessionsByUser(Collections.emptyMap())
                .timestamp(java.time.Instant.now())
                .build();

        when(storageService.cleanupActiveUsers(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupPageViews(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupUserSessions(anyLong())).thenReturn(Mono.empty());
        when(metricsCollector.calculateAllMetrics(any(MetricContext.class)))
                .thenReturn(Mono.just(mockResponse));

        // When
        StepVerifier.create(metricsService.getCurrentMetrics())
                .expectNext(mockResponse)
                .verifyComplete();

        // Then - verify context is built with correct time windows
        verify(metricsCollector, times(1)).calculateAllMetrics(argThat(context -> {
            long now = System.currentTimeMillis();
            long fiveMinAgo = now - Duration.ofMinutes(5).toMillis();
            long fifteenMinAgo = now - Duration.ofMinutes(15).toMillis();

            // Allow some tolerance for time differences
            long tolerance = 1000; // 1 second tolerance
            return Math.abs(context.getCurrentTimestamp() - now) < tolerance &&
                   Math.abs(context.getFiveMinutesAgo() - fiveMinAgo) < tolerance &&
                   Math.abs(context.getFifteenMinutesAgo() - fifteenMinAgo) < tolerance &&
                   context.getStorageService() == storageService;
        }));
    }
}

