package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.model.PageView;
import com.liftlab.analytics.repository.MetricsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TopPagesMetric
 */
@ExtendWith(MockitoExtension.class)
class TopPagesMetricTest {

    @Mock
    private MetricsStorageService storageService;

    private TopPagesMetric topPagesMetric;

    @BeforeEach
    void setUp() {
        topPagesMetric = new TopPagesMetric();
    }

    @Test
    void testGetName() {
        // When
        String name = topPagesMetric.getName();

        // Then
        assertThat(name).isEqualTo("topPages");
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

        List<PageView> mockTopPages = Arrays.asList(
                PageView.builder().url("/home").viewCount(100L).build(),
                PageView.builder().url("/about").viewCount(50L).build()
        );

        when(storageService.getTopPages(eq(5), anyLong())).thenReturn(Mono.just(mockTopPages));

        // When
        StepVerifier.create(topPagesMetric.calculate(context))
                .expectNextMatches(result -> {
                    assertThat(result.getMetricName()).isEqualTo("topPages");
                    @SuppressWarnings("unchecked")
                    List<PageView> pages = (List<PageView>) result.getValue();
                    assertThat(pages).hasSize(2);
                    assertThat(pages.get(0).getUrl()).isEqualTo("/home");
                    assertThat(pages.get(0).getViewCount()).isEqualTo(100L);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getTopPages(eq(5), eq(context.getFifteenMinutesAgo()));
    }

    @Test
    void testCalculateWithEmptyTopPages() {
        // Given
        long now = System.currentTimeMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();

        when(storageService.getTopPages(eq(5), anyLong())).thenReturn(Mono.just(Collections.emptyList()));

        // When
        StepVerifier.create(topPagesMetric.calculate(context))
                .expectNextMatches(result -> {
                    @SuppressWarnings("unchecked")
                    List<PageView> pages = (List<PageView>) result.getValue();
                    assertThat(pages).isEmpty();
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testCalculateUsesCorrectTimeWindow() {
        // Given
        long now = System.currentTimeMillis();
        long fifteenMinAgo = now - Duration.ofMinutes(15).toMillis();
        MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(fifteenMinAgo)
                .storageService(storageService)
                .build();

        List<PageView> mockTopPages = Arrays.asList(
                PageView.builder().url("/home").viewCount(10L).build()
        );

        when(storageService.getTopPages(eq(5), eq(fifteenMinAgo))).thenReturn(Mono.just(mockTopPages));

        // When
        StepVerifier.create(topPagesMetric.calculate(context))
                .expectNextMatches(result -> {
                    @SuppressWarnings("unchecked")
                    List<PageView> pages = (List<PageView>) result.getValue();
                    assertThat(pages).hasSize(1);
                    return true;
                })
                .verifyComplete();

        // Then
        verify(storageService, times(1)).getTopPages(eq(5), eq(fifteenMinAgo));
    }
}

