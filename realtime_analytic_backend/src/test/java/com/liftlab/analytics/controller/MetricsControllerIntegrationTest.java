package com.liftlab.analytics.controller;

import com.liftlab.analytics.model.MetricsResponse;
import com.liftlab.analytics.model.PageView;
import com.liftlab.analytics.repository.MetricsStorageService;
import com.liftlab.analytics.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MetricsController
 */
@WebFluxTest(MetricsController.class)
class MetricsControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private MetricsStorageService storageService;

    @BeforeEach
    void setUp() {
        // Setup cleanup mocks
        when(storageService.cleanupActiveUsers(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupPageViews(anyLong())).thenReturn(Mono.empty());
        when(storageService.cleanupUserSessions(anyLong())).thenReturn(Mono.empty());
    }

    @Test
    void testGetMetricsSuccess() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(10)
                .topPages(List.of(
                        PageView.builder().url("/home").viewCount(100L).build(),
                        PageView.builder().url("/about").viewCount(50L).build()
                ))
                .activeSessionsByUser(Map.of(
                        "user1", 2,
                        "user2", 1
                ))
                .timestamp(Instant.now())
                .build();

        when(metricsService.getCurrentMetrics()).thenReturn(Mono.just(mockResponse));

        // When/Then
        webTestClient.get()
                .uri("/api/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.activeUsersCount").isEqualTo(10)
                .jsonPath("$.topPages").isArray()
                .jsonPath("$.topPages[0].url").isEqualTo("/home")
                .jsonPath("$.topPages[0].viewCount").isEqualTo(100)
                .jsonPath("$.activeSessionsByUser.user1").isEqualTo(2)
                .jsonPath("$.activeSessionsByUser.user2").isEqualTo(1)
                .jsonPath("$.timestamp").exists();

        verify(metricsService, times(1)).getCurrentMetrics();
    }

    @Test
    void testGetMetricsWithEmptyData() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(0)
                .topPages(Collections.emptyList())
                .activeSessionsByUser(Collections.emptyMap())
                .timestamp(Instant.now())
                .build();

        when(metricsService.getCurrentMetrics()).thenReturn(Mono.just(mockResponse));

        // When/Then
        webTestClient.get()
                .uri("/api/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.activeUsersCount").isEqualTo(0)
                .jsonPath("$.topPages").isArray()
                .jsonPath("$.activeSessionsByUser").isMap();
    }

    @Test
    void testGetMetricsHandlesErrors() {
        // Given
        when(metricsService.getCurrentMetrics())
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // When/Then - Should return empty response on error
        webTestClient.get()
                .uri("/api/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.activeUsersCount").isEqualTo(0)
                .jsonPath("$.topPages").isArray()
                .jsonPath("$.activeSessionsByUser").isMap();
    }

    @Test
    void testGetMetricsResponseStructure() {
        // Given
        MetricsResponse mockResponse = MetricsResponse.builder()
                .activeUsersCount(5)
                .topPages(List.of(PageView.builder().url("/test").viewCount(10L).build()))
                .activeSessionsByUser(new HashMap<>())
                .timestamp(Instant.now())
                .build();

        when(metricsService.getCurrentMetrics()).thenReturn(Mono.just(mockResponse));

        // When/Then
        webTestClient.get()
                .uri("/api/metrics")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MetricsResponse.class)
                .value(response -> {
                    assert response != null;
                    assert response.getActiveUsersCount() == 5;
                    assert response.getTopPages() != null;
                    assert response.getActiveSessionsByUser() != null;
                    assert response.getTimestamp() != null;
                });
    }
}

