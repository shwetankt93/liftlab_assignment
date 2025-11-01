package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Metric for active sessions per user (last 5 minutes)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActiveSessionsMetric implements IMetric {
    
    @Override
    public String getName() {
        return "activeSessions";
    }
    
    @Override
    public Mono<MetricResult> calculate(MetricContext context) {
        return context.getStorageService()
            .getActiveSessionsByUser(context.getFiveMinutesAgo())
            .map(sessionsByUser -> {
                log.debug("Calculated active sessions for {} users", sessionsByUser.size());
                return (MetricResult) new ActiveSessionsMetricResult(sessionsByUser);
            });
    }
    
    @Value
    private static class ActiveSessionsMetricResult implements MetricResult {
        Map<String, Integer> sessionsByUser;
        
        @Override
        public String getMetricName() {
            return "activeSessions";
        }
        
        @Override
        public Object getValue() {
            return sessionsByUser;
        }
    }
}

