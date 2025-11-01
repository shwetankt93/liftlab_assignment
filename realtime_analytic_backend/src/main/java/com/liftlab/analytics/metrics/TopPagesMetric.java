package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Metric for top pages (last 15 minutes)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TopPagesMetric implements IMetric {
    
    @Override
    public String getName() {
        return "topPages";
    }
    
    @Override
    public Mono<MetricResult> calculate(MetricContext context) {
        return context.getStorageService()
            .getTopPages(5, context.getFifteenMinutesAgo())
            .map(topPages -> {
                log.debug("Calculated top pages: {}", topPages.size());
                return (MetricResult) new TopPagesMetricResult(topPages);
            });
    }
    
    @Value
    private static class TopPagesMetricResult implements MetricResult {
        List<com.liftlab.analytics.model.PageView> topPages;
        
        @Override
        public String getMetricName() {
            return "topPages";
        }
        
        @Override
        public Object getValue() {
            return topPages;
        }
    }
}

