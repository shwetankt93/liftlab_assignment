package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActiveSessionsMetric
 */
@ExtendWith(MockitoExtension.class)
class ActiveSessionsMetricTest {

    @Mock
    private MetricsStorageService storageService;

    private ActiveSessionsMetric activeSessionsMetric;

    @BeforeEach
    void setUp() {
        activeSessionsMetric = new ActiveSessionsMetric();
    }

    @Test
    void testGetName() {
        // When
        String name = activeSessionsMetric.getName();

        // Then
        assertThat(name).isEqualTo("activeSessions");
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

        Map<String, Integer> mockSessions = new HashMap<>();
        mockSessions.put("user1", 2);
        mockSessions.put("user2", 1);

        when(storageService.getActiveSessionsByUser(anyLong())).thenReturn(Mono.just(mockSessions));

        // When
        StepVerifier.create(activeSessionsMetric.calculate(context))
                .expectNextMatches(result -> {
                    assertThat(result.getMetricName()).isEqualTo("activeSessions");
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> sessions = (Map<String, Integer>) result.getValue();
                    assertThat(sessions).hasSize(2);
                    assertThat(sessions.get("user1")).isEqualTo(2);
                    assertThat(sessions.get("user2")).isEqualTo(1);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getActiveSessionsByUser(context.getFiveMinutesAgo());
    }

    @Test
    void testCalculateWithEmptySessions() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        when(storageService.getActiveSessionsByUser(anyLong())).thenReturn(Mono.just(Collections.emptyMap()));

        // When
        StepVerifier.create(activeSessionsMetric.calculate(context))
                .expectNextMatches(result -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> sessions = (Map<String, Integer>) result.getValue();
                    assertThat(sessions).isEmpty();
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

        Map<String, Integer> mockSessions = new HashMap<>();
        mockSessions.put("user1", 1);

        when(storageService.getActiveSessionsByUser(eq(fiveMinAgo))).thenReturn(Mono.just(mockSessions));

        // When
        StepVerifier.create(activeSessionsMetric.calculate(context))
                .expectNextMatches(result -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> sessions = (Map<String, Integer>) result.getValue();
                    assertThat(sessions).hasSize(1);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getActiveSessionsByUser(fiveMinAgo);
    }
}

