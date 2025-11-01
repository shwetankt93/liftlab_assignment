package com.liftlab.analytics.metrics;

import com.liftlab.analytics.metrics.model.MetricContext;
import com.liftlab.analytics.metrics.model.MetricResult;
import reactor.core.publisher.Mono;

/**
 * Interface for metric calculations
 */
public interface IMetric {
    
    /**
     * Get the name/identifier of this metric
     */
    String getName();
    
    /**
     * Calculate and return the metric value
     * @param context Metric calculation context (time windows, etc.)
     * @return Metric result wrapped in Mono for reactive processing
     */
    Mono<MetricResult> calculate(MetricContext context);

}

