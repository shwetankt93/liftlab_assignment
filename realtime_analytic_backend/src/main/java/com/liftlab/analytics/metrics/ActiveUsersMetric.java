package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Metric for active users count (last 5 minutes)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActiveUsersMetric implements IMetric {
    
    @Override
    public String getName() {
        return "activeUsers";
    }
    
    @Override
    public Mono<MetricResult> calculate(MetricContext context) {
        return context.getStorageService()
            .getActiveUserCount(context.getFiveMinutesAgo())
            .map(count -> {
                log.debug("Calculated active users: {}", count);
                return (MetricResult) new ActiveUsersMetricResult(count);
            });
    }
    
    @Value
    private static class ActiveUsersMetricResult implements MetricResult {
        int activeUsersCount;
        
        @Override
        public String getMetricName() {
            return "activeUsers";
        }
        
        @Override
        public Object getValue() {
            return activeUsersCount;
        }
    }
}

