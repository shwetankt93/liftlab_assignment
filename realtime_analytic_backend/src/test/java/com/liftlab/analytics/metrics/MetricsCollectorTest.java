package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import com.liftlab.analytics.model.MetricsResponse;
import com.liftlab.analytics.repository.MetricsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetricsCollector
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricsCollectorTest {

    @Mock
    private MetricsStorageService storageService;

    private MetricsCollector metricsCollector;
    private IMetric mockMetric1;
    private IMetric mockMetric2;

    @BeforeEach
    void setUp() {
        // Create mock metrics
        mockMetric1 = mock(IMetric.class);
        mockMetric2 = mock(IMetric.class);

        metricsCollector = new MetricsCollector(Arrays.asList(mockMetric1, mockMetric2));
    }

    @Test
    void testCalculateAllMetricsCallsAllMetrics() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        MetricResult result1 = new MetricResult() {
            @Override
            public String getMetricName() {
                return "metric1";
            }

            @Override
            public Object getValue() {
                return 10;
            }
        };

        MetricResult result2 = new MetricResult() {
            @Override
            public String getMetricName() {
                return "metric2";
            }

            @Override
            public Object getValue() {
                return "value2";
            }
        };

        when(mockMetric1.calculate(context)).thenReturn(Mono.just(result1));
        when(mockMetric2.calculate(context)).thenReturn(Mono.just(result2));

        // When
        StepVerifier.create(metricsCollector.calculateAllMetrics(context))
                .expectNextMatches(response -> {
                    assertThat(response).isNotNull();
                    return true;
                })
                .verifyComplete();

        // Then
        verify(mockMetric1, times(1)).calculate(context);
        verify(mockMetric2, times(1)).calculate(context);
    }

    @Test
    void testCalculateAllMetricsHandlesErrors() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        MetricResult result2 = new MetricResult() {
            @Override
            public String getMetricName() {
                return "metric2";
            }

            @Override
            public Object getValue() {
                return "value2";
            }
        };

        when(mockMetric1.calculate(context))
                .thenReturn(Mono.error(new RuntimeException("Metric calculation failed")));
        when(mockMetric2.calculate(context)).thenReturn(Mono.just(result2));
        // Note: When using Mono.zip, all monos are evaluated, but if one fails, the zip fails

        // When/Then - Mono.zip will fail if any of the monos fail
        StepVerifier.create(metricsCollector.calculateAllMetrics(context))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testCalculateAllMetricsWithRealMetrics() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        // Create real metric instances
        ActiveUsersMetric activeUsersMetric = new ActiveUsersMetric();
        TopPagesMetric topPagesMetric = new TopPagesMetric();
        ActiveSessionsMetric activeSessionsMetric = new ActiveSessionsMetric();

        when(storageService.getActiveUserCount(anyLong())).thenReturn(Mono.just(5));
        when(storageService.getTopPages(anyInt(), anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(storageService.getActiveSessionsByUser(anyLong())).thenReturn(Mono.just(Collections.emptyMap()));

        MetricsCollector collector = new MetricsCollector(
                Arrays.asList(activeUsersMetric, topPagesMetric, activeSessionsMetric)
        );

        // When
        StepVerifier.create(collector.calculateAllMetrics(context))
                .expectNextMatches(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getActiveUsersCount()).isEqualTo(5);
                    assertThat(response.getTopPages()).isEmpty();
                    assertThat(response.getActiveSessionsByUser()).isEmpty();
                    assertThat(response.getTimestamp()).isNotNull();
                    return true;
                })
                .verifyComplete();
    }
}

