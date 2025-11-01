package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import com.liftlab.analytics.repository.MetricsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActiveUsersMetric
 */
@ExtendWith(MockitoExtension.class)
class ActiveUsersMetricTest {

    @Mock
    private MetricsStorageService storageService;

    private ActiveUsersMetric activeUsersMetric;

    @BeforeEach
    void setUp() {
        activeUsersMetric = new ActiveUsersMetric();
    }

    @Test
    void testGetName() {
        // When
        String name = activeUsersMetric.getName();

        // Then
        assertThat(name).isEqualTo("activeUsers");
    }

    @Test
    void testCalculate() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        when(storageService.getActiveUserCount(anyLong())).thenReturn(Mono.just(10));

        // When
        StepVerifier.create(activeUsersMetric.calculate(context))
                .expectNextMatches(result -> {
                    assertThat(result.getMetricName()).isEqualTo("activeUsers");
                    assertThat(result.getValue()).isEqualTo(10);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getActiveUserCount(context.getFiveMinutesAgo());
    }

    @Test
    void testCalculateWithZeroUsers() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        when(storageService.getActiveUserCount(anyLong())).thenReturn(Mono.just(0));

        // When
        StepVerifier.create(activeUsersMetric.calculate(context))
                .expectNextMatches(result -> {
                    assertThat(result.getValue()).isEqualTo(0);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testCalculateUsesCorrectTimeWindow() {
        // Given
        long now = System.currentTimeMillis();
        long fiveMinAgo = now - Duration.ofMinutes(5).toMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(fiveMinAgo)
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        when(storageService.getActiveUserCount(eq(fiveMinAgo))).thenReturn(Mono.just(5));

        // When
        StepVerifier.create(activeUsersMetric.calculate(context))
                .expectNextMatches(result -> {
                    assertThat(result.getValue()).isEqualTo(5);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getActiveUserCount(fiveMinAgo);
    }
}

