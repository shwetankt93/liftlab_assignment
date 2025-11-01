package com.liftlab.analytics.service;

import com.liftlab.analytics.metrics.MetricsCollector;
import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.model.MetricsResponse;
import com.liftlab.analytics.repository.MetricsStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for retrieving metrics
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetricsService {
    
    private final MetricsCollector metricsCollector;
    private final MetricsStorageService storageService;
    
    /**
     * Get current metrics (for REST API)
     * Performs cleanup before calculating metrics to ensure data accuracy
     */
    public Mono<MetricsResponse> getCurrentMetrics() {
        long now = System.currentTimeMillis();
        
        // Step 1: Perform cleanup operations (in parallel)
        Mono<Void> cleanupMono = Mono.when(
            storageService.cleanupActiveUsers(now),
            storageService.cleanupPageViews(now),
            storageService.cleanupUserSessions(now)
        ).doOnSuccess(v -> log.debug("Completed all cleanup operations before calculating metrics"));
        
        // Step 2: After cleanup completes, calculate metrics
        return cleanupMono.then(Mono.fromCallable(() -> {
            MetricContext context = MetricContext.builder()
                .currentTimestamp(now)
                .fiveMinutesAgo(now - Duration.ofMinutes(5).toMillis())
                .fifteenMinutesAgo(now - Duration.ofMinutes(15).toMillis())
                .storageService(storageService)
                .build();
            return context;
        }))
        .flatMap(context -> metricsCollector.calculateAllMetrics(context));
    }
}

