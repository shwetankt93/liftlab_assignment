package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import com.liftlab.analytics.model.MetricsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Collects and calculates all metrics
 */
@Component
@Slf4j
public class MetricsCollector {
    
    private final List<IMetric> metrics;
    
    public MetricsCollector(List<IMetric> metrics) {
        this.metrics = metrics;
        log.info("Initialized MetricsCollector with {} metrics", metrics.size());
    }
    
    /**
     * Calculate all metrics in parallel
     */
    public reactor.core.publisher.Mono<MetricsResponse> calculateAllMetrics(MetricContext context) {
        List<reactor.core.publisher.Mono<MetricResult>> metricMonoList = metrics.stream()
            .map(metric -> metric.calculate(context))
            .collect(toList());
        
        return reactor.core.publisher.Mono.zip(metricMonoList, results -> {
            Map<String, Object> metricMap = new HashMap<>();
            for (Object result : results) {
                if (result instanceof MetricResult) {
                    MetricResult mr = (MetricResult) result;
                    metricMap.put(mr.getMetricName(), mr.getValue());
                }
            }
            return buildMetricsResponse(metricMap);
        });
    }
    
    private MetricsResponse buildMetricsResponse(Map<String, Object> metricMap) {
        return MetricsResponse.builder()
            .activeUsersCount((Integer) metricMap.getOrDefault("activeUsers", 0))
            .topPages((List<com.liftlab.analytics.model.PageView>) metricMap.getOrDefault("topPages", Collections.emptyList()))
            .activeSessionsByUser((Map<String, Integer>) metricMap.getOrDefault("activeSessions", Collections.emptyMap()))
            .timestamp(Instant.now())
            .build();
    }
}

